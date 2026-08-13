#!/usr/bin/env node

const coreApiBaseUrl = process.env.CORE_API_BASE_URL ?? process.env.WISHPOOL_CORE_API_BASE_URL ?? "http://localhost:8080";
const internalToken = process.env.WISHPOOL_INTERNAL_TOKEN ?? "wishpool-local-internal-token";
const phoneNumber = process.env.WISHPOOL_SEED_PHONE ?? "18800000001";
const familyName = process.env.WISHPOOL_SEED_FAMILY_NAME ?? "星愿小屋";
const childName = process.env.WISHPOOL_SEED_CHILD_NAME ?? "小满";
const timezone = process.env.WISHPOOL_SEED_TIMEZONE ?? "Asia/Shanghai";

const taskInputs = [
  {
    title: "亲子阅读 20 分钟",
    category: "reading",
    submissionType: "photo",
    description: "一起读一本绘本，并记录阅读时刻。",
    targetText: "拍下今天阅读的绘本或阅读角。",
    defaultDurationSec: 1200,
    weekdays: [1, 2, 3, 4, 5, 6, 7],
    isCore: true,
    requireReview: true
  },
  {
    title: "运动挑战 15 分钟",
    category: "exercise",
    submissionType: "video",
    description: "完成一段轻量运动，让身体也参与成长。",
    targetText: "上传一段运动片段。",
    defaultDurationSec: 900,
    weekdays: [1, 3, 5, 7],
    isCore: true,
    requireReview: true
  },
  {
    title: "记录一件开心的小事",
    category: "habit",
    submissionType: "audio",
    description: "用自己的声音说一件今天开心的事。",
    targetText: "录一段 20 秒以上语音。",
    defaultDurationSec: 120,
    weekdays: [2, 4, 6, 7],
    isCore: false,
    requireReview: true
  }
];

async function main() {
  await waitForCoreApi();
  const auth = await loginWithDebugPhoneCode();
  const accessToken = auth.accessToken;
  const context = await ensureFamilyContext(accessToken, auth.primaryFamilyId);
  const week = currentIsoWeek();
  const templates = [];
  for (const input of taskInputs) {
    templates.push(await ensureTaskTemplate(accessToken, context.family.id, input));
  }
  const wish = await ensureWish(accessToken, context, week);
  const plan = await saveWeeklyPlan(accessToken, context, week, wish.id, templates);
  await materializeWeeklyPlan(plan.id);
  const today = await getJson(`/children/${context.child.id}/today`, accessToken);
  const pairing = await postJson(
    `/families/${context.family.id}/pairing-sessions`,
    { childId: context.child.id },
    { accessToken }
  );

  console.log(JSON.stringify({
    coreApiBaseUrl,
    parentWebUrl: "http://localhost:3000",
    adminWebUrl: "http://localhost:3001",
    adminToken: process.env.WISHPOOL_ADMIN_TOKEN ?? "wishpool-local-admin-token",
    seedPhone: phoneNumber,
    familyId: context.family.id,
    childId: context.child.id,
    accessToken,
    refreshToken: auth.refreshToken,
    pairingCode: pairing.pairingCode,
    pairingExpiresAt: pairing.expiresAt,
    weeklyPlanId: plan.id,
    todayTaskCount: Array.isArray(today.tasks) ? today.tasks.length : 0
  }, null, 2));
}

async function waitForCoreApi() {
  const deadline = Date.now() + Number(process.env.WISHPOOL_SEED_WAIT_MS ?? "120000");
  while (Date.now() < deadline) {
    try {
      const response = await fetch(`${coreApiBaseUrl}/actuator/health`, { cache: "no-store" });
      if (response.ok) return;
    } catch {
      await delay(1500);
    }
  }
  throw new Error(`Core API did not become healthy at ${coreApiBaseUrl}.`);
}

async function loginWithDebugPhoneCode() {
  const codeResponse = await postJson("/auth/phone-codes", { phoneNumber, purpose: "login" });
  if (!codeResponse.debugCode) {
    throw new Error("Local seed requires WISHPOOL_LOCAL_DEBUG_PHONE_CODE=true on Core API.");
  }
  return postJson("/auth/login", {
    provider: "phone",
    credential: `${codeResponse.verificationToken}:${codeResponse.debugCode}`,
    device: {
      platform: "web",
      deviceName: "WishPool Local Seed"
    }
  });
}

async function ensureFamilyContext(accessToken, primaryFamilyId) {
  const me = await getJson("/me", accessToken);
  const existing = Array.isArray(me.families)
    ? me.families.find((context) => context.family?.id === primaryFamilyId) ?? me.families[0]
    : null;
  const family = existing?.family ?? await postJson("/families", {
    name: familyName,
    timezone
  }, {
    accessToken
  });
  const children = await getJson(`/families/${family.id}/children`, accessToken);
  const child = Array.isArray(children) && children.length > 0
    ? children[0]
    : await postJson(`/families/${family.id}/children`, {
      nickname: childName,
      birthYear: 2018,
      roomTheme: "forest"
    }, {
      accessToken
    });
  return { family, child };
}

