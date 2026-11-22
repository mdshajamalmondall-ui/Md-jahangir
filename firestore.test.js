const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_lumina_123";
const BOB_UID = "bob_lumina_456";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write preferences, favorites, or albums", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const now = new Date(Date.now() - 1000);

  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      themePreset: "OBSIDIAN_NEON",
      darkModeEnabled: true,
      gridColumns: 3,
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Owner can create and partially update UserPreference; rejects ghost fields & immutable tampering", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Prism",
      themePreset: "OBSIDIAN_NEON",
      darkModeEnabled: true,
      gridColumns: 3,
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themePreset: "AURORA_GLASS",
      gridColumns: 4,
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject invalid themePreset
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      themePreset: "INVALID_THEME",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: new Date(Date.now() - 500),
      updatedAt: new Date(Date.now() - 500),
    })
  );

  // Reject ghost field
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
});

test("Owner can create, list, and delete CloudFavorite; rejects Drive ID poisoning or updates", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date(Date.now() - 1000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").doc("fav_drive_1").set({
      userId: ALICE_UID,
      photoKey: "drive:1abcDEF",
      title: "Sunset.jpg",
      sourceType: "DRIVE",
      driveFileId: "1abcDEF",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").get()
  );

  // Reject extra `id` field (mirrors @DocumentId exclusion)
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").doc("fav_drive_2").set({
      id: "fav_drive_2",
      userId: ALICE_UID,
      photoKey: "drive:2xyz",
      title: "ExtraId.jpg",
      sourceType: "DRIVE",
      createdAt: now,
    })
  );

  // Updates are disallowed on favorites
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").doc("fav_drive_1").update({
      title: "Modified",
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").doc("fav_drive_1").delete()
  );
});

test("Owner can create, list, update, and delete CloudAlbum; rejects future timestamp", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const created = new Date(Date.now() - 5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").doc("album_travel").set({
      userId: ALICE_UID,
      name: "Nordic Trip",
      accentHex: "#00E5FF",
      photoKeys: ["curated:aurora", "drive:1abcDEF"],
      createdAt: created,
      updatedAt: created,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").doc("album_travel").update({
      name: "Nordic Winter",
      photoKeys: ["curated:aurora", "drive:1abcDEF", "curated:coastline"],
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").get()
  );

  // Reject future timestamp
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").doc("album_future").set({
      userId: ALICE_UID,
      name: "Future",
      accentHex: "#FF6B6B",
      photoKeys: [],
      createdAt: new Date(Date.now() + 120000),
      updatedAt: new Date(Date.now() + 120000),
    })
  );
});

test("Cross-user isolation: Bob cannot read or write Alice's data", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    const now = new Date(Date.now() - 5000);
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice",
      themePreset: "OBSIDIAN_NEON",
      darkModeEnabled: true,
      gridColumns: 3,
      createdAt: now,
      updatedAt: now,
    });
    await db.collection("users").doc(ALICE_UID).collection("cloudAlbums").doc("album_1").set({
      userId: ALICE_UID,
      name: "Private",
      accentHex: "#FF6B6B",
      photoKeys: [],
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cloudFavorites").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("cloudAlbums").doc("album_1").update({
      name: "Hijacked",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
});
