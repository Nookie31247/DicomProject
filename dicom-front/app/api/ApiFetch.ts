/**
 * API 서버 환경의 열거형입니다.
 */
export enum ApiServer {
  MEDICAL_SERVER,
  RESEARCH_SERVER,
}

/**
 * 사용자 계정 역할을 나타내는 타입입니다.
 */
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

/**
 * 계정 유형에 따라 API 서버를 결정합니다.
 *
 * @param accountType - 계정 유형
 * @returns 해당하는 API 서버 환경
 */
export function apiServerForAccount(accountType: AccountType) {
  return accountType === "RESEARCHER" ? ApiServer.RESEARCH_SERVER : ApiServer.MEDICAL_SERVER;
}

/**
 * 로컬 스토리지에서 저장된 계정 유형을 가져옵니다.
 *
 * @returns 저장된 계정 유형, 기본값은 MEDICAL
 */
export function getStoredAccountType(): AccountType {
  if (typeof window === "undefined") {
    return "MEDICAL";
  }

  return localStorage.getItem("userType") === "RESEARCHER" ? "RESEARCHER" : "MEDICAL";
}

/**
 * 로컬 스토리지에서 저장된 인증 데이터를 지웁니다.
 * auth-state-changed 이벤트를 발생시킵니다.
 */
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

/**
 * 오류 처리 및 인증 상태 관리와 함께 API fetch 요청을 수행합니다.
 *
 * @param path - API 엔드포인트 경로
 * @param options - Fetch 요청 옵션
 * @returns 파싱된 응답 데이터 또는 null
 */
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

/**
 * 의료 서버로 향하는 API fetch 요청을 수행합니다.
 *
 * @param path - API 엔드포인트 경로
 * @param options - Fetch 요청 옵션
 * @returns 파싱된 응답 데이터 또는 null
 */
export function medicalApiFetch(path: string, options: RequestInit = {}) {
  return ApiFetch(normalizeApiPath(ApiServer.MEDICAL_SERVER, path), options);
}

/**
 * 연구 서버로 향하는 API fetch 요청을 수행합니다.
 *
 * @param path - API 엔드포인트 경로
 * @param options - Fetch 요청 옵션
 * @returns 파싱된 응답 데이터 또는 null
 */
export function researchApiFetch(path: string, options: RequestInit = {}) {
  return ApiFetch(normalizeApiPath(ApiServer.RESEARCH_SERVER, path), options);
}
