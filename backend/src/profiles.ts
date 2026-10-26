import type { Db } from "./db.js";
import type { User } from "./generated/prisma/client.js";
import type { Loadout } from "./validation.js";

/** The public profile the client reads in net/UserProfile.fromJson. */
export interface PublicProfile {
  uuid: string;
  displayName: string;
  nameColor?: string;
  badge: string;
  cosmetics: Loadout;
}

export function toPublic(user: User): PublicProfile {
  return {
    uuid: user.uuid,
    displayName: user.displayName ?? user.name,
    ...(user.nameColor ? { nameColor: user.nameColor } : {}),
    badge: user.badge,
    cosmetics: parseLoadout(user.cosmetics),
  };
}

function parseLoadout(json: string): Loadout {
  try {
    const value: unknown = JSON.parse(json);
    return value && typeof value === "object" ? (value as Loadout) : {};
  } catch {
    return {};
  }
}

/** Database access for user profiles. */
export class Profiles {
  constructor(private readonly db: Db) {}

  find(uuid: string): Promise<User | null> {
    return this.db.user.findUnique({ where: { uuid } });
  }

  /** Called after a successful sign-in: creates the user or refreshes their account name. */
  signedIn(uuid: string, name: string): Promise<User> {
    const now = new Date();
    return this.db.user.upsert({
      where: { uuid },
      create: { uuid, name, lastSeenAt: now },
      update: { name, lastSeenAt: now },
    });
  }

  setCosmetics(uuid: string, loadout: Loadout): Promise<User> {
    return this.db.user.update({ where: { uuid }, data: { cosmetics: JSON.stringify(loadout) } });
  }

  /** undefined leaves a field as it is; null clears it. */
  setProfile(uuid: string, fields: { displayName?: string | null; nameColor?: string | null }): Promise<User> {
    return this.db.user.update({ where: { uuid }, data: fields });
  }
}
