import { SignJWT, jwtVerify } from "jose";
import type { Config } from "./config.js";

/** A Mojang account as the session server reports it. */
export interface MojangProfile {
  uuid: string;
  name: string;
}

/**
 * Checks that a player really owns an account, using the same handshake Minecraft servers use:
 * the client calls Mojang's joinServer with a random serverId, then we ask Mojang whether that
 * username "has joined" that serverId. Only the real account holder can make the first call.
 */
export interface SessionVerifier {
  hasJoined(username: string, serverId: string): Promise<MojangProfile | null>;
}

const MOJANG_TIMEOUT_MS = 5000;

export class MojangSessionVerifier implements SessionVerifier {
  constructor(private readonly baseUrl: string) {}

  async hasJoined(username: string, serverId: string): Promise<MojangProfile | null> {
    const url = new URL("/session/minecraft/hasJoined", this.baseUrl);
    url.searchParams.set("username", username);
    url.searchParams.set("serverId", serverId);
    const res = await fetch(url, { signal: AbortSignal.timeout(MOJANG_TIMEOUT_MS) });
    if (res.status === 204 || res.status === 403) return null; // not joined
    if (!res.ok) throw new Error(`Mojang session server answered ${res.status}`);
    const body = (await res.json()) as { id?: unknown; name?: unknown };
    if (typeof body.id !== "string" || typeof body.name !== "string") return null;
    return { uuid: dashed(body.id), name: body.name };
  }
}

function dashed(hex: string): string {
  const h = hex.replaceAll("-", "").toLowerCase();
  return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
}

/** How long a serverId stays "used", which is longer than Mojang keeps a join valid. */
const SERVER_ID_MEMORY_MS = 5 * 60 * 1000;

/** Remembers serverIds that already produced a token, so a captured one can't be replayed. */
export class ReplayGuard {
  private readonly seen = new Map<string, number>();

  /** True the first time a serverId is seen; false if it was used in the last few minutes. */
  claim(serverId: string, now = Date.now()): boolean {
    for (const [id, at] of this.seen) if (now - at > SERVER_ID_MEMORY_MS) this.seen.delete(id);
    if (this.seen.has(serverId)) return false;
    this.seen.set(serverId, now);
    return true;
  }
}

export interface TokenClaims {
  uuid: string;
  name: string;
}

/** Signs and checks the session tokens (HS256 JWTs) the client sends as a Bearer token. */
export class Tokens {
  private readonly key: Uint8Array;

  constructor(private readonly config: Pick<Config, "JWT_SECRET" | "JWT_TTL_HOURS">) {
    this.key = new TextEncoder().encode(config.JWT_SECRET);
  }

  async issue(claims: TokenClaims): Promise<{ token: string; expiresAt: Date }> {
    const expiresAt = new Date(Date.now() + this.config.JWT_TTL_HOURS * 3600 * 1000);
    const token = await new SignJWT({ name: claims.name })
      .setProtectedHeader({ alg: "HS256" })
      .setSubject(claims.uuid)
      .setIssuedAt()
      .setExpirationTime(expiresAt)
      .setIssuer("archangel")
      .sign(this.key);
    return { token, expiresAt };
  }

  /** The token's claims, or null if it's invalid or expired. */
  async verify(token: string): Promise<TokenClaims | null> {
    try {
      const { payload } = await jwtVerify(token, this.key, { issuer: "archangel", algorithms: ["HS256"] });
      if (typeof payload.sub !== "string" || typeof payload.name !== "string") return null;
      return { uuid: payload.sub, name: payload.name };
    } catch {
      return null;
    }
  }
}
