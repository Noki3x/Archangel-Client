import type { WebSocket } from "ws";
import type { FastifyBaseLogger } from "fastify";
import type { Tokens } from "./auth.js";
import { EmoteId, ServerAddress, Uuid, Username } from "./validation.js";

/**
 * The WebSocket hub at /ws. Each Minecraft server the players are on is a room: emotes and
 * presence only go to players in the same room, and profile changes go to every room the player
 * is in. Messages are small JSON objects with a "type" (see docs/STATUS.md, milestone 6 and 9).
 *
 * client -> hub: hello {token?, uuid, name, server}, emote {emote}, emote_stop, ping
 * hub -> client: welcome {uuid, verified, online[]}, presence {uuid, online}, emote {uuid, emote},
 *                emote_stop {uuid}, profile_update {uuid}, pong, error {reason}
 */

/** Bigger messages are refused by the socket itself. */
export const MAX_MESSAGE_BYTES = 2048;
/** Close codes in the application range (4000-4999). */
const CLOSE_AUTH = 4001;
const CLOSE_REPLACED = 4002;
const CLOSE_ABUSE = 4003;
const CLOSE_HELLO_TIMEOUT = 4004;
/** A connection must say hello this quickly. */
const HELLO_TIMEOUT_MS = 10_000;
/** Malformed or unexpected messages allowed before we hang up. */
const MAX_STRIKES = 10;
/** Emotes: a burst of this many, then one more every refill interval. */
const EMOTE_BURST = 4;
const EMOTE_REFILL_MS = 1500;
/** Ping interval; a connection that misses a pong is dropped at the next one. */
const HEARTBEAT_MS = 30_000;

/** Token bucket for one kind of message on one connection. */
class Bucket {
  private tokens: number;
  private last = Date.now();

  constructor(
    private readonly capacity: number,
    private readonly refillMs: number,
  ) {
    this.tokens = capacity;
  }

  take(now = Date.now()): boolean {
    this.tokens = Math.min(this.capacity, this.tokens + (now - this.last) / this.refillMs);
    this.last = now;
    if (this.tokens < 1) return false;
    this.tokens -= 1;
    return true;
  }
}

interface Peer {
  socket: WebSocket;
  uuid?: string;
  name?: string;
  room?: string;
  verified: boolean;
  alive: boolean;
  strikes: number;
  emotes: Bucket;
  stops: Bucket;
}

export interface HubOptions {
  tokens: Tokens;
  /** Accept a hello without a token, trusting its uuid. Only for local development. */
  allowUnverified: boolean;
  log: FastifyBaseLogger;
}

export class Hub {
  private readonly rooms = new Map<string, Set<Peer>>();
  private readonly peers = new Set<Peer>();
  private readonly heartbeat: NodeJS.Timeout;

  constructor(private readonly options: HubOptions) {
    this.heartbeat = setInterval(() => this.beat(), HEARTBEAT_MS);
    this.heartbeat.unref();
  }

  get onlineCount(): number {
    return this.peers.size;
  }

  attach(socket: WebSocket): void {
    const peer: Peer = {
      socket,
      verified: false,
      alive: true,
      strikes: 0,
      emotes: new Bucket(EMOTE_BURST, EMOTE_REFILL_MS),
      stops: new Bucket(EMOTE_BURST * 2, EMOTE_REFILL_MS / 2),
    };
    this.peers.add(peer);
    const helloTimer = setTimeout(() => {
      if (!peer.room) socket.close(CLOSE_HELLO_TIMEOUT, "hello expected");
    }, HELLO_TIMEOUT_MS);
    socket.on("pong", () => (peer.alive = true));
    socket.on("message", (data, isBinary) => {
      if (isBinary) return this.strike(peer);
      void this.handle(peer, data.toString()).catch((err) => {
        this.options.log.warn({ err }, "websocket message failed");
      });
    });
    socket.on("close", () => {
      clearTimeout(helloTimer);
      this.remove(peer);
    });
    socket.on("error", () => socket.terminate());
  }

  /** Tells everyone who can see this player to reload their profile. */
  profileChanged(uuid: string): void {
    const message = JSON.stringify({ type: "profile_update", uuid });
    const sent = new Set<Peer>();
    for (const room of this.roomsOf(uuid)) {
      for (const p of room) {
        if (!sent.has(p)) send(p, message);
        sent.add(p);
      }
    }
  }

