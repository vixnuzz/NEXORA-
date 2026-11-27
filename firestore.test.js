const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_nexora_1";
const BOB_UID = "bob_nexora_2";
const BIZ_ID = "biz_acme_1";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function pastDate(msOffset = 2000) {
  return new Date(Date.now() - msOffset);
}

async function seedBusinessForAlice() {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const now = pastDate(5000);
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).set({
      ownerId: ALICE_UID,
      name: "Acme Cloud Corp",
      industry: "Enterprise SaaS",
      currency: "USD",
      taxRate: 8.5,
      address: "100 Market St, SF",
      email: "ops@acme.io",
      activeRole: "BUSINESS_OWNER",
      demoSeeded: false,
      createdAt: now,
      updatedAt: now,
    })
  );
  return aliceDb;
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
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

test("Unauthenticated user cannot read or write user profiles, businesses, or modules", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = pastDate();
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Intruder",
      email: "bad@example.com",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("businesses").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).collection("invoices").get()
  );
});

test("Cross-user isolation blocks Bob from reading or modifying Alice's business and data", async () => {
  const aliceDb = await seedBusinessForAlice();
  const ts = pastDate();
  await assertSucceeds(
    aliceDb
      .collection("users").doc(ALICE_UID)
      .collection("businesses").doc(BIZ_ID)
      .collection("crm_leads").doc("lead_1")
      .set({
        userId: ALICE_UID,
        businessId: BIZ_ID,
        name: "Elena Vance",
        company: "Orbital Tech",
        stage: "QUALIFIED",
        dealValue: 45000,
        createdAt: ts,
        updatedAt: ts,
      })
  );

  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("businesses").get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).get());
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).collection("crm_leads").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).collection("crm_leads").doc("lead_1").update({
      stage: "WON",
      updatedAt: pastDate(500),
    })
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).collection("crm_leads").doc("lead_1").delete()
  );
});

test("Owner can create and partially update UserProfile & Business while blocking ghost/immutable fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(5000);

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      displayName: "Alice Mercer",
      email: "alice@nexora.io",
      jobTitle: "Founder & CEO",
      activeBusinessId: BIZ_ID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).update({
      jobTitle: "Managing Partner",
      updatedAt: pastDate(500),
    })
  );

  // Reject ghost field on UserProfile
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      isAdmin: true,
      updatedAt: pastDate(400),
    })
  );

  // Reject immutable createdAt mutation
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).update({
      createdAt: pastDate(100),
      updatedAt: pastDate(100),
    })
  );

  await seedBusinessForAlice();
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).update({
      activeRole: "ACCOUNTANT",
      updatedAt: pastDate(300),
    })
  );

  // Reject invalid role enum
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID).update({
      activeRole: "HACKER",
      updatedAt: pastDate(200),
    })
  );
});

