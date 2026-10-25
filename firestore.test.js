const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const ALICE_EMAIL = "alice@gmail.com";
const BOB_UID = "bob_456";
const BOB_EMAIL = "bob@gmail.com";
const CHARLIE_UID = "charlie_789";
const CHARLIE_EMAIL = "charlie@gmail.com";

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

// Setup helper for a household
async function setupHousehold(hid = "house_1", ownerUid = ALICE_UID, memberUids = [ALICE_UID, BOB_UID], partnerEmail = BOB_EMAIL) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("households").doc(hid).set({
      name: "הבית שלנו",
      ownerUid,
      memberUids,
      partnerEmail,
      partnerUid: memberUids.includes(BOB_UID) ? BOB_UID : "",
    });
  });
}

// 1. Member can list household items
test("Household member: can create and list household items", async () => {
  await setupHousehold();

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  await assertSucceeds(
    aliceDb.collection("households").doc("house_1").collection("tasks").doc("task_1").set({
      title: "לקנות מצרכים לשבת",
      userId: ALICE_UID
    })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();
  await assertSucceeds(
    bobDb.collection("households").doc("house_1").collection("tasks").get()
  );
});

// 2. Non-member denied
test("Non-member: denied access to household items", async () => {
  await setupHousehold();

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: CHARLIE_EMAIL }).firestore();
  await assertFails(
    charlieDb.collection("households").doc("house_1").collection("tasks").get()
  );
  await assertFails(
    charlieDb.collection("households").doc("house_1").collection("tasks").doc("task_1").get()
  );
});

// 3. Invitee can read only her invite and join
test("Join flow: invitee can read only her own invite and join household", async () => {
  // Alice sets up household without Bob yet
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID], BOB_EMAIL);

  // Alice creates invite for Bob
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("invites").doc(BOB_EMAIL).set({
      householdId: "house_1",
      ownerEmail: ALICE_EMAIL
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: CHARLIE_EMAIL }).firestore();

  // Bob can read his invite
  await assertSucceeds(bobDb.collection("invites").doc(BOB_EMAIL).get());

  // Charlie CANNOT read Bob's invite
  await assertFails(charlieDb.collection("invites").doc(BOB_EMAIL).get());

  // Bob joins household by updating memberUids and partnerUid
  await assertSucceeds(
    bobDb.collection("households").doc("house_1").update({
      memberUids: [ALICE_UID, BOB_UID],
      partnerUid: BOB_UID
    })
  );
});

// 4. Partner cannot change owner or partnerEmail
test("Partner restriction: cannot change ownerUid or partnerEmail", async () => {
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID, BOB_UID], BOB_EMAIL);

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();

  // Bob attempts to change ownerUid to himself
  await assertFails(
    bobDb.collection("households").doc("house_1").update({
      ownerUid: BOB_UID
    })
  );

  // Bob attempts to change partnerEmail
  await assertFails(
    bobDb.collection("households").doc("house_1").update({
      partnerEmail: "intruder@gmail.com"
    })
  );

  // Alice (owner) CAN update partnerEmail
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  await assertSucceeds(
    aliceDb.collection("households").doc("house_1").update({
      partnerEmail: "bob_new@gmail.com"
    })
  );
});

// 5. Private items readable only by owner
test("Private items: stored under users/{uid}/private and readable only by owner", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(ALICE_UID).collection("private").doc("item_1").set({
      title: "פתק אישי סודי",
      userId: ALICE_UID
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();

  // Alice can read her private item
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("item_1").get()
  );

  // Bob (even though household partner) is DENIED access to Alice's private items
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("item_1").get()
  );
});
