#!/usr/bin/env node
// Read-only check: which money documents in the LIVE database would the rules in ./firestore.rules refuse?
// Run it BEFORE merging a firestore.rules change (merging to main deploys the rules).
//
// It never writes to the live database. It reads every money document of every household (or one, with
// --household <id>), then replays each one as a household member against a local Firestore emulator that
// runs the new rules. A refused document is printed with its collection, id and a hint (unknown or missing
// keys compared with the app's fixtures). Values are never printed.
//
//   GOOGLE_APPLICATION_CREDENTIALS=/path/to/service-account.json \
//   firebase emulators:exec --only firestore --project demo-no-project "node tools/check-money-docs.js"
//
// Needs: firebase-tools, firebase, firebase-admin, @firebase/rules-unit-testing (npm), Java for the emulator.
// Exit code 0 = no refused documents, 1 = at least one, 2 = could not run.
// Limitation: the Admin SDK reads 5.0 and 5 the same, so a whole-number double in the cloud is replayed as an int.

const fs = require("node:fs");
const path = require("node:path");

const ROOT = path.join(__dirname, "..");
const DATABASE = JSON.parse(fs.readFileSync(path.join(ROOT, "firebase.json"), "utf8")).firestore[0].database;
const FIXTURES_DIR = path.join(ROOT, "firestore-fixtures", "money");
const MONEY_COLLECTIONS = fs.readdirSync(FIXTURES_DIR).filter((f) => fs.statSync(path.join(FIXTURES_DIR, f)).isDirectory()).sort();

const CHECKER_UID = "check-money-docs";
const CHECK_HOUSEHOLD = "check-household";

/** Keys the app writes for each collection: every key seen in its fixtures, and the keys every fixture has. */
function fixtureKeys(collection) {
  const dir = path.join(FIXTURES_DIR, collection);
  const docs = fs.readdirSync(dir).filter((f) => f.endsWith(".json")).map((f) => JSON.parse(fs.readFileSync(path.join(dir, f), "utf8")));
  const all = new Set(docs.flatMap((d) => Object.keys(d)));
  const always = [...all].filter((k) => k !== "v" && docs.every((d) => k in d));
  return { all, always };
}

function hint(collection, data) {
  if (!MONEY_COLLECTIONS.includes(collection)) return "not a money collection";
  const { all, always } = fixtureKeys(collection);
  const unknown = Object.keys(data).filter((k) => !all.has(k));
  const missing = always.filter((k) => !(k in data));
  const parts = [];
  if (unknown.length) parts.push(`unknown keys: ${unknown.join(", ")}`);
  if (missing.length) parts.push(`missing keys: ${missing.join(", ")}`);
  return parts.length ? parts.join("; ") : "a value has the wrong type, size, range or format";
}

/**
 * Replays [docs] ({collection, id, data}) against [testEnv]'s emulator; returns the refused ones with a hint.
 * Each document is written into a scratch household where the checker is a member, so only the
 * document's own shape decides.
 */
async function checkDocs(testEnv, docs) {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("households").doc(CHECK_HOUSEHOLD)
      .set({ ownerUid: CHECKER_UID, memberUids: [CHECKER_UID] });
  });
  const house = testEnv.authenticatedContext(CHECKER_UID).firestore().collection("households").doc(CHECK_HOUSEHOLD);
  const refused = [];
  for (const doc of docs) {
    try {
      await house.collection(doc.collection).doc(doc.id).set(doc.data);
    } catch (e) {
      if (e.code !== "permission-denied" && !String(e.message).includes("PERMISSION_DENIED")) throw e;
      refused.push({ ...doc, reason: hint(doc.collection, doc.data) });
    }
  }
  return refused;
}

/** Every money document of the live households, read with the Admin SDK. */
async function readLive(householdId) {
  const { initializeApp, applicationDefault } = require("firebase-admin/app");
  const { getFirestore } = require("firebase-admin/firestore");
  // `firebase emulators:exec` sets GCLOUD_PROJECT to the emulator's demo project; read the real one from the key.
  const { project_id: projectId } = JSON.parse(fs.readFileSync(process.env.GOOGLE_APPLICATION_CREDENTIALS, "utf8"));
  if (!projectId) throw new Error("The service account JSON has no project_id.");
  const app = initializeApp({ credential: applicationDefault(), projectId }, "check-money-docs");
  const db = getFirestore(app, DATABASE);
  const households = householdId ? [householdId] : (await db.collection("households").listDocuments()).map((d) => d.id);
  const docs = [];
  for (const hid of households) {
    for (const collection of MONEY_COLLECTIONS) {
      const snap = await db.collection("households").doc(hid).collection(collection).get();
      snap.forEach((d) => docs.push({ household: hid, collection, id: d.id, data: d.data() }));
    }
  }
  return { households, docs };
}

async function main() {
  const args = process.argv.slice(2);
  const i = args.indexOf("--household");
  const householdId = i >= 0 ? args[i + 1] : null;

  // The emulator address is for the replay only; the Admin SDK must read the real database.
  const emulator = process.env.FIRESTORE_EMULATOR_HOST;
  if (!emulator) throw new Error("Run inside `firebase emulators:exec --only firestore` (FIRESTORE_EMULATOR_HOST is not set).");
  if (!process.env.GOOGLE_APPLICATION_CREDENTIALS) throw new Error("Set GOOGLE_APPLICATION_CREDENTIALS to a service account JSON.");
  delete process.env.FIRESTORE_EMULATOR_HOST;
  delete process.env.GCLOUD_PROJECT;
  delete process.env.GOOGLE_CLOUD_PROJECT;
  const { households, docs } = await readLive(householdId);

  const { initializeTestEnvironment } = require("@firebase/rules-unit-testing");
  const [host, port] = emulator.split(":");
  const testEnv = await initializeTestEnvironment({
    projectId: "demo-no-project",
    firestore: { rules: fs.readFileSync(path.join(ROOT, "firestore.rules"), "utf8"), host, port: Number(port) },
  });
  try {
    const refused = await checkDocs(testEnv, docs);
    console.log(`Checked ${docs.length} money documents in ${households.length} household(s) of database ${DATABASE}.`);
    refused.forEach((r) => console.log(`FAIL households/${r.household}/${r.collection}/${r.id}: ${r.reason}`));
    console.log(refused.length === 0 ? "0 failures: safe to deploy these rules." : `${refused.length} failure(s): do NOT merge yet.`);
    process.exitCode = refused.length === 0 ? 0 : 1;
  } finally {
    await testEnv.cleanup();
  }
}

module.exports = { checkDocs, hint, MONEY_COLLECTIONS };

if (require.main === module) {
  main().catch((e) => {
    console.error(`Could not run the check: ${e.message}`);
    process.exitCode = 2;
  });
}
