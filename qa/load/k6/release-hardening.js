import http from "k6/http";
import { check, sleep } from "k6";
import { crypto } from "k6/experimental/webcrypto";

const BASE_URL = __ENV.K6_BASE_URL || "http://localhost:8080";

const CHEF_LOGIN = __ENV.K6_CHEF_LOGIN || __ENV.K6_LOGIN || "";
const CHEF_PASSWORD = __ENV.K6_CHEF_PASSWORD || __ENV.K6_PASSWORD || "";
const STUDENT_LOGIN = __ENV.K6_STUDENT_LOGIN || "";
const STUDENT_PASSWORD = __ENV.K6_STUDENT_PASSWORD || "";

const QR_MEAL_TYPE = __ENV.K6_QR_MEAL_TYPE || "LUNCH";
const SYNC_MEAL_TYPE = __ENV.K6_SYNC_MEAL_TYPE || "LUNCH";
const QR_NONCE_PREFIX = __ENV.K6_QR_NONCE_PREFIX || "nonce-release-hardening";
const DISABLE_DYNAMIC_SIGN = (__ENV.K6_DISABLE_DYNAMIC_SIGN || "false").toLowerCase() === "true";
const FALLBACK_QR_SIGNATURE = __ENV.K6_QR_SIGNATURE || "";

const SYNC_STUDENT_ID_ENV = __ENV.K6_SYNC_STUDENT_ID || "";
const QR_USER_ID_ENV = __ENV.K6_QR_USER_ID || "";

const STAGES = [
  { duration: "2m", target: 100 },
  { duration: "4m", target: 300 },
  { duration: "6m", target: 500 },
  { duration: "2m", target: 0 },
];

const ACCEPTABLE_QR_ERROR_CODES = new Set([
  "ALREADY_ATE",
  "ALREADY_USED",
  "NO_PERMISSION",
  "QR_EXPIRED",
]);

let importedPrivateKey = null;

export const options = {
  scenarios: {
    qr_validate: {
      executor: "ramping-vus",
      exec: "runQrValidate",
      stages: STAGES,
      gracefulRampDown: "30s",
    },
    sync_batch: {
      executor: "ramping-vus",
      exec: "runSyncBatch",
      stages: STAGES,
      gracefulRampDown: "30s",
    },
  },
  thresholds: {
    "http_req_duration{endpoint:qr_validate}": ["p(95)<500"],
    "http_req_duration{endpoint:sync_batch}": ["p(95)<900"],
    "http_req_failed{endpoint:qr_validate}": ["rate<0.01"],
    "http_req_failed{endpoint:sync_batch}": ["rate<0.01"],
  },
};

export function setup() {
  if (!CHEF_LOGIN || !CHEF_PASSWORD) {
    throw new Error("Set K6_CHEF_LOGIN and K6_CHEF_PASSWORD.");
  }

  const chefAuth = login(CHEF_LOGIN, CHEF_PASSWORD);
  check(chefAuth.res, {
    "chef login status is 200": (r) => r.status === 200,
    "chef token exists": () => !!(chefAuth.body && chefAuth.body.token),
  });

  const setupData = {
    chefToken: (chefAuth.body && chefAuth.body.token) || "",
    qrUserId: QR_USER_ID_ENV || "",
    syncStudentId: SYNC_STUDENT_ID_ENV || "",
    studentPrivateKey: "",
    dynamicSignEnabled: !DISABLE_DYNAMIC_SIGN,
  };

  if (!DISABLE_DYNAMIC_SIGN) {
    if (!STUDENT_LOGIN || !STUDENT_PASSWORD) {
      throw new Error("Set K6_STUDENT_LOGIN and K6_STUDENT_PASSWORD (or disable dynamic sign).");
    }
    const studentAuth = login(STUDENT_LOGIN, STUDENT_PASSWORD);
    check(studentAuth.res, {
      "student login status is 200": (r) => r.status === 200,
      "student token exists": () => !!(studentAuth.body && studentAuth.body.token),
      "student private key exists": () => !!(studentAuth.body && studentAuth.body.privateKey),
      "student user id exists": () => !!(studentAuth.body && studentAuth.body.userId),
    });
    setupData.qrUserId = setupData.qrUserId || (studentAuth.body && studentAuth.body.userId) || "";
    setupData.syncStudentId = setupData.syncStudentId || (studentAuth.body && studentAuth.body.userId) || "";
    setupData.studentPrivateKey = (studentAuth.body && studentAuth.body.privateKey) || "";
  } else {
    if (!FALLBACK_QR_SIGNATURE) {
      throw new Error("Set K6_QR_SIGNATURE when K6_DISABLE_DYNAMIC_SIGN=true.");
    }
    if (!setupData.qrUserId || !setupData.syncStudentId) {
      throw new Error("Set K6_QR_USER_ID and K6_SYNC_STUDENT_ID when dynamic signing is disabled.");
    }
  }

  if (!setupData.chefToken) throw new Error("Chef token is empty after setup.");
  if (!setupData.qrUserId) throw new Error("QR user id is empty after setup.");
  if (!setupData.syncStudentId) throw new Error("Sync student id is empty after setup.");
  return setupData;
}

