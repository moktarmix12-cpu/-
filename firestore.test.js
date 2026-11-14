const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");
const crypto = require("node:crypto");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function sha256(str) {
  return crypto.createHash("sha256").update(str).digest("hex");
}

function pastTimestamp() {
  return new Date(Date.now() - 2000);
}

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
  if (testEnv) await testEnv.cleanup();
});

beforeEach(async () => {
  if (testEnv) await testEnv.clearFirestore();
});

test("Unauthenticated user: cannot read users, paymentRequests, activationCodes, or chapters", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("paymentRequests").get());
  await assertFails(unauthDb.collection("activationCodes").get());
  await assertFails(unauthDb.collection("chapters").get());
  await assertFails(unauthDb.collection("lessonContents").doc("1").get());
});

test("User profile: can create own unactivated profile, cannot self-activate directly", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastTimestamp();

  // Attempting to create already activated profile MUST FAIL
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      childName: "أحمد",
      email: "alice@example.com",
      isActivated: true,
      revoked: false,
      activationMethod: "none",
      stars: 0,
      totalScore: 0,
      completedChapters: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Creating unactivated profile succeeds
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      childName: "أحمد",
      email: "alice@example.com",
      isActivated: false,
      revoked: false,
      activationMethod: "none",
      stars: 0,
      totalScore: 0,
      completedChapters: [],
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Attempting to self-activate without valid code MUST FAIL
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isActivated: true,
      activationMethod: "primary_code",
      redeemedCodeHash: sha256("wrong_code"),
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's profile
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
});

test("Lesson contents: Chapter 1 is free; Chapter 2 is blocked until activated", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      childName: "فاطمة",
      isActivated: false,
      revoked: false,
      activationMethod: "none",
      stars: 0,
      totalScore: 0,
      completedChapters: [],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("lessonContents").doc("1").set({
      chapterNumber: 1,
      title: "مولد النبي ﷺ ونسبه",
      illustration: "kaaba",
      isFree: true,
      content: "نص الفصل الأول الموثق للأطفال.",
      sourceNote: "الرحيق المختوم",
      questions: [{ question: "أين ولد؟", options: ["مكة", "المدينة"], correctIndex: 0 }],
      updatedAt: ts,
    });
    await db.collection("lessonContents").doc("2").set({
      chapterNumber: 2,
      title: "طفولته ونشأته",
      illustration: "desert",
      isFree: false,
      content: "نص الفصل الثاني المدفوع للأطفال.",
      sourceNote: "صحيح السيرة",
      questions: [{ question: "من أرضعته؟", options: ["حليمة السعدية", "أم سلمة"], correctIndex: 0 }],
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(aliceDb.collection("lessonContents").doc("1").get());
  await assertFails(aliceDb.collection("lessonContents").doc("2").get());

  // Activate with primary code hash (default xs509)
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isActivated: true,
      activationMethod: "primary_code",
      redeemedCodeHash: sha256("xs509"),
      updatedAt: ts,
    })
  );

  // Now Chapter 2 succeeds!
  await assertSucceeds(aliceDb.collection("lessonContents").doc("2").get());
});

test("Unique activation code: atomic batch redemption works once and blocks second user", async () => {
  const ts = pastTimestamp();
  const codeHash = sha256("SR-998877");
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      childName: "عمر",
      isActivated: false,
      revoked: false,
      activationMethod: "none",
      stars: 0,
      totalScore: 0,
      completedChapters: [],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      childName: "سارة",
      isActivated: false,
      revoked: false,
      activationMethod: "none",
      stars: 0,
      totalScore: 0,
      completedChapters: [],
      createdAt: ts,
      updatedAt: ts,
    });
    await db.collection("activationCodes").doc(codeHash).set({
      codeDisplay: "SR-998877",
      isActive: true,
      maxUses: 1,
      usedCount: 0,
      boundUserId: "",
      note: "For Omar",
      createdAt: ts,
      updatedAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  // Buyer cannot read or list activationCodes directly
  await assertFails(aliceDb.collection("activationCodes").doc(codeHash).get());
  await assertFails(aliceDb.collection("activationCodes").get());

  // Alice redeems atomically
  const batch1 = aliceDb.batch();
  batch1.update(aliceDb.collection("users").doc(ALICE_UID), {
    isActivated: true,
    activationMethod: "unique_code",
    redeemedCodeHash: codeHash,
    updatedAt: ts,
  });
  batch1.update(aliceDb.collection("activationCodes").doc(codeHash), {
    usedCount: 1,
    boundUserId: ALICE_UID,
    updatedAt: ts,
  });
  await assertSucceeds(batch1.commit());

  // Bob cannot reuse the same single-use code
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const batch2 = bobDb.batch();
  batch2.update(bobDb.collection("users").doc(BOB_UID), {
    isActivated: true,
    activationMethod: "unique_code",
    redeemedCodeHash: codeHash,
    updatedAt: ts,
  });
  batch2.update(bobDb.collection("activationCodes").doc(codeHash), {
    usedCount: 2,
    boundUserId: BOB_UID,
    updatedAt: ts,
  });
  await assertFails(batch2.commit());
});

test("Payment requests: submitting receipt sets PENDING, cannot self-approve, only admin approves", async () => {
  const ts = pastTimestamp();
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("admins").doc(ADMIN_UID).set({
      email: "moktar.mix12@gmail.com",
      createdAt: ts,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  await assertSucceeds(
    aliceDb.collection("paymentRequests").doc("req_1").set({
      userId: ALICE_UID,
      childName: "أحمد",
      userEmail: "alice@example.com",
      method: "Bankily",
      senderPhone: "22334455",
      receiverPhone: "49719739",
      amountMru: 200,
      transactionNote: "Ref 123",
      receiptBase64: "iVBORw0KGgoAAAANSUhEUg==",
      status: "PENDING",
      adminNote: "",
      issuedCode: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invalid payment method rejected
  await assertFails(
    aliceDb.collection("paymentRequests").doc("req_bad").set({
      userId: ALICE_UID,
      childName: "أحمد",
      userEmail: "alice@example.com",
      method: "PayPal",
      senderPhone: "22334455",
      receiverPhone: "49719739",
      amountMru: 200,
      transactionNote: "",
      receiptBase64: "iVBORw0KGgoAAAANSUhEUg==",
      status: "PENDING",
      adminNote: "",
      issuedCode: "",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Alice cannot approve her own request
  await assertFails(
    aliceDb.collection("paymentRequests").doc("req_1").update({
      status: "APPROVED",
      issuedCode: "xs509",
      updatedAt: ts,
    })
  );

  // Admin can list and approve
  const adminDb = testEnv.authenticatedContext(ADMIN_UID).firestore();
  await assertSucceeds(adminDb.collection("paymentRequests").get());
  await assertSucceeds(
    adminDb.collection("paymentRequests").doc("req_1").update({
      status: "APPROVED",
      adminNote: "تم تأكيد وصول 200 أوقية",
      issuedCode: "SR-1234",
      updatedAt: ts,
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  const spoofDb = testEnv.authenticatedContext("spoof_uid", {
    email: "moktar.mix12@gmail.com",
    email_verified: false,
  }).firestore();
  await assertFails(spoofDb.collection("activationCodes").get());

  const realAdminDb = testEnv.authenticatedContext("real_admin_uid", {
    email: "moktar.mix12@gmail.com",
    email_verified: true,
  }).firestore();
  await assertSucceeds(realAdminDb.collection("activationCodes").get());
});
