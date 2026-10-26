import Fastify, { type FastifyInstance, type FastifyReply, type FastifyRequest } from "fastify";
import rateLimit from "@fastify/rate-limit";
import websocket from "@fastify/websocket";
import { z } from "zod";
import { ReplayGuard, Tokens, type SessionVerifier, type TokenClaims } from "./auth.js";
import type { Config } from "./config.js";
import type { Db } from "./db.js";
import { Profiles, toPublic } from "./profiles.js";
import { Hub, MAX_MESSAGE_BYTES } from "./realtime.js";
import { Loadout, ProfileUpdate, ServerHash, Username, Uuid, cleanLoadout } from "./validation.js";

export interface AppDeps {
  config: Config;
  db: Db;
  verifier: SessionVerifier;
}

declare module "fastify" {
  interface FastifyRequest {
    user?: TokenClaims;
  }
  interface FastifyInstance {
    hub: Hub;
  }
}

/** Largest JSON body any route accepts. */
const BODY_LIMIT_BYTES = 4096;

/** Builds the HTTP + WebSocket server. Kept separate from index.ts so tests can inject requests. */
export async function buildApp({ config, db, verifier }: AppDeps): Promise<FastifyInstance> {
  const app = Fastify({
    logger: { level: config.LOG_LEVEL },
    trustProxy: config.TRUST_PROXY,
    bodyLimit: BODY_LIMIT_BYTES,
  });
  const tokens = new Tokens(config);
  const replay = new ReplayGuard();
  const profiles = new Profiles(db);
  const hub = new Hub({ tokens, allowUnverified: config.ALLOW_UNVERIFIED, log: app.log });

  await app.register(rateLimit, { max: config.RATE_LIMIT_PER_MINUTE, timeWindow: "1 minute" });
  await app.register(websocket, { options: { maxPayload: MAX_MESSAGE_BYTES } });

  /** Parses with zod and answers 400 with the first problem when the input is wrong. */
  function parse<T extends z.ZodType>(schema: T, value: unknown, reply: FastifyReply): z.infer<T> | undefined {
    const result = schema.safeParse(value);
    if (result.success) return result.data;
    const issue = result.error.issues[0];
    void reply.code(400).send({ error: "invalid_request", message: issue ? `${issue.path.join(".") || "input"}: ${issue.message}` : "invalid" });
    return undefined;
  }

  /** preHandler for routes that need a signed-in player (Authorization: Bearer <token>). */
  async function requireUser(request: FastifyRequest, reply: FastifyReply): Promise<void> {
    const header = request.headers.authorization;
    const token = header?.startsWith("Bearer ") ? header.slice("Bearer ".length) : undefined;
    const claims = token ? await tokens.verify(token) : null;
    if (!claims) return reply.code(401).send({ error: "unauthorized" });
    request.user = claims;
  }

  app.get("/health", async () => ({ ok: true, online: hub.onlineCount }));

  // Sign-in: the client has called Mojang's joinServer with `serverId`; we confirm it with hasJoined.
  app.post(
    "/auth",
    { config: { rateLimit: { max: config.AUTH_RATE_LIMIT_PER_MINUTE, timeWindow: "1 minute" } } },
    async (request, reply) => {
      const body = parse(z.strictObject({ username: Username, serverId: ServerHash }), request.body, reply);
      if (!body) return;
      let profile;
      try {
        profile = await verifier.hasJoined(body.username, body.serverId);
      } catch (err) {
        request.log.warn({ err }, "Mojang session check failed");
        return reply.code(502).send({ error: "mojang_unavailable" });
      }
      if (!profile) return reply.code(401).send({ error: "not_verified" });
      if (!replay.claim(body.serverId)) return reply.code(401).send({ error: "server_id_reused" });
      const user = await profiles.signedIn(profile.uuid, profile.name);
      const { token, expiresAt } = await tokens.issue({ uuid: user.uuid, name: user.name });
      return { token, expiresAt: expiresAt.toISOString(), profile: toPublic(user) };
    },
  );

  // Development sign-in for offline accounts (no Mojang session). Only exists with ALLOW_UNVERIFIED.
  if (config.ALLOW_UNVERIFIED) {
    app.post(
      "/auth/dev",
      { config: { rateLimit: { max: config.AUTH_RATE_LIMIT_PER_MINUTE, timeWindow: "1 minute" } } },
      async (request, reply) => {
        const body = parse(z.strictObject({ uuid: Uuid, username: Username }), request.body, reply);
        if (!body) return;
        const user = await profiles.signedIn(body.uuid, body.username);
        const { token, expiresAt } = await tokens.issue({ uuid: user.uuid, name: user.name });
        return { token, expiresAt: expiresAt.toISOString(), profile: toPublic(user) };
      },
    );
  }

  app.get("/users/:uuid", async (request, reply) => {
    const uuid = parse(Uuid, (request.params as { uuid?: unknown }).uuid, reply);
    if (!uuid) return;
    const user = await profiles.find(uuid);
    if (!user) return reply.code(404).send({ error: "not_found" });
    return toPublic(user);
  });

  app.get("/me", { preHandler: requireUser }, async (request, reply) => {
    const user = await profiles.find(request.user!.uuid);
    return user ? toPublic(user) : reply.code(404).send({ error: "not_found" });
  });

  app.put("/me/cosmetics", { preHandler: requireUser }, async (request, reply) => {
    const loadout = parse(Loadout, request.body, reply);
    if (!loadout) return;
    const user = await profiles.setCosmetics(request.user!.uuid, cleanLoadout(loadout));
    hub.profileChanged(user.uuid);
    return toPublic(user);
  });

  app.put("/me/profile", { preHandler: requireUser }, async (request, reply) => {
    const fields = parse(ProfileUpdate, request.body, reply);
    if (!fields) return;
    const user = await profiles.setProfile(request.user!.uuid, fields);
    hub.profileChanged(user.uuid);
    return toPublic(user);
  });

  app.get("/ws", { websocket: true }, (socket) => hub.attach(socket));

  app.decorate("hub", hub);
  app.addHook("onClose", async () => hub.close());
  return app;
}