async function ensureTaskTemplate(accessToken, familyId, input) {
  const templates = await getJson(`/task-templates?familyId=${encodeURIComponent(familyId)}`, accessToken);
  const existing = Array.isArray(templates)
    ? templates.find((template) => template.title === input.title && template.submissionType === input.submissionType)
    : null;
  if (existing) return existing;
  return postJson("/task-templates", {
    familyId,
    title: input.title,
    category: input.category,
    submissionType: input.submissionType,
    description: input.description,
    targetText: input.targetText,
    defaultDurationSec: input.defaultDurationSec
  }, {
    accessToken,
    idempotencyKey: `local-seed-template-${slug(input.title)}`
  });
}

async function ensureWish(accessToken, context, week) {
  const wishes = await getJson(`/children/${context.child.id}/wishes?weekId=${encodeURIComponent(week.weekId)}`, accessToken);
  const active = Array.isArray(wishes) ? wishes.find((wish) => ["active", "unlocked", "redeemed"].includes(wish.status)) : null;
  if (active) return active;
  const created = await postJson("/wishes", {
    familyId: context.family.id,
    childId: context.child.id,
    weekId: week.weekId,
    title: "周末搭建城市积木",
    note: "完成本周计划后一起搭一座城市。",
    requiredFragments: 10,
    rewardMode: "flexible"
  }, {
    accessToken,
    idempotencyKey: `local-seed-wish-${week.weekId}`
  });
  return postJson(`/wishes/${created.id}/activate`, {}, {
    accessToken,
    idempotencyKey: `local-seed-wish-activate-${week.weekId}`
  });
}

async function saveWeeklyPlan(accessToken, context, week, wishId, templates) {
  const rules = taskInputs.map((input, index) => {
    const template = templates[index];
    return {
      taskTemplateId: template.id,
      title: input.title,
      category: input.category,
      submissionType: input.submissionType,
      description: input.description,
      targetText: input.targetText,
      weekdays: input.weekdays,
      isCore: input.isCore,
      requireReview: input.requireReview,
      sortOrder: index
    };
  });
  return postJson("/plans", {
    familyId: context.family.id,
    childId: context.child.id,
    weekId: week.weekId,
    startDate: week.startDate,
    endDate: week.endDate,
    wishId,
    rewardMode: "flexible",
    rules
  }, {
    accessToken,
    idempotencyKey: `local-seed-plan-${week.weekId}`
  });
}

async function materializeWeeklyPlan(weeklyPlanId) {
  await postJson("/internal/workflows/materialize-weekly-plan", {
    weeklyPlanId
  }, {
    internalToken
  });
}

async function getJson(path, accessToken) {
  const response = await fetch(`${coreApiBaseUrl}${path}`, {
    cache: "no-store",
    headers: accessToken ? { Authorization: `Bearer ${accessToken}` } : undefined
  });
  return parseResponse(response);
}

async function postJson(path, body, options = {}) {
  const headers = {
    "Content-Type": "application/json"
  };
  if (options.accessToken) headers.Authorization = `Bearer ${options.accessToken}`;
  if (options.internalToken) headers["X-Internal-Token"] = options.internalToken;
  if (options.idempotencyKey) headers["Idempotency-Key"] = options.idempotencyKey;
  const response = await fetch(`${coreApiBaseUrl}${path}`, {
    method: "POST",
    cache: "no-store",
    headers,
    body: JSON.stringify(body)
  });
  return parseResponse(response);
}

async function parseResponse(response) {
  const text = await response.text();
  if (!response.ok) {
    throw new Error(`${response.status} ${response.statusText}: ${text}`);
  }
  return text ? JSON.parse(text) : {};
}

function currentIsoWeek() {
  const now = new Date();
  const day = now.getUTCDay() || 7;
  const monday = new Date(Date.UTC(now.getUTCFullYear(), now.getUTCMonth(), now.getUTCDate()));
  monday.setUTCDate(monday.getUTCDate() - day + 1);
  const sunday = new Date(monday);
  sunday.setUTCDate(monday.getUTCDate() + 6);
  const thursday = new Date(monday);
  thursday.setUTCDate(monday.getUTCDate() + 3);
  const yearStart = new Date(Date.UTC(thursday.getUTCFullYear(), 0, 1));
  const weekNumber = Math.ceil((((thursday - yearStart) / 86400000) + 1) / 7);
  return {
    weekId: `${thursday.getUTCFullYear()}-W${String(weekNumber).padStart(2, "0")}`,
    startDate: monday.toISOString().slice(0, 10),
    endDate: sunday.toISOString().slice(0, 10)
  };
}

function slug(value) {
  return Buffer.from(value).toString("base64url").slice(0, 24);
}

function delay(ms) {
  return new Promise((resolve) => setTimeout(resolve, ms));
}

main().catch((error) => {
  console.error(error.message);
  process.exit(1);
});
