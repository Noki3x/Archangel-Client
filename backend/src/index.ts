import "dotenv/config";
import { MojangSessionVerifier } from "./auth.js";
import { buildApp } from "./app.js";
import { loadConfig } from "./config.js";
import { openDatabase } from "./db.js";

const config = loadConfig();
const db = openDatabase(config.DATABASE_URL);
const app = await buildApp({ config, db, verifier: new MojangSessionVerifier(config.MOJANG_SESSION_URL) });
if (config.ALLOW_UNVERIFIED) app.log.warn("ALLOW_UNVERIFIED is on: WebSocket clients are trusted without signing in. Development only.");

for (const signal of ["SIGINT", "SIGTERM"] as const) {
  process.once(signal, () => {
    app.log.info(`${signal} received, shutting down`);
    void app.close().then(() => db.$disconnect()).finally(() => process.exit(0));
  });
}

await app.listen({ host: config.HOST, port: config.PORT });