export async function runQrValidate(data) {
  const roundedTs = roundTimestampToMinute(Math.floor(Date.now() / 1000));
  const nonce = `${QR_NONCE_PREFIX}-${__VU}-${__ITER}-${roundedTs}`;
  const signature = data.dynamicSignEnabled
    ? await signPayloadBase64(data.qrUserId, roundedTs, QR_MEAL_TYPE, nonce, data.studentPrivateKey)
    : FALLBACK_QR_SIGNATURE;

  const payload = JSON.stringify({
    userId: data.qrUserId,
    timestamp: roundedTs,
    mealType: QR_MEAL_TYPE,
    nonce: nonce,
    signature: signature,
  });

  const res = http.post(`${BASE_URL}/api/v1/qr/validate`, payload, {
    headers: authHeaders(data.chefToken),
    tags: { endpoint: "qr_validate" },
  });
  const body = parseJson(res.body);

  check(res, {
    "qr validate returns 200": (r) => r.status === 200,
    "qr validate has business payload": () =>
      body !== null &&
      (body.isValid === true || ACCEPTABLE_QR_ERROR_CODES.has(body.errorCode)),
  });
  sleep(0.15);
}

export function runSyncBatch(data) {
  const nowEpochSec = Math.floor(Date.now() / 1000);
  const transactionHash = `k6-${__VU}-${__ITER}-${nowEpochSec}`;
  const payload = JSON.stringify([
    {
      studentId: data.syncStudentId,
      mealType: SYNC_MEAL_TYPE,
      transactionHash: transactionHash,
      timestampEpochSec: nowEpochSec,
    },
  ]);

  const res = http.post(`${BASE_URL}/api/v1/transactions/batch`, payload, {
    headers: authHeaders(data.chefToken),
    tags: { endpoint: "sync_batch" },
  });
  const body = parseJson(res.body);

  check(res, {
    "sync batch returns 200": (r) => r.status === 200,
    "sync batch has response shape": () => body !== null && typeof body.successCount === "number",
    "sync batch has processed status": () =>
      body !== null &&
      Array.isArray(body.processed) &&
      body.processed.length > 0 &&
      typeof body.processed[0].status === "string",
  });
  sleep(0.2);
}

function login(login, password) {
  const payload = JSON.stringify({ login: login, password: password });
  const res = http.post(`${BASE_URL}/api/v1/auth/login`, payload, {
    headers: { "Content-Type": "application/json" },
    tags: { endpoint: "auth_login" },
  });
  return { res: res, body: parseJson(res.body) };
}

function authHeaders(token) {
  return {
    "Content-Type": "application/json",
    Authorization: `Bearer ${token}`,
  };
}

function parseJson(raw) {
  try {
    return JSON.parse(raw);
  } catch (_) {
    return null;
  }
}

function roundTimestampToMinute(timestampSec) {
  return Math.floor(timestampSec / 60) * 60;
}

async function signPayloadBase64(userId, timestamp, mealType, nonce, privateKeyBase64) {
  const payloadBytes = utf8ToBytes(`${userId}:${timestamp}:${mealType}:${nonce}`);
  const key = await ensureImportedPrivateKey(privateKeyBase64);
  if (!key) {
    throw new Error("Failed to import student private key for dynamic signing.");
  }
  const signatureBuffer = await crypto.subtle.sign(
    { name: "ECDSA", hash: "SHA-256" },
    key,
    payloadBytes,
  );
  return arrayBufferToBase64(signatureBuffer);
}

async function ensureImportedPrivateKey(privateKeyBase64) {
  if (importedPrivateKey !== null) return importedPrivateKey;
  const privateKeyBytes = decodeBase64ToArrayBuffer(privateKeyBase64);
  importedPrivateKey = await crypto.subtle.importKey(
    "pkcs8",
    privateKeyBytes,
    { name: "ECDSA", namedCurve: "P-256" },
    false,
    ["sign"],
  );
  return importedPrivateKey;
}

function decodeBase64ToArrayBuffer(raw) {
  const normalized = normalizeBase64(raw || "");
  const binary = atob(normalized);
  const bytes = new Uint8Array(binary.length);
  for (let i = 0; i < binary.length; i += 1) {
    bytes[i] = binary.charCodeAt(i);
  }
  return bytes.buffer;
}

function normalizeBase64(raw) {
  const trimmed = raw.replace(/\s+/g, "");
  const normalized = trimmed.replace(/-/g, "+").replace(/_/g, "/");
  const remainder = normalized.length % 4;
  if (remainder === 0) return normalized;
  return normalized + "=".repeat(4 - remainder);
}

function utf8ToBytes(value) {
  return new TextEncoder().encode(value);
}

function arrayBufferToBase64(buffer) {
  const bytes = new Uint8Array(buffer);
  let binary = "";
  for (let i = 0; i < bytes.length; i += 1) {
    binary += String.fromCharCode(bytes[i]);
  }
  return btoa(binary);
}
