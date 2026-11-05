const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const assert = require("node:assert");

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
      ownerEmail: ALICE_EMAIL,
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

// Money pillar: the app's own documents, as its writers produce them (MoneyFixturesTest keeps these in sync).
const path = require("node:path");
const FIXTURES_DIR = path.join(__dirname, "firestore-fixtures", "money");
const MONEY_COLLECTIONS = [
  "cards", "incomes", "fixedExpenses", "subscriptions", "variableBudgets",
  "installments", "periodicBudgets", "debts", "savings", "assets", "categories",
  "transactions", "merchantMemory", "sourceClosures", "goals", "donations", "tithe", "averagesSettings", "closedMonths", "pensionFunds", "car",
];

function fixtures(collection) {
  const dir = path.join(FIXTURES_DIR, collection);
  return fs.readdirSync(dir).filter((f) => f.endsWith(".json")).sort()
    .map((f) => ({ name: f.replace(/\.json$/, ""), doc: JSON.parse(fs.readFileSync(path.join(dir, f), "utf8")) }));
}

// The document id the app would use: settings documents are fixed, closed months are "yyyy-MM".
function docIdFor(collection, index) {
  if (collection === "tithe" || collection === "car" || collection === "averagesSettings") return "settings";
  if (collection === "closedMonths") return `2025-${String(index + 1).padStart(2, "0")}`;
  return `0b7c5f3e-money-${index + 1}`;
}

function moneyDb(uid = ALICE_UID, email = ALICE_EMAIL) {
  return testEnv.authenticatedContext(uid, { email }).firestore().collection("households").doc("house_1");
}

test("Money fixtures: every money collection has fixtures", () => {
  assert.deepStrictEqual(fs.readdirSync(FIXTURES_DIR).sort(), [...MONEY_COLLECTIONS].sort());
});

for (const collection of MONEY_COLLECTIONS) {
  test(`Money ${collection}: every app document is accepted, with and without "v"`, async () => {
    await setupHousehold();
    const house = moneyDb();
    const cases = fixtures(collection);
    assert.ok(cases.length > 0);
    for (const [i, { name, doc }] of cases.entries()) {
      const ref = house.collection(collection).doc(docIdFor(collection, i));
      await assertSucceeds(ref.set(doc)).catch((e) => { throw new Error(`${collection}/${name}: ${e.message}`); });
      const { v, ...withoutVersion } = doc;
      if (collection === "sourceClosures") await assertFails(ref.set(withoutVersion));
      else await assertSucceeds(ref.set(withoutVersion)).catch((e) => { throw new Error(`${collection}/${name} without v: ${e.message}`); });
    }
  });

  test(`Money ${collection}: members read and write, partner edits, stranger denied`, async () => {
    await setupHousehold();
    const [{ doc: body }] = fixtures(collection);
    const doc = (db) => db.collection(collection).doc(docIdFor(collection, 0));

    const aliceDb = moneyDb();
    await assertSucceeds(doc(aliceDb).set(body));
    await assertSucceeds(doc(aliceDb).get());

    const bobDb = moneyDb(BOB_UID, BOB_EMAIL);
    await assertSucceeds(doc(bobDb).get());
    await assertSucceeds(doc(bobDb).set(body));
    await assertSucceeds(bobDb.collection(collection).get());

    const charlieDb = moneyDb(CHARLIE_UID, CHARLIE_EMAIL);
    await assertFails(doc(charlieDb).get());
    await assertFails(doc(charlieDb).set(body));
    await assertFails(doc(charlieDb).delete());
    await assertFails(charlieDb.collection(collection).get());

    const anonDb = testEnv.unauthenticatedContext().firestore().collection("households").doc("house_1");
    await assertFails(doc(anonDb).get());

    await assertSucceeds(doc(bobDb).delete());
  });
}