  close(): void {
    clearInterval(this.heartbeat);
    for (const p of this.peers) p.socket.close(1001, "server shutting down");
  }

  private async handle(peer: Peer, text: string): Promise<void> {
    let message: Record<string, unknown>;
    try {
      const parsed: unknown = JSON.parse(text);
      if (!parsed || typeof parsed !== "object" || Array.isArray(parsed)) return this.strike(peer);
      message = parsed as Record<string, unknown>;
    } catch {
      return this.strike(peer);
    }
    switch (message.type) {
      case "hello":
        return this.hello(peer, message);
      case "ping":
        return send(peer, JSON.stringify({ type: "pong" }));
      case "emote":
        return this.emote(peer, message);
      case "emote_stop":
        if (peer.room && peer.stops.take()) this.toRoom(peer, { type: "emote_stop", uuid: peer.uuid });
        return;
      default:
        return this.strike(peer);
    }
  }

  private async hello(peer: Peer, m: Record<string, unknown>): Promise<void> {
    if (peer.room) return this.strike(peer); // one hello per connection
    const server = ServerAddress.safeParse(m.server);
    if (!server.success) return reject(peer, "bad_hello");

    if (typeof m.token === "string") {
      const claims = await this.options.tokens.verify(m.token);
      if (!claims) return reject(peer, "invalid_token");
      peer.uuid = claims.uuid;
      peer.name = claims.name;
      peer.verified = true;
    } else if (this.options.allowUnverified) {
      const uuid = Uuid.safeParse(m.uuid);
      const name = Username.safeParse(m.name);
      if (!uuid.success || !name.success) return reject(peer, "bad_hello");
      peer.uuid = uuid.data;
      peer.name = name.data;
    } else {
      return reject(peer, "auth_required");
    }
    if (peer.socket.readyState !== peer.socket.OPEN) return;

    let room = this.rooms.get(server.data);
    if (!room) this.rooms.set(server.data, (room = new Set()));
    for (const other of room) {
      if (other.uuid === peer.uuid) other.socket.close(CLOSE_REPLACED, "connected elsewhere");
    }
    peer.room = server.data;
    room.add(peer);
    const online = [...room].filter((p) => p !== peer && p.uuid !== peer.uuid).map((p) => p.uuid);
    send(peer, JSON.stringify({ type: "welcome", uuid: peer.uuid, verified: peer.verified, online }));
    this.toRoom(peer, { type: "presence", uuid: peer.uuid, online: true });
  }

  private emote(peer: Peer, m: Record<string, unknown>): void {
    if (!peer.room) return this.strike(peer);
    const emote = EmoteId.safeParse(m.emote);
    if (!emote.success) return this.strike(peer);
    if (!peer.emotes.take()) return; // flood: drop quietly, the client already rate limits
    this.toRoom(peer, { type: "emote", uuid: peer.uuid, emote: emote.data });
  }

  private remove(peer: Peer): void {
    this.peers.delete(peer);
    if (!peer.room) return;
    const room = this.rooms.get(peer.room);
    if (!room?.delete(peer)) return;
    if (room.size === 0) this.rooms.delete(peer.room);
    else if (![...room].some((p) => p.uuid === peer.uuid)) this.toRoom(peer, { type: "presence", uuid: peer.uuid, online: false });
  }

  private toRoom(from: Peer, payload: object): void {
    const room = from.room ? this.rooms.get(from.room) : undefined;
    if (!room) return;
    const message = JSON.stringify(payload);
    for (const p of room) if (p !== from) send(p, message);
  }

  private *roomsOf(uuid: string): Iterable<Set<Peer>> {
    for (const room of this.rooms.values()) {
      for (const p of room) {
        if (p.uuid === uuid) {
          yield room;
          break;
        }
      }
    }
  }

  private strike(peer: Peer): void {
    if (++peer.strikes > MAX_STRIKES) peer.socket.close(CLOSE_ABUSE, "too many bad messages");
  }

  private beat(): void {
    for (const p of this.peers) {
      if (!p.alive) {
        p.socket.terminate();
        continue;
      }
      p.alive = false;
      p.socket.ping();
    }
  }
}

function send(peer: Peer, message: string): void {
  if (peer.socket.readyState === peer.socket.OPEN) peer.socket.send(message);
}

function reject(peer: Peer, reason: string): void {
  send(peer, JSON.stringify({ type: "error", reason }));
  peer.socket.close(CLOSE_AUTH, reason);
}
