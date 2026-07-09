export enum ApiServer {
  MEDICAL_SERVER,
  RESEARCH_SERVER,
}

export type AccountType = "MEDICAL" | "RESEARCHER";

const AUTH_ERROR_CODES = new Set(["JWT_001", "JWT_002", "JWT_003", "JWT_004"]);

function normalizeApiPath(server: ApiServer, path: string) {
  if (path.startsWith("/api/medical/") || path.startsWith("/api/research/")) {
    return path;
  }

  const prefix = server === ApiServer.RESEARCH_SERVER ? "/api/research" : "/api/medical";

  if (path.startsWith("/api/")) {
    return `${prefix}${path.slice("/api".length)}`;
  }

  return path.startsWith("/") ? `${prefix}${path}` : `${prefix}/${path}`;
}

export function apiServerForAccount(accountType: AccountType) {
  return accountType === "RESEARCHER" ? ApiServer.RESEARCH_SERVER : ApiServer.MEDICAL_SERVER;
}

export function getStoredAccountType(): AccountType {
  if (typeof window === "undefined") {
    return "MEDICAL";
  }

  return localStorage.getItem("userType") === "RESEARCHER" ? "RESEARCHER" : "MEDICAL";
}

export function clearStoredAuth() {
  if (typeof window === "undefined") {
    return;
  }

  localStorage.removeItem("username");
  localStorage.removeItem("userType");
  window.dispatchEvent(new Event("auth-state-changed"));
}

function isAuthError(data: unknown) {
  return !!(
    data
    && typeof data === "object"
    && "code" in data
    && typeof data.code === "string"
    && AUTH_ERROR_CODES.has(data.code)
  );
}

export async function ApiFetch(path: string, options: RequestInit = {}) {
  const response = await fetch(path, options);

  if (response.status === 204) {
    return null;
  }

  const contentType = response.headers.get("Content-Type");
  const text = await response.text();
  const data = contentType?.includes("application/json")
    ? (text ? JSON.parse(text) : null)
    : text;

  if (!response.ok) {
    if (isAuthError(data)) {
      clearStoredAuth();
    }

    const message = (data && typeof data === "object" && "message" in data)
      ? data.message
      : `요청 실패 (status: ${response.status})`;

    const error = new Error(message);
    Object.assign(error, { status: response.status });
    throw error;
  }

  return data;
}

export function medicalApiFetch(path: string, options: RequestInit = {}) {
  return ApiFetch(normalizeApiPath(ApiServer.MEDICAL_SERVER, path), options);
}

export function researchApiFetch(path: string, options: RequestInit = {}) {
  return ApiFetch(normalizeApiPath(ApiServer.RESEARCH_SERVER, path), options);
}