// A required key each collection's writer always produces.
const REQUIRED = {
  cards: "name", incomes: "titheRate", fixedExpenses: "klass", subscriptions: "renewalDate", variableBudgets: "amounts",
  installments: "total", periodicBudgets: "categoryId", debts: "creditor", savings: "accumulated", assets: "liquid",
  categories: "klass", goals: "allocated", donations: "submittedForTaxRefund", tithe: "startMonth", closedMonths: "closedOn",
  car: "fields", averagesSettings: "v", pensionFunds: "feeOnBalance", transactions: "status", merchantMemory: "key", sourceClosures: "closedFrom",
};
// A number field, pushed past its allowed range (agorot beyond ±₪100M, or a day / count / horizon too big).
const OUT_OF_RANGE = {
  cards: "billingDay", installments: "total", periodicBudgets: "annualAmount", debts: "balance", savings: "accumulated",
  assets: "balance", goals: "amount", donations: "amount", tithe: "opening", closedMonths: "actualIncomes",
  pensionFunds: "value", transactions: "charge", car: "horizonYears",
};
// A month ("yyyy-MM") or a date ("yyyy-MM-dd") field.
const MONTH_OR_DATE = {
  installments: "firstMonth", periodicBudgets: "yearStartMonth", savings: "maturity", goals: "targetMonth", tithe: "startMonth",
  subscriptions: "renewalDate", debts: "asOf", assets: "lastUpdated", donations: "date", closedMonths: "closedOn",
  averagesSettings: "averagesFrom", pensionFunds: "lastUpdated", transactions: "date", sourceClosures: "closedFrom",
};
// An enum field and a value the app never writes.
const ENUM = {
  incomes: "titheRate", fixedExpenses: "frequency", subscriptions: "currency", variableBudgets: "klass", assets: "kind",
  categories: "klass", pensionFunds: "kind", transactions: "status",
};

function wrongType(doc) {
  const numberKey = Object.keys(doc).find((k) => k !== "v" && typeof doc[k] === "number");
  if (numberKey) return { ...doc, [numberKey]: String(doc[numberKey]) };
  const textKey = Object.keys(doc).find((k) => typeof doc[k] === "string");
  return { ...doc, [textKey]: 5 };
}

for (const collection of MONEY_COLLECTIONS) {
  test(`Money ${collection}: malformed documents are denied`, async () => {
    await setupHousehold();
    const ref = moneyDb().collection(collection).doc(docIdFor(collection, 0));
    const cases = fixtures(collection);
    const base = cases.find((c) => !MONTH_OR_DATE[collection] || MONTH_OR_DATE[collection] in c.doc).doc;
    const deny = async (label, doc) => assertFails(ref.set(doc)).catch((e) => { throw new Error(`${collection} ${label}: ${e.message}`); });

    await deny("unknown key", { ...base, hacked: 1 });
    await deny("wrong type", wrongType(base));
    const { [REQUIRED[collection]]: _dropped, ...missing } = base;
    await deny("missing required key", missing);
    await deny("v = 0", { ...base, v: 0 });
    await deny("v as text", { ...base, v: "1" });
    if (OUT_OF_RANGE[collection]) await deny("out of range", { ...base, [OUT_OF_RANGE[collection]]: 10000000001 });
    if (MONTH_OR_DATE[collection]) {
      const key = MONTH_OR_DATE[collection];
      await deny("bad month", { ...base, [key]: base[key].length === 7 ? "2026-13" : "2026-1-05" });
    }
    if (ENUM[collection]) await deny("unknown enum", { ...base, [ENUM[collection]]: "HACKED" });
  });
}

test("Money: settings documents and closed months only under their own ids", async () => {
  await setupHousehold();
  const house = moneyDb();
  await assertFails(house.collection("tithe").doc("other").set(fixtures("tithe")[0].doc));
  await assertFails(house.collection("car").doc("other").set(fixtures("car")[0].doc));
  await assertFails(house.collection("averagesSettings").doc("other").set(fixtures("averagesSettings")[0].doc));
  await assertFails(house.collection("closedMonths").doc("2026-13").set(fixtures("closedMonths")[0].doc));
  await assertFails(house.collection("closedMonths").doc("sept").set(fixtures("closedMonths")[0].doc));
});

