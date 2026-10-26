import { after, before, describe, test } from "node:test";
import assert from "node:assert/strict";
import { ALICE, BOB, randomHash, startServer } from "./helpers.js";

describe("REST API", () => {
  let server: Awaited<ReturnType<typeof startServer>>;
  before(async () => (server = await startServer()));
  after(async () => server.close());

  test("unknown players are 404, so the client treats them as non-Archangel", async () => {
    const res = await server.app.inject({ url: `/users/${BOB.uuid}` });
    assert.equal(res.statusCode, 404);
  });

  test("malformed UUIDs are rejected", async () => {
    const res = await server.app.inject({ url: "/users/not-a-uuid" });
    assert.equal(res.statusCode, 400);
  });

  test("sign-in needs a matching Mojang join", async () => {
    const res = await server.app.inject({ method: "POST", url: "/auth", payload: { username: "Alice", serverId: randomHash() } });
    assert.equal(res.statusCode, 401);
    assert.equal(res.json().error, "not_verified");
  });

  test("sign-in validates its input", async () => {
    const res = await server.app.inject({ method: "POST", url: "/auth", payload: { username: "no spaces allowed", serverId: "zz" } });
    assert.equal(res.statusCode, 400);
  });

  test("sign-in issues a token and creates the profile", async () => {
    const serverId = randomHash();
    server.mojang.join(ALICE, serverId);
    const res = await server.app.inject({ method: "POST", url: "/auth", payload: { username: "Alice", serverId } });
    assert.equal(res.statusCode, 200);
    const body = res.json();
    assert.ok(body.token);
    assert.deepEqual(body.profile, { uuid: ALICE.uuid, displayName: "Alice", badge: "archangel", cosmetics: {} });

    // the same serverId can't be used twice
    const again = await server.app.inject({ method: "POST", url: "/auth", payload: { username: "Alice", serverId } });
    assert.equal(again.statusCode, 401);
    assert.equal(again.json().error, "server_id_reused");

    // and the profile is public now, by either UUID spelling
    const profile = await server.app.inject({ url: `/users/${ALICE.uuid.replaceAll("-", "").toUpperCase()}` });
    assert.equal(profile.statusCode, 200);
    assert.equal(profile.json().displayName, "Alice");
  });

  test("Mojang being down is a 502, not a crash", async () => {
    server.mojang.down = true;
    try {
      const res = await server.app.inject({ method: "POST", url: "/auth", payload: { username: "Alice", serverId: randomHash() } });
      assert.equal(res.statusCode, 502);
    } finally {
      server.mojang.down = false;
    }
  });

  test("/me routes need a valid token", async () => {
    for (const authorization of [undefined, "Bearer nonsense", "Basic abc"]) {
      const res = await server.app.inject({ method: "PUT", url: "/me/cosmetics", payload: {}, headers: authorization ? { authorization } : {} });
      assert.equal(res.statusCode, 401);
    }
  });

  test("cosmetics are saved and validated", async () => {
    const token = await server.signIn(ALICE);
    const headers = { authorization: `Bearer ${token}` };
    const ok = await server.app.inject({
      method: "PUT",
      url: "/me/cosmetics",
      headers,
      payload: { cape: "cape_nebula", wings: "wings_seraph", halo: null },
    });
    assert.equal(ok.statusCode, 200);
    assert.deepEqual(ok.json().cosmetics, { cape: "cape_nebula", wings: "wings_seraph" });

    const pub = await server.app.inject({ url: `/users/${ALICE.uuid}` });
    assert.deepEqual(pub.json().cosmetics, { cape: "cape_nebula", wings: "wings_seraph" });

    for (const payload of [{ jetpack: "x" }, { cape: "Cape With Spaces" }, { cape: 5 }, { cape: "a".repeat(49) }]) {
      const bad = await server.app.inject({ method: "PUT", url: "/me/cosmetics", headers, payload });
      assert.equal(bad.statusCode, 400, JSON.stringify(payload));
    }
  });

  test("display name and color follow the client's rules", async () => {
    const token = await server.signIn(ALICE);
    const headers = { authorization: `Bearer ${token}` };
    const ok = await server.app.inject({ method: "PUT", url: "/me/profile", headers, payload: { displayName: "  Seraph  ", nameColor: "#A78BFA" } });
    assert.equal(ok.statusCode, 200);
    assert.equal(ok.json().displayName, "Seraph");
    assert.equal(ok.json().nameColor, "#A78BFA");

    for (const payload of [{ displayName: "§cAdmin" }, { displayName: "x".repeat(25) }, { nameColor: "red" }, { badge: "staff" }]) {
      const bad = await server.app.inject({ method: "PUT", url: "/me/profile", headers, payload });
      assert.equal(bad.statusCode, 400, JSON.stringify(payload));
    }

    const cleared = await server.app.inject({ method: "PUT", url: "/me/profile", headers, payload: { displayName: null, nameColor: null } });
    assert.equal(cleared.json().displayName, "Alice");
    assert.equal(cleared.json().nameColor, undefined);
  });

  test("dev sign-in does not exist in production mode", async () => {
    const res = await server.app.inject({ method: "POST", url: "/auth/dev", payload: { uuid: BOB.uuid, username: "Bob" } });
    assert.equal(res.statusCode, 404);
  });

  test("oversized bodies are refused", async () => {
    const token = await server.signIn(ALICE);
    const res = await server.app.inject({
      method: "PUT",
      url: "/me/profile",
      headers: { authorization: `Bearer ${token}`, "content-type": "application/json" },
      payload: JSON.stringify({ displayName: "x".repeat(10_000) }),
    });
    assert.equal(res.statusCode, 413);
  });
});

describe("rate limiting", () => {
  let server: Awaited<ReturnType<typeof startServer>>;
  before(async () => (server = await startServer({ AUTH_RATE_LIMIT_PER_MINUTE: "3" })));
  after(async () => server.close());

  test("sign-in attempts are limited per minute", async () => {
    const codes: number[] = [];
    for (let i = 0; i < 5; i++) {
      const res = await server.app.inject({ method: "POST", url: "/auth", payload: { username: "Alice", serverId: randomHash() } });
      codes.push(res.statusCode);
    }
    assert.deepEqual(codes, [401, 401, 401, 429, 429]);
  });
});
