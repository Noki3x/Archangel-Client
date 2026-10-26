import { z } from "zod";

const flag = z.enum(["true", "false"]).default("false").transform((v) => v === "true");

/** Every setting comes from the environment (or .env); see .env.example for what each one does. */
const Env = z.object({
  HOST: z.string().default("127.0.0.1"),
  PORT: z.coerce.number().int().min(1).max(65535).default(8787),
  DATABASE_URL: z.string().default("file:./data/archangel.db"),
  JWT_SECRET: z.string().min(32, "JWT_SECRET must be at least 32 characters"),
  JWT_TTL_HOURS: z.coerce.number().int().positive().default(24),
  MOJANG_SESSION_URL: z.url().default("https://sessionserver.mojang.com"),
  ALLOW_UNVERIFIED: flag,
  TRUST_PROXY: flag,
  RATE_LIMIT_PER_MINUTE: z.coerce.number().int().positive().default(120),
  AUTH_RATE_LIMIT_PER_MINUTE: z.coerce.number().int().positive().default(10),
  LOG_LEVEL: z.enum(["fatal", "error", "warn", "info", "debug", "trace", "silent"]).default("info"),
});

export type Config = z.infer<typeof Env>;

export function loadConfig(env: NodeJS.ProcessEnv = process.env): Config {
  const parsed = Env.safeParse(env);
  if (!parsed.success) {
    const problems = parsed.error.issues.map((i) => `  ${i.path.join(".")}: ${i.message}`).join("\n");
    throw new Error(`Invalid configuration:\n${problems}`);
  }
  return parsed.data;
}
