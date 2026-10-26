import { z } from "zod";

/**
 * Input rules shared by the REST routes and the WebSocket. They mirror what the client accepts
 * (net/UserProfile.java, cosmetics/CosmeticType.java), so nothing the backend stores is dropped
 * on the other side.
 */

/** Mojang UUID with or without dashes, normalized to lowercase with dashes. */
export const Uuid = z
  .string()
  .regex(/^[0-9a-fA-F]{8}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{4}-?[0-9a-fA-F]{12}$/, "not a UUID")
  .transform((s) => {
    const h = s.replaceAll("-", "").toLowerCase();
    return `${h.slice(0, 8)}-${h.slice(8, 12)}-${h.slice(12, 16)}-${h.slice(16, 20)}-${h.slice(20)}`;
  });

/** Minecraft account name. */
export const Username = z.string().regex(/^[A-Za-z0-9_]{1,16}$/, "not a Minecraft username");

/** The serverId the client passed to Mojang's joinServer: a (possibly negative) hex digest. */
export const ServerHash = z.string().regex(/^-?[0-9a-f]{1,40}$/, "not a server hash");

/** Same rule as the client: letters, digits, underscore and spaces, 1-24 characters. */
export const DisplayName = z
  .string()
  .trim()
  .regex(/^[A-Za-z0-9_ ]{1,24}$/, "display names use letters, digits, _ and spaces (max 24)");

export const NameColor = z.string().regex(/^#[0-9A-Fa-f]{6}$/, "colors look like #RRGGBB");

/** Cosmetic slots, as CosmeticType.key() names them on the client. */
export const COSMETIC_SLOTS = ["cape", "wings", "hat", "halo", "aura", "trail", "hit_effect"] as const;
export const CosmeticId = z.string().regex(/^[a-z0-9_]{1,48}$/, "not a cosmetic id");

/** A full loadout: every slot is optional, and null (or leaving it out) means nothing equipped. */
export const Loadout = z.strictObject(
  Object.fromEntries(COSMETIC_SLOTS.map((slot) => [slot, CosmeticId.nullable().optional()])) as Record<
    (typeof COSMETIC_SLOTS)[number],
    z.ZodOptional<z.ZodNullable<typeof CosmeticId>>
  >,
);
export type Loadout = Partial<Record<(typeof COSMETIC_SLOTS)[number], string>>;

/** Drops empty slots so the stored JSON only holds what's equipped. */
export function cleanLoadout(input: z.infer<typeof Loadout>): Loadout {
  const out: Loadout = {};
  for (const slot of COSMETIC_SLOTS) {
    const id = input[slot];
    if (id) out[slot] = id;
  }
  return out;
}

export const ProfileUpdate = z.strictObject({
  displayName: DisplayName.nullable().optional(),
  nameColor: NameColor.nullable().optional(),
});

export const EmoteId = z.string().regex(/^[a-z0-9_]{1,32}$/);

/** Server address from the client's hello, used only to group players into rooms. */
export const ServerAddress = z
  .string()
  .trim()
  .min(1)
  .max(255)
  .regex(/^[\x21-\x7e]+$/, "printable ASCII only")
  .transform((s) => s.toLowerCase());
