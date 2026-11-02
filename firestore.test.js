const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

const firebase = require("firebase/compat/app");
require("firebase/compat/firestore");
const FieldValue = firebase.firestore.FieldValue;

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
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID], BOB_EMAIL);

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

// 4. Negative case: Stranger with no invite cannot join
test("Join flow: stranger with no invite cannot join", async () => {
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID], BOB_EMAIL);

  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: CHARLIE_EMAIL }).firestore();
  await assertFails(
    charlieDb.collection("households").doc("house_1").update({
      memberUids: [ALICE_UID, CHARLIE_UID],
      partnerUid: CHARLIE_UID
    })
  );
});

// 5. Negative case: Invitee with invite to house_1 cannot join house_2
test("Join flow: invitee with invite to house_1 cannot join house_2", async () => {
  // Alice owns house_1 and invites Bob
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID], BOB_EMAIL);
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("invites").doc(BOB_EMAIL).set({
      householdId: "house_1",
      ownerEmail: ALICE_EMAIL
    });
    // Charlie owns house_2
    await context.firestore().collection("households").doc("house_2").set({
      name: "בית 2",
      ownerUid: CHARLIE_UID,
      memberUids: [CHARLIE_UID],
      partnerEmail: "",
      partnerUid: ""
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();
  // Bob tries to join house_2 where he has NO invite
  await assertFails(
    bobDb.collection("households").doc("house_2").update({
      memberUids: [CHARLIE_UID, BOB_UID],
      partnerUid: BOB_UID
    })
  );
});

// 6. Negative case: Partner cannot remove owner or add third UID
test("Membership: partner cannot remove owner from memberUids or add another uid", async () => {
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID, BOB_UID], BOB_EMAIL);

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();

  // Partner attempts to remove the owner Alice
  await assertFails(
    bobDb.collection("households").doc("house_1").update({
      memberUids: [BOB_UID]
    })
  );

  // Partner attempts to add Charlie
  await assertFails(
    bobDb.collection("households").doc("house_1").update({
      memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID]
    })
  );

  // Partner CAN remove himself (leave the household)
  await assertSucceeds(
    bobDb.collection("households").doc("house_1").update({
      memberUids: [ALICE_UID]
    })
  );
});

// 7. Positive case: Join via arrayUnion succeeds without prior read, and invite can be deleted
test("Join flow: join via arrayUnion succeeds without prior read, and invite can be deleted", async () => {
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID], BOB_EMAIL);

  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("invites").doc(BOB_EMAIL).set({
      householdId: "house_1",
      ownerEmail: ALICE_EMAIL
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();

  // Non-member cannot read before joining
  await assertFails(bobDb.collection("households").doc("house_1").get());

  // Join directly via arrayUnion without reading
  await assertSucceeds(
    bobDb.collection("households").doc("house_1").update({
      memberUids: FieldValue.arrayUnion(BOB_UID),
      partnerUid: BOB_UID
    })
  );

  // After join succeeds, Bob is a member and can read the household
  await assertSucceeds(bobDb.collection("households").doc("house_1").get());

  // Invitee can delete her invite after joining
  await assertSucceeds(bobDb.collection("invites").doc(BOB_EMAIL).delete());
});

// 8. Partner cannot change owner or partnerEmail
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

// 9. Items create & update: require incoming().userId == request.auth.uid and userId immutable
test("Items: require userId == auth.uid on create, and userId cannot change on update", async () => {
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID, BOB_UID], BOB_EMAIL);

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();

  // Alice cannot create a task impersonating Bob
  await assertFails(
    aliceDb.collection("households").doc("house_1").collection("tasks").doc("task_spoof").set({
      title: "משימה מתחזה",
      userId: BOB_UID
    })
  );

  // Alice creates a task with her own uid
  await assertSucceeds(
    aliceDb.collection("households").doc("house_1").collection("tasks").doc("task_alice").set({
      title: "משימה של אליס",
      userId: ALICE_UID
    })
  );

  // Alice cannot change userId to Bob's uid on update
  await assertFails(
    aliceDb.collection("households").doc("house_1").collection("tasks").doc("task_alice").update({
      userId: BOB_UID
    })
  );

  // Alice can update other task fields leaving userId unchanged
  await assertSucceeds(
    aliceDb.collection("households").doc("house_1").collection("tasks").doc("task_alice").update({
      title: "משימה מעודכנת של אליס"
    })
  );
});

// 10. Private items readable only by owner
test("Private items: stored under users/{uid}/private and readable only by owner; partner is denied", async () => {
  await setupHousehold("house_1", ALICE_UID, [ALICE_UID, BOB_UID], BOB_EMAIL);

  await testEnv.withSecurityRulesDisabled(async (context) => {
    // Owner Alice creates a private task under users/{uid}/private/tasks/items/{id}
    await context.firestore()
      .collection("users")
      .doc(ALICE_UID)
      .collection("private")
      .doc("tasks")
      .collection("items")
      .doc("task_private_alice")
      .set({
        title: "משימה אישית סודית של אליס",
        userId: ALICE_UID,
        space: "private"
      });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();

  // Alice can read her private task
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("tasks").collection("items").doc("task_private_alice").get()
  );

  // Bob (household partner) is DENIED access to Alice's private task
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("tasks").collection("items").doc("task_private_alice").get()
  );

  // Bob is also DENIED listing Alice's private tasks subcollection
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("private").doc("tasks").collection("items").get()
  );
});

