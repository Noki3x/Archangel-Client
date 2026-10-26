import { mkdtempSync, readdirSync, readFileSync, rmSync } from "node:fs";
import { tmpdir } from "node:os";
import path from "node:path";
import type { AddressInfo } from "node:net";
import WebSocket from "ws";
import type { MojangProfile, SessionVerifier } from "../src/auth.js";
import { buildApp } from "../src/app.js";
import { loadConfig } from "../src/config.js";
import { openDatabase } from "../src/db.js";

/** Stands in for Mojang: a username "has joined" a serverId once the test says so. */
export class FakeMojang implements SessionVerifier {
  private readonly joins = new Map<string, MojangProfile>();
  down = false;

  join(profile: MojangProfile, serverId: string): void {
    this.joins.set(`${profile.name}|${serverId}`, profile);
  }

  async hasJoined(username: string, serverId: string): Promise<MojangProfile | null> {
    if (this.down) throw new Error("mojang down");
    return this.joins.get(`${username}|${serverId}`) ?? null;
  }
}

export const ALICE: MojangProfile = { uuid: "0f0e0d0c-0b0a-4908-8706-050403020100", name: "Alice" };
export const BOB: MojangProfile = { uuid: "11111111-2222-4333-8444-555555555555", name: "Bob" };

/** A server on a fresh SQLite file with the migrations applied, listening on a random port. */
export async function startServer(env: Record<string, string> = {}) {
  const dir = mkdtempSync(path.join(tmpdir(), "archangel-test-"));
  const url = `file:${path.join(dir, "test.db")}`;
  const config = loadConfig({ JWT_SECRET: "x".repeat(40), LOG_LEVEL: "silent", DATABASE_URL: url, ...env });
  const db = openDatabase(url);
  const migrations = path.resolve(import.meta.dirname, "../prisma/migrations");
  for (const name of readdirSync(migrations).filter((n) => !n.endsWith(".toml")).sort()) {
    const sql = readFileSync(path.join(migrations, name, "migration.sql"), "utf8");
    for (const statement of sql.split(/;\s*$/m).map((s) => s.trim()).filter(Boolean)) await db.$executeRawUnsafe(statement);
  }
  const mojang = new FakeMojang();
  const app = await buildApp({ config, db, verifier: mojang });
  await app.listen({ host: "127.0.0.1", port: 0 });
  const port = (app.server.address() as AddressInfo).port;
  return {
    app,
    mojang,
    port,
    async signIn(profile: MojangProfile, serverId = randomHash()): Promise<string> {
      mojang.join(profile, serverId);
      const res = await app.inject({ method: "POST", url: "/auth", payload: { username: profile.name, serverId } });
      if (res.statusCode !== 200) throw new Error(`sign-in failed: ${res.statusCode} ${res.body}`);
      return res.json().token as string;
    },
    async close() {
      await app.close();
      await db.$disconnect();
      rmSync(dir, { recursive: true, force: true });
    },
  };
}

export function randomHash(): string {
  return [...crypto.getRandomValues(new Uint8Array(20))].map((b) => b.toString(16).padStart(2, "0")).join("");
}

/** A WebSocket client that collects every message it gets. */
export async function connect(port: number) {
  const ws = new WebSocket(`ws://127.0.0.1:${port}/ws`);
  const inbox: Record<string, unknown>[] = [];
  const waiters: (() => void)[] = [];
  let closeCode: number | undefined;
  ws.on("message", (data) => {
    inbox.push(JSON.parse(data.toString()));
    waiters.splice(0).forEach((w) => w());
  });
  ws.on("close", (code) => {
    closeCode = code;
    waiters.splice(0).forEach((w) => w());
  });
  await new Promise<void>((resolve, reject) => {
    ws.once("open", () => resolve());
    ws.once("error", reject);
  });
  return {
    ws,
    inbox,
    get closeCode() {
      return closeCode;
    },
    send(message: object) {
      ws.send(JSON.stringify(message));
    },
    /** Resolves with the first message of `type` (already received or still to come). */
    async next(type: string, timeoutMs = 2000): Promise<Record<string, unknown>> {
      const deadline = Date.now() + timeoutMs;
      for (;;) {
        const i = inbox.findIndex((m) => m.type === type);
        if (i >= 0) return inbox.splice(i, 1)[0]!;
        if (closeCode !== undefined) throw new Error(`closed (${closeCode}) before "${type}"`);
        const left = deadline - Date.now();
        if (left <= 0) throw new Error(`no "${type}" message`);
        await new Promise<void>((resolve) => {
          waiters.push(resolve);
          setTimeout(resolve, left);
        });
      }
    },
    /** Waits a moment and returns the types received in that time (for "nothing arrives" checks). */
    async quiet(ms = 200): Promise<string[]> {
      await new Promise((r) => setTimeout(r, ms));
      return inbox.splice(0).map((m) => String(m.type));
    },
    async closed(timeoutMs = 2000): Promise<number> {
      const deadline = Date.now() + timeoutMs;
      while (closeCode === undefined && Date.now() < deadline) await new Promise((r) => setTimeout(r, 20));
      if (closeCode === undefined) throw new Error("still open");
      return closeCode;
    },
    close() {
      ws.close();
    },
  };
}
