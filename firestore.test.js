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
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_789";

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

test("Unauthenticated user cannot read or list posts or users", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(unauthDb.collection("posts").where("visibility", "==", "public").get());
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
});

test("User profile isolation and shadow update rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const now = new Date();

  const validProfile = {
    userId: ALICE_UID,
    username: "alice_star",
    displayName: "Alice Star",
    bio: "Neon creator",
    avatarUrl: "",
    vipLevel: 1,
    vipXp: 0,
    diamondBalance: 100,
    followersCount: 0,
    followingCount: 0,
    eligibleVideoViews: 0,
    eligibleReelViews: 0,
    eligiblePhotoViews: 0,
    invalidViewsCount: 0,
    pendingEarningsPaise: 0,
    approvedEarningsPaise: 0,
    paidEarningsPaise: 0,
    accountStatus: "active",
    statusReason: "",
    createdAt: now,
    updatedAt: now,
  };

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validProfile));
  // Cross-user read rejected
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  // Shadow update with ghost field rejected
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: new Date(),
    })
  );
});

test("Email spoofing test: unverified admin email is rejected", async () => {
  await testEnv.withSecurityRulesDisabled(async (context) => {
    const now = new Date();
    await context.firestore().collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "alice_star",
      displayName: "Alice Star",
      bio: "",
      avatarUrl: "",
      vipLevel: 1,
      vipXp: 0,
      diamondBalance: 0,
      followersCount: 0,
      followingCount: 0,
      eligibleVideoViews: 0,
      eligibleReelViews: 0,
      eligiblePhotoViews: 0,
      invalidViewsCount: 0,
      pendingEarningsPaise: 0,
      approvedEarningsPaise: 0,
      paidEarningsPaise: 0,
      accountStatus: "active",
      statusReason: "",
      createdAt: now,
      updatedAt: now,
    });
  });

  const spoofedAdminDb = testEnv
    .authenticatedContext("spoof_uid", {
      email: "abdulbarik80000@gmail.com",
      email_verified: false,
    })
    .firestore();

  await assertFails(spoofedAdminDb.collection("users").doc(ALICE_UID).get());

  const verifiedAdminDb = testEnv
    .authenticatedContext(ADMIN_UID, {
      email: "abdulbarik80000@gmail.com",
      email_verified: true,
    })
    .firestore();

  await assertSucceeds(verifiedAdminDb.collection("users").doc(ALICE_UID).get());
});

test("Diamond purchase cannot be marked verified by client", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = new Date();

  await assertFails(
    aliceDb.collection("wallet_transactions").doc("tx_spoof").set({
      userId: ALICE_UID,
      txType: "diamond_purchase",
      packageId: "pkg_500",
      diamondsDelta: 500,
      amountInrPaise: 49900,
      recipientId: "",
      streamId: "",
      verificationStatus: "verified",
      paymentRef: "pay_123",
      createdAt: now,
    })
  );

  await assertSucceeds(
    aliceDb.collection("wallet_transactions").doc("tx_valid").set({
      userId: ALICE_UID,
      txType: "diamond_purchase",
      packageId: "pkg_500",
      diamondsDelta: 500,
      amountInrPaise: 49900,
      recipientId: "",
      streamId: "",
      verificationStatus: "pending_backend_verification",
      paymentRef: "pay_123",
      createdAt: now,
    })
  );
});

test("Withdrawal requires Level 30, minimum Rs 10,000, and verified bank account", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("bank_accounts").doc("bank_alice").set({
      userId: ALICE_UID,
      accountHolderName: "Alice Star",
      bankName: "State Bank",
      accountNumberMasked: "XXXX1234",
      ifscCode: "SBIN0001234",
      verificationStatus: "verified",
      createdAt: now,
      updatedAt: now,
    });
  });

  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Fails if VIP level < 30
  await assertFails(
    aliceDb.collection("withdrawals").doc("wd_low_level").set({
      userId: ALICE_UID,
      bankAccountId: "bank_alice",
      amountInrPaise: 1000000,
      userVipLevelAtRequest: 25,
      status: "pending_review",
      scheduledPayoutDay: 18,
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );

  // Succeeds when VIP level >= 30 and amount >= 1,000,000 paise (Rs 10,000)
  await assertSucceeds(
    aliceDb.collection("withdrawals").doc("wd_valid").set({
      userId: ALICE_UID,
      bankAccountId: "bank_alice",
      amountInrPaise: 1000000,
      userVipLevelAtRequest: 30,
      status: "pending_review",
      scheduledPayoutDay: 18,
      adminNote: "",
      createdAt: now,
      updatedAt: now,
    })
  );
});

test("Suspended or banned user cannot create posts on backend", async () => {
  const now = new Date();
  await testEnv.withSecurityRulesDisabled(async (context) => {
    await context.firestore().collection("users").doc(BOB_UID).set({
      userId: BOB_UID,
      username: "bob_banned",
      displayName: "Bob",
      bio: "",
      avatarUrl: "",
      vipLevel: 1,
      vipXp: 0,
      diamondBalance: 0,
      followersCount: 0,
      followingCount: 0,
      eligibleVideoViews: 0,
      eligibleReelViews: 0,
      eligiblePhotoViews: 0,
      invalidViewsCount: 0,
      pendingEarningsPaise: 0,
      approvedEarningsPaise: 0,
      paidEarningsPaise: 0,
      accountStatus: "banned",
      statusReason: "Violated community guidelines",
      createdAt: now,
      updatedAt: now,
    });
  });

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(
    bobDb.collection("posts").doc("post_banned").set({
      authorId: BOB_UID,
      authorName: "Bob",
      postType: "reel",
      title: "Blocked Reel",
      description: "Should fail",
      hashtags: "#blocked",
      mediaCount: 1,
      mediaUri: "reel://1",
      thumbnailTitle: "Blocked",
      durationSeconds: 15,
      viewsCount: 0,
      likesCount: 0,
      commentsCount: 0,
      sharesCount: 0,
      visibility: "public",
      createdAt: now,
      updatedAt: now,
    })
  );
});