test("Money: the app's partial writes (merge and field update) stay valid", async () => {
  await setupHousehold();
  const house = moneyDb();
  const memory = house.collection("merchantMemory").doc("m-3f2a9c0d1e4b5a6c7d8e9f01");
  const learnt = fixtures("merchantMemory").find((c) => c.name === "learnt").doc;
  await assertSucceeds(memory.set(learnt));
  await assertSucceeds(memory.set({ v: 1, key: learnt.key, alias: "סופר" }, { merge: true }));
  await assertSucceeds(memory.set({ v: 1, key: learnt.key, askEachTime: true, categoryId: FieldValue.delete() }, { merge: true }));
  await assertSucceeds(memory.set({ v: 1, key: learnt.key, alias: FieldValue.delete() }, { merge: true }));
  await assertFails(memory.set({ alias: "x".repeat(61) }, { merge: true }));

  const tx = house.collection("transactions").doc("tx-max-1");
  await assertSucceeds(tx.set(fixtures("transactions")[0].doc));
  await assertSucceeds(tx.update("categoryId", "current-2"));
  await assertSucceeds(tx.update("categoryId", FieldValue.delete()));
  await assertFails(tx.update("charge", "a lot"));
});

test("Money: unknown collection, bad id and empty name are denied", async () => {
  await setupHousehold();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
  const house = aliceDb.collection("households").doc("house_1");
  await assertFails(house.collection("secrets").doc("x1").set({ name: "לא" }));
  await assertFails(house.collection("incomes").doc("bad id!").set(fixtures("incomes")[0].doc));
  await assertFails(house.collection("incomes").doc("inc_1").set({ ...fixtures("incomes")[0].doc, name: "" }));
  await assertFails(house.collection("debts").doc("debt_1").set({ ...fixtures("debts")[0].doc, creditor: "" }));
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

test("check-money-docs: reports exactly the documents the rules refuse", async () => {
  const { checkDocs } = require("./tools/check-money-docs.js");
  const good = MONEY_COLLECTIONS.flatMap((collection) =>
    fixtures(collection).map(({ name, doc }, i) => ({ household: "h", collection, id: docIdFor(collection, i), data: doc, name })));
  const broken = [
    { household: "h", collection: "incomes", id: "old-income", data: { name: "משכורת", amount: 100 } },
    { household: "h", collection: "debts", id: "bad-balance", data: { ...fixtures("debts")[0].doc, balance: "1000" } },
    { household: "h", collection: "closedMonths", id: "2026-13", data: fixtures("closedMonths")[0].doc },
  ];
  const refused = await checkDocs(testEnv, [...good, ...broken]);
  assert.deepStrictEqual(refused.map((r) => `${r.collection}/${r.id}`), broken.map((b) => `${b.collection}/${b.id}`));
  assert.strictEqual(refused[0].reason, "unknown keys: amount; missing keys: amounts, titheRate");
});

// Task 13: ownership and deletion order, using the same query shapes as the client.
test('Owner transfers to a member, optionally leaving in the same write', async () => {
  for (const leave of [false, true]) {
    await setupHousehold();
    const db = testEnv.authenticatedContext(ALICE_UID, { email: ALICE_EMAIL }).firestore();
    await assertSucceeds(db.doc('households/house_1').update({
      ownerUid: BOB_UID, ownerEmail: BOB_EMAIL,
      memberUids: leave ? [BOB_UID] : [ALICE_UID, BOB_UID],
      partnerUid: leave ? '' : ALICE_UID, partnerEmail: leave ? '' : ALICE_EMAIL,
    }));
  }
});
test('Ownership transfer rejects outsiders, non-owners and inconsistent partners', async () => {
  await setupHousehold();
  const alice = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bob = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(alice.doc('households/house_1').update({ ownerUid: CHARLIE_UID, memberUids: [ALICE_UID, BOB_UID, CHARLIE_UID] }));
  await assertFails(bob.doc('households/house_1').update({ ownerUid: BOB_UID, partnerUid: ALICE_UID }));
  await assertFails(alice.doc('households/house_1').update({ ownerUid: BOB_UID }));
  await assertFails(alice.doc('households/house_1').update({ ownerUid: BOB_UID, memberUids: [ALICE_UID], partnerUid: '' }));
});
test('Member leaves with cleared partner metadata, cannot remove another member', async () => {
  await setupHousehold();
  const db = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(db.doc('households/house_1').update({ memberUids: [BOB_UID], partnerUid: '', partnerEmail: '' }));
  await assertSucceeds(db.doc('households/house_1').update({ memberUids: [ALICE_UID], partnerUid: '', partnerEmail: '' }));
});
test('Owner lists and deletes household invites; another member cannot', async () => {
  await setupHousehold();
  await testEnv.withSecurityRulesDisabled(async c => {
    await c.firestore().doc('invites/test@example.com').set({ householdId: 'house_1' });
  });
  const owner = testEnv.authenticatedContext(ALICE_UID).firestore();
  const member = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertSucceeds(owner.collection('invites').where('householdId', '==', 'house_1').get());
  await assertFails(owner.collection('invites').get());
  await assertFails(member.collection('invites').where('householdId', '==', 'house_1').get());
  await assertFails(member.doc('invites/test@example.com').delete());
  await assertSucceeds(owner.doc('invites/test@example.com').delete());
});
test('Household must be deleted LAST: former members cannot read or delete ANY orphan collection', async () => {
  await setupHousehold();
  const money = [...fs.readFileSync('firestore.rules', 'utf8').match(/function isMoneyCollection\(name\)[\s\S]*?\[([\s\S]*?)\]/)[1].matchAll(/'([^']+)'/g)].map(m => m[1]);
  const collections = [...money, 'tasks', 'dreams', 'inbox'];
  await testEnv.withSecurityRulesDisabled(async c => {
    for (const name of collections) await c.firestore().doc(`households/house_1/${name}/orphan`).set({});
  });
  const owner = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(owner.doc('households/house_1').delete());
  for (const uid of [ALICE_UID, BOB_UID]) {
    const db = testEnv.authenticatedContext(uid).firestore();
    for (const name of collections) {
      const ref = db.doc(`households/house_1/${name}/orphan`);
      await assertFails(ref.get());
      await assertFails(ref.delete());
      await assertFails(ref.parent.get());
    }
  }
});
test('Deletion lock freezes shared writers and invites but permits draining and final deletion', async () => {
  await setupHousehold();
  const owner = testEnv.authenticatedContext(ALICE_UID, {email: ALICE_EMAIL}).firestore();
  const member = testEnv.authenticatedContext(BOB_UID, {email: BOB_EMAIL}).firestore();
  await assertFails(member.doc('households/house_1').update({deleting: true}));
  await assertSucceeds(owner.doc('households/house_1/tasks/old').set({title:'old',userId:ALICE_UID}));
  await assertSucceeds(owner.doc('households/house_1').update({deleting:true}));
  await assertFails(owner.doc('households/house_1').update({deleting:false}));
  await assertFails(member.doc('households/house_1/tasks/new').set({title:'new',userId:BOB_UID}));
  await assertFails(owner.doc('invites/new@example.com').set({householdId:'house_1'}));
  await assertSucceeds(owner.doc('households/house_1/tasks/old').delete());
  await assertSucceeds(owner.doc('households/house_1').delete());
});

test('Client deletion inventory exactly matches isMoneyCollection', () => {
  const rules = fs.readFileSync('firestore.rules', 'utf8');
  const kotlin = fs.readFileSync('app/src/main/java/com/menash/myself/core/data/account/AccountDeletion.kt', 'utf8');
  const moneyRules = [...rules.match(/function isMoneyCollection\(name\)[\s\S]*?\[([\s\S]*?)\]/)[1].matchAll(/'([^']+)'/g)].map(m => m[1]).sort();
  const moneyClient = [...kotlin.match(/val money = listOf\(([\s\S]*?)\)/)[1].matchAll(/"([^"]+)"/g)].map(m => m[1]).sort();
  assert.deepStrictEqual(moneyClient, moneyRules);
});

test('Online transaction batches delete 500 documents while the household still exists', async () => {
  await setupHousehold();
  await testEnv.withSecurityRulesDisabled(async c => {
    const batch = c.firestore().batch();
    for (let i = 0; i < 500; i++) batch.set(c.firestore().doc(`households/house_1/tasks/t${i}`), {title:'task',userId:ALICE_UID});
    await batch.commit();
  });
  const db = testEnv.authenticatedContext(ALICE_UID, {email: ALICE_EMAIL}).firestore();
  await db.runTransaction(async tx => {
    const ref = db.doc('households/house_1');
    assert.strictEqual((await tx.get(ref)).data().ownerUid, ALICE_UID);
    tx.update(ref, {deleting:true});
  });
  const docs = await db.collection('households/house_1/tasks').limit(500).get({source:'server'});
  await assertSucceeds(db.runTransaction(async tx => {
    await tx.get(db.doc('households/house_1'));
    for (const doc of docs.docs) tx.delete(doc.ref);
  }));
  assert.strictEqual((await db.collection('households/house_1/tasks').get()).size, 0);
  await assertSucceeds(db.doc('households/house_1').delete());
});

test('Removed partner observes an empty membership query and can create a replacement household', async () => {
  for (const deleted of [true, false]) {
    await setupHousehold();
    const owner = testEnv.authenticatedContext(ALICE_UID, {email:ALICE_EMAIL}).firestore();
    const partner = testEnv.authenticatedContext(BOB_UID, {email:BOB_EMAIL}).firestore();
    if (deleted) await owner.doc('households/house_1').delete();
    else await owner.doc('households/house_1').update({memberUids:[ALICE_UID],partnerUid:'',partnerEmail:''});
    const query = await assertSucceeds(partner.collection('households').where('memberUids', 'array-contains', BOB_UID).get({source:'server'}));
    assert.strictEqual(query.size, 0);
    await assertSucceeds(partner.doc('households/replacement').set({ownerUid:BOB_UID,memberUids:[BOB_UID],partnerUid:'',partnerEmail:''}));
    await testEnv.clearFirestore();
  }
});


test("Source closures: validate dates, required fields and household access", async () => {
  await setupHousehold();
  const doc = moneyDb().collection("sourceClosures").doc("s-123");
  const valid = {v: 1, source: "MAX", sourceId: "max-1234", closedFrom: "2024-06"};
  await assertSucceeds(doc.set(valid));
  await assertFails(doc.set({...valid, closedFrom: "2024-13"}));
  await assertFails(doc.set({...valid, source: "OTHER"}));
  await assertFails(doc.set({...valid, sourceId: "x".repeat(201)}));
  await assertFails(doc.set({...valid, extra: true}));
  await assertFails(doc.update({closedFrom: FieldValue.delete()}));
  await assertFails(moneyDb(CHARLIE_UID, CHARLIE_EMAIL).collection("sourceClosures").doc("s-123").get());
  await assertFails(moneyDb(CHARLIE_UID, CHARLIE_EMAIL).collection("sourceClosures").doc("s-123").set(valid));
  await assertSucceeds(doc.delete());
});


test("Savings: optional startMonth must be a valid month on create and update", async () => {
  await setupHousehold();
  const doc = moneyDb().collection("savings").doc("saving-start");
  const valid = {v: 1, name: "חיסכון", monthlyDeposit: 100, accumulated: 200};
  await assertSucceeds(doc.set(valid));
  await assertSucceeds(doc.update({startMonth: "2026-01"}));
  await assertFails(doc.update({startMonth: "2026-13"}));
  await assertFails(doc.set({...valid, startMonth: 202601}));
  await assertSucceeds(doc.update({startMonth: FieldValue.delete()}));
});
