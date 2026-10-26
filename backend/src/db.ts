import { PrismaBetterSqlite3 } from "@prisma/adapter-better-sqlite3";
import { PrismaClient } from "./generated/prisma/client.js";

export type Db = PrismaClient;

/** Opens the SQLite database at {@code url} (e.g. "file:./data/archangel.db"). */
export function openDatabase(url: string): Db {
  return new PrismaClient({ adapter: new PrismaBetterSqlite3({ url }) });
}