// Money pillar: every collection is shared by household members and closed to everyone else.
const MONEY_COLLECTIONS = [
  "cards", "incomes", "fixedExpenses", "subscriptions", "variableBudgets",
  "installments", "periodicBudgets", "debts", "savings", "assets", "categories",
  "transactions", "merchantMemory", "goals", "donations", "tithe", "closedMonths", "pensionFunds", "car",
];

for (const collection of MONEY_COLLECTIONS) {
  test(`Money ${collection}: members read and write, partner edits, stranger denied`, async () => {
    await setupHousehold();
    const doc = (db) => db.collection("households").doc("house_1").collection(collection).doc("0b7c5f3e-money-1");

    const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
    await assertSucceeds(doc(aliceDb).set({ name: "פריט", amount: 125000 }));
    await assertSucceeds(doc(aliceDb).get());

    const bobDb = testEnv.authenticatedContext(BOB_UID, { email: BOB_EMAIL }).firestore();
    await assertSucceeds(doc(bobDb).get());
    await assertSucceeds(doc(bobDb).set({ name: "פריט מעודכן", amount: 130000 }));
    await assertSucceeds(bobDb.collection("households").doc("house_1").collection(collection).get());

    const charlieDb = testEnv.authenticatedContext(CHARLIE_UID, { email: CHARLIE_EMAIL }).firestore();
    await assertFails(doc(charlieDb).get());
    await assertFails(doc(charlieDb).set({ name: "זר" }));
    await assertFails(doc(charlieDb).delete());
    await assertFails(charlieDb.collection("households").doc("house_1").collection(collection).get());

    const anonDb = testEnv.unauthenticatedContext().firestore();
    await assertFails(doc(anonDb).get());

    await assertSucceeds(doc(bobDb).delete());
  });
}

test("Money: unknown collection, bad id and empty name are denied", async () => {
  await setupHousehold();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  const house = aliceDb.collection("households").doc("house_1");
  await assertFails(house.collection("secrets").doc("x1").set({ name: "לא" }));
  await assertFails(house.collection("incomes").doc("bad id!").set({ name: "משכורת" }));
  await assertFails(house.collection("incomes").doc("inc_1").set({ name: "" }));
  await assertFails(house.collection("debts").doc("debt_1").set({ creditor: "" }));
});

test("Import: a full transaction and a merchant memory document are accepted, oversized ones denied", async () => {
  await setupHousehold();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  const house = aliceDb.collection("households").doc("house_1");
  const tx = {
    source: "MAX", sourceId: "max-1234", date: "2026-10-12", chargeDate: "2026-11-02",
    description: "מחסני חשמל", details: "תשלום 2 מתוך 3", charge: 40000, originalAmount: 120000,
    originalCurrency: "ILS", status: "COUNTED", installmentNumber: 2, installmentOf: 3,
    hint: "חשמל ומחשבים", operation: "תשלומים", categoryId: "current-3",
  };
  await assertSucceeds(house.collection("transactions").doc("tx-max-06318c8ac6ae5d5a81f53ca2").set(tx));
  await assertSucceeds(house.collection("transactions").doc("tx-max-06318c8ac6ae5d5a81f53ca2").set(tx));
  await assertSucceeds(house.collection("merchantMemory").doc("m-3f2a9c0d1e4b5a6c7d8e9f01")
    .set({ key: "שופרסל דיל", categoryId: "current-1", ownTransfer: false }));
  const oversized = { ...tx };
  for (let i = 0; i < 10; i++) oversized[`extra${i}`] = i;
  await assertFails(house.collection("transactions").doc("tx-max-oversized").set(oversized));
  await assertFails(house.collection("transactions").doc("tx max!").set(tx));
});

// Legacy calculator data is retained for owner reads/deletion, never writable.
test("calculator_sync: owner create denied", async () => {
  const db = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertFails(db.collection("calculator_sync").doc("new").set({ userId: ALICE_UID }));
});
test("calculator_sync: existing data is read/delete only for its owner", async () => {
  await testEnv.withSecurityRulesDisabled(async context => {
    await context.firestore().collection("calculator_sync").doc("old").set({userId: ALICE_UID, value: 1});
  });
  const owner = testEnv.authenticatedContext(ALICE_UID).firestore().collection("calculator_sync");
  const other = testEnv.authenticatedContext(BOB_UID).firestore().collection("calculator_sync");
  await assertFails(owner.doc("old").update({value: 2}));
  await assertFails(owner.doc("old").update({userId: BOB_UID}));
  await assertSucceeds(owner.doc("old").get());
  await assertSucceeds(owner.where("userId", "==", ALICE_UID).get());
  await assertFails(other.doc("old").get());
  await assertFails(other.where("userId", "==", ALICE_UID).get());
  await assertFails(other.doc("old").delete());
  await assertFails(testEnv.unauthenticatedContext().firestore().collection("calculator_sync").get());
  await assertSucceeds(owner.doc("old").delete());
});