test("Orphaned module writes fail if parent Business does not exist, and valid module CRUD succeeds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const ts = pastDate(4000);

  // Orphan write before business exists must fail
  await assertFails(
    aliceDb
      .collection("users").doc(ALICE_UID)
      .collection("businesses").doc("nonexistent_biz")
      .collection("finance_records").doc("fin_1")
      .set({
        userId: ALICE_UID,
        businessId: "nonexistent_biz",
        title: "Orphan",
        type: "INCOME",
        category: "Sales",
        amount: 100,
        dateStr: "2026-11-26",
        createdAt: ts,
        updatedAt: ts,
      })
  );

  // Tampered future timestamp must fail
  await seedBusinessForAlice();
  await assertFails(
    aliceDb
      .collection("users").doc(ALICE_UID)
      .collection("businesses").doc(BIZ_ID)
      .collection("finance_records").doc("fin_future")
      .set({
        userId: ALICE_UID,
        businessId: BIZ_ID,
        title: "Future Entry",
        type: "INCOME",
        category: "Sales",
        amount: 500,
        dateStr: "2026-11-26",
        createdAt: new Date(Date.now() + 60000),
        updatedAt: new Date(Date.now() + 60000),
      })
  );

  // Valid writes across all connected enterprise modules
  const bizRef = aliceDb.collection("users").doc(ALICE_UID).collection("businesses").doc(BIZ_ID);

  await assertSucceeds(
    bizRef.collection("crm_contacts").doc("contact_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      name: "Marcus Chen",
      company: "Vertex Logistics",
      status: "ACTIVE",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("finance_records").doc("fin_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      title: "Enterprise Retainer",
      type: "INCOME",
      category: "Subscriptions",
      amount: 12500.5,
      dateStr: "2026-11-26",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("invoices").doc("inv_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      invoiceNumber: "INV-2026-001",
      customerName: "Vertex Logistics",
      docType: "INVOICE",
      amount: 5400,
      taxAmount: 459,
      status: "PENDING",
      dueDate: "2026-12-15",
      createdAt: ts,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    bizRef.collection("invoices").doc("inv_1").update({
      status: "PAID",
      updatedAt: pastDate(300),
    })
  );

  await assertSucceeds(
    bizRef.collection("employees").doc("emp_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      fullName: "Priya Nair",
      email: "priya@acme.io",
      department: "Engineering",
      jobTitle: "Staff Architect",
      role: "MANAGER",
      salary: 145000,
      attendanceStatus: "PRESENT",
      leaveBalanceDays: 14,
      leaveRequestStatus: "NONE",
      performanceScore: 94,
      payrollStatus: "PAID",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("projects").doc("proj_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      name: "Q4 Platform Rollout",
      status: "ACTIVE",
      deadline: "2026-12-30",
      progressPercent: 65,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("project_tasks").doc("task_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      projectId: "proj_1",
      projectName: "Q4 Platform Rollout",
      title: "Configure SSO",
      priority: "HIGH",
      kanbanColumn: "IN_PROGRESS",
      dueDate: "2026-12-05",
      reminderActive: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("inventory_products").doc("prod_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      sku: "NX-HUB-01",
      name: "Edge Gateway Pro",
      category: "Hardware",
      supplierName: "Apex Silicon",
      unitPrice: 499,
      stockQty: 6,
      reorderLevel: 10,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("purchase_orders").doc("po_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      poNumber: "PO-1042",
      supplierName: "Apex Silicon",
      productId: "prod_1",
      productName: "Edge Gateway Pro",
      quantity: 25,
      totalCost: 8750,
      status: "ORDERED",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("sales_orders").doc("ord_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      orderNumber: "SO-8810",
      customerName: "Vertex Logistics",
      productName: "Edge Gateway Pro",
      quantity: 4,
      subtotal: 1996,
      discountPercent: 10,
      totalAmount: 1796.4,
      monthLabel: "2026-11",
      status: "PROCESSING",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("marketing_campaigns").doc("camp_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      name: "Black Friday Enterprise",
      channel: "EMAIL",
      status: "ACTIVE",
      budget: 2500,
      impressions: 18400,
      clicks: 2410,
      conversions: 142,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("support_tickets").doc("tkt_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      ticketNumber: "TKT-309",
      customerName: "Vertex Logistics",
      subject: "API Webhook Timeout",
      priority: "HIGH",
      status: "OPEN",
      assignedAgent: "Priya Nair",
      createdAt: ts,
      updatedAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("activity_logs").doc("log_1").set({
      userId: ALICE_UID,
      businessId: BIZ_ID,
      actorName: "Alice Mercer",
      actorRole: "BUSINESS_OWNER",
      module: "FINANCE",
      action: "CREATE",
      summary: "Created invoice INV-2026-001",
      unread: true,
      createdAt: ts,
    })
  );

  await assertSucceeds(
    bizRef.collection("activity_logs").doc("log_1").update({
      unread: false,
    })
  );

  // Verify @DocumentId `id` field is rejected if mistakenly sent inside document payload
  await assertFails(
    bizRef.collection("projects").doc("proj_bad").set({
      id: "proj_bad",
      userId: ALICE_UID,
      businessId: BIZ_ID,
      name: "Bad ID Field",
      status: "PLANNING",
      deadline: "2026-12-30",
      progressPercent: 0,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verify unfiltered list on user-scoped subcollections succeeds for owner
  await assertSucceeds(bizRef.collection("crm_leads").get());
  await assertSucceeds(bizRef.collection("invoices").get());
  await assertSucceeds(bizRef.collection("activity_logs").get());
});
