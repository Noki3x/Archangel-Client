import { after, before, describe, test } from "node:test";
import assert from "node:assert/strict";
import { ALICE, BOB, connect, startServer } from "./helpers.js";

describe("WebSocket hub", () => {
  let server: Awaited<ReturnType<typeof startServer>>;
  let aliceToken: string;
  let bobToken: string;
  before(async () => {
    server = await startServer();
    aliceToken = await server.signIn(ALICE);
    bobToken = await server.signIn(BOB);
  });
  after(async () => server.close());

  test("a hello without a token is refused unless ALLOW_UNVERIFIED is on", async () => {
    const c = await connect(server.port);
    c.send({ type: "hello", uuid: ALICE.uuid, name: "Alice", server: "play.example.net" });
    assert.equal((await c.next("error")).reason, "auth_required");
    assert.equal(await c.closed(), 4001);
  });

  test("a bad token is refused", async () => {
    const c = await connect(server.port);
    c.send({ type: "hello", token: "junk", server: "play.example.net" });
    assert.equal((await c.next("error")).reason, "invalid_token");
  });

  test("the identity comes from the token, not from what the client claims", async () => {
    const c = await connect(server.port);
    c.send({ type: "hello", token: aliceToken, uuid: BOB.uuid, name: "Bob", server: "play.example.net" });
    const welcome = await c.next("welcome");
    assert.equal(welcome.uuid, ALICE.uuid);
    assert.equal(welcome.verified, true);
    c.close();
  });

  test("emotes and presence stay inside a server, and profile changes reach viewers", async () => {
    const alice = await connect(server.port);
    alice.send({ type: "hello", token: aliceToken, server: "Play.Example.net" });
    assert.deepEqual((await alice.next("welcome")).online, []);

    const bob = await connect(server.port);
    bob.send({ type: "hello", token: bobToken, server: "play.example.net" });
    assert.deepEqual((await bob.next("welcome")).online, [ALICE.uuid]);
    assert.deepEqual(await alice.next("presence"), { type: "presence", uuid: BOB.uuid, online: true });

    const elsewhere = await connect(server.port);
    const carolToken = await server.signIn({ uuid: "99999999-8888-4777-8666-555555555555", name: "Carol" });
    elsewhere.send({ type: "hello", token: carolToken, server: "other.example.net" });
    await elsewhere.next("welcome");

    alice.send({ type: "emote", emote: "wave" });
    assert.deepEqual(await bob.next("emote"), { type: "emote", uuid: ALICE.uuid, emote: "wave" });
    alice.send({ type: "emote_stop" });
    assert.deepEqual(await bob.next("emote_stop"), { type: "emote_stop", uuid: ALICE.uuid });

    const res = await server.app.inject({
      method: "PUT",
      url: "/me/cosmetics",
      headers: { authorization: `Bearer ${aliceToken}` },
      payload: { halo: "halo_seraph" },
    });
    assert.equal(res.statusCode, 200);
    assert.deepEqual(await bob.next("profile_update"), { type: "profile_update", uuid: ALICE.uuid });
    assert.deepEqual(await alice.next("profile_update"), { type: "profile_update", uuid: ALICE.uuid });

    assert.deepEqual(await elsewhere.quiet(), [], "nothing leaks to another server");

    bob.close();
    assert.deepEqual(await alice.next("presence"), { type: "presence", uuid: BOB.uuid, online: false });
    alice.close();
    elsewhere.close();
  });

  test("emote floods are dropped and junk gets the connection closed", async () => {
    const alice = await connect(server.port);
    alice.send({ type: "hello", token: aliceToken, server: "flood.example.net" });
    await alice.next("welcome");
    const bob = await connect(server.port);
    bob.send({ type: "hello", token: bobToken, server: "flood.example.net" });
    await bob.next("welcome");
    await alice.next("presence");

    for (let i = 0; i < 20; i++) alice.send({ type: "emote", emote: "dab" });
    const got = (await bob.quiet(300)).filter((t) => t === "emote").length;
    assert.ok(got >= 1 && got <= 5, `relayed ${got} of 20`);

    alice.send({ type: "emote", emote: "../../etc" });
    for (let i = 0; i < 12; i++) alice.ws.send("not json");
    assert.equal(await alice.closed(), 4003);
    bob.close();
  });

  test("a second connection for the same player replaces the first", async () => {
    const first = await connect(server.port);
    first.send({ type: "hello", token: aliceToken, server: "dupe.example.net" });
    await first.next("welcome");
    const second = await connect(server.port);
    second.send({ type: "hello", token: aliceToken, server: "dupe.example.net" });
    await second.next("welcome");
    assert.equal(await first.closed(), 4002);
    second.close();
  });
});

describe("WebSocket hub in development mode", () => {
  let server: Awaited<ReturnType<typeof startServer>>;
  before(async () => (server = await startServer({ ALLOW_UNVERIFIED: "true" })));
  after(async () => server.close());

  test("ALLOW_UNVERIFIED accepts the claimed uuid, marked unverified", async () => {
    const c = await connect(server.port);
    c.send({ type: "hello", uuid: ALICE.uuid.replaceAll("-", ""), name: "Alice", server: "local" });
    const welcome = await c.next("welcome");
    assert.equal(welcome.uuid, ALICE.uuid);
    assert.equal(welcome.verified, false);
    c.close();
  });

  test("dev sign-in works for offline accounts", async () => {
    const res = await server.app.inject({ method: "POST", url: "/auth/dev", payload: { uuid: BOB.uuid, username: "Bob" } });
    assert.equal(res.statusCode, 200);
    assert.ok(res.json().token);
  });
});
