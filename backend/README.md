# Archangel backend

Profiles, cosmetics, sign-in and a realtime relay for Archangel Client.
It is built with Node + TypeScript, Fastify 5, Prisma 7 on SQLite, and a WebSocket hub.

The client never needs this server. Without it, you see your own cosmetics, and nobody else's
show up. With it, Archangel players see each other's cosmetics, names, badges and emotes.

## Run it

```bash
cd backend
npm install                 # also generates the Prisma client
cp .env.example .env        # then set JWT_SECRET
npm run db:deploy           # creates data/archangel.db
npm run dev                 # http://127.0.0.1:8787, restarts on changes
```

For production, run `npm run build`, then `npm start`. Put it behind HTTPS (Caddy or nginx), set
`HOST=0.0.0.0` and `TRUST_PROXY=true`.

To point the client at it, start Minecraft with `-Darchangel.api=https://your.host`. The default
is `http://127.0.0.1:8787`. The WebSocket URL is derived from it (`wss://your.host/ws`).

`npm test` runs the API and WebSocket tests against a throwaway database.

## Local development with offline accounts

Dev clients (`./gradlew runClient`) use offline accounts that Mojang won't vouch for. To test
anyway:

1. Set `ALLOW_UNVERIFIED=true` in `.env`. This enables `POST /auth/dev` and token-less WebSocket
   hellos. **Never** turn it on for a public server.
2. Start the client with `-Darchangel.devAuth=true -Darchangel.allowOfflineUuids=true`.

## How sign-in works

It's the same proof a Minecraft server uses, so the player's Mojang access token never leaves the
game:

1. The client makes a random `serverId` (40 hex characters) and calls Mojang's `joinServer` with it.
2. It sends `POST /auth {username, serverId}`.
3. The backend asks Mojang's `hasJoined?username=…&serverId=…`. If Mojang confirms, the backend
   returns a JWT (`{token, expiresAt, profile}`). Each `serverId` works only once.

The client signs in only while it's already in a world, so it never disturbs a server
handshake. It renews the token before it expires.

## REST API

All bodies are JSON (4 KB max). Errors look like `{"error": "...", "message"?: "..."}`.

| Route | Auth | What it does |
| --- | --- | --- |
| `GET /health` | | `{ok, online}` |
| `POST /auth` | | `{username, serverId}` → `{token, expiresAt, profile}`. 401 if Mojang doesn't confirm or the serverId was used; 502 if Mojang is down. |
| `POST /auth/dev` | | Only with `ALLOW_UNVERIFIED`: `{uuid, username}` → same as `/auth`. |
| `GET /users/:uuid` | | The public profile (below), or 404 for players who never signed in. Either UUID spelling works. |
| `GET /me` | Bearer | Your own profile. |
| `PUT /me/cosmetics` | Bearer | Replaces your loadout, e.g. `{"cape": "cape_nebula", "wings": null}`. Missing or null slots are empty. |
| `PUT /me/profile` | Bearer | `{displayName?, nameColor?}`; null resets a field. |

Public profile:

```json
{ "uuid": "…", "displayName": "Seraph", "nameColor": "#A78BFA", "badge": "archangel",
  "cosmetics": { "cape": "cape_nebula", "halo": "halo_seraph" } }
```

Validation follows the client's own rules (`src/validation.ts`):

- Slots are `cape wings hat halo aura trail hit_effect`.
- Cosmetic ids are `[a-z0-9_]{1,48}`.
- Display names are `[A-Za-z0-9_ ]{1,24}`.
- Colors are `#RRGGBB`.
- Players can't set their own badge.

If a display name differs from the account name, the client always shows the real name in gray
beside it.

## WebSocket `/ws`

Each connection starts with a hello. Players are grouped into rooms by the server address in the
hello, and emotes and presence only go to players in the same room.

Client → server:

- `{"type":"hello","token":"<jwt>","server":"play.example.net"}`
  - The identity comes from the token. Any `uuid` or `name` in the hello is ignored, unless
    `ALLOW_UNVERIFIED` is on and there's no token.
- `{"type":"emote","emote":"wave"}`
- `{"type":"emote_stop"}`
- `{"type":"ping"}`

Server → client:

- `{"type":"welcome","uuid","verified","online":[uuid…]}`
- `{"type":"presence","uuid","online":true|false}`
- `{"type":"emote","uuid","emote"}`, `{"type":"emote_stop","uuid"}`
- `{"type":"profile_update","uuid"}`
  - Sent to everyone sharing a room with that player when they change cosmetics or name. Clients
    then refetch `GET /users/:uuid`.
- `{"type":"pong"}`, `{"type":"error","reason"}`

Limits:

- Messages are 2 KB at most.
- A hello must arrive within 10 s.
- Emotes allow a burst of 4, then 1 every 1.5 s; extras are dropped.
- After 10 malformed or unexpected messages, the server closes the connection (code 4003).
- A second connection by the same player in the same room replaces the first (4002).
- Silent connections are dropped after a missed ping (30 s).

## Rate limiting

There's a global limit (`RATE_LIMIT_PER_MINUTE`, default 120/min per IP) and a stricter one for
sign-in (`AUTH_RATE_LIMIT_PER_MINUTE`, default 10/min). Over the limit you get a 429.

## Layout

```
src/index.ts        entry point (config, database, listen, graceful shutdown)
src/app.ts          routes and plugins; buildApp() is what the tests use
src/realtime.ts     WebSocket hub: rooms, presence, emote relay, profile updates
src/auth.ts         Mojang hasJoined check, replay guard, JWTs
src/validation.ts   input rules shared by REST and WebSocket
src/profiles.ts     database access and the public profile shape
prisma/             schema and migrations (SQLite)
test/               node:test suites (npm test)
```
