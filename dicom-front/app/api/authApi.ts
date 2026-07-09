import {AccountType, ApiFetch, getStoredAccountType,} from "./ApiFetch";

const USER_API_BASE = {
  MEDICAL: "/api/medical/users",
  RESEARCHER: "/api/research/users",
} satisfies Record<AccountType, string>;

function userApiBase(accountType: AccountType = getStoredAccountType()) {
  return USER_API_BASE[accountType];
}

/**
 * 사용자를 로그인합니다.
 *
 * @param userId - 사용자 ID
 * @param password - 사용자 비밀번호
 * @param accountType - 계정 유형
 * @returns 로그인 요청에 대한 응답 데이터
 */
export async function login(userId: string, password: string, accountType: AccountType) {
  return ApiFetch(`${userApiBase(accountType)}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, password }),
  });
}

/**
 * 새로운 사용자를 가입시킵니다.
 *
 * @param userId - 사용자 ID
 * @param password - 사용자 비밀번호
 * @param name - 사용자 이름
 * @param accountType - 계정 유형
 * @returns 가입 요청에 대한 응답 데이터
 */
export async function signup(userId: string, password: string, name: string, accountType: AccountType) {
  return ApiFetch(`${userApiBase(accountType)}/signup`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, password, name }),
  });
}

/**
 * 현재 사용자를 로그아웃합니다.
 *
 * @param accountType - 계정 유형
 * @returns 로그아웃 요청에 대한 응답 데이터
 */
export async function logout(accountType: AccountType = getStoredAccountType()) {
  return ApiFetch(`${userApiBase(accountType)}/logout`, {
    method: "POST",
    credentials: "include",
  });
}

/**
 * 사용자 ID가 사용 가능한지 확인합니다.
 *
 * @param userId - 확인할 사용자 ID
 * @param accountType - 계정 유형
 * @returns ID 확인 요청에 대한 응답 데이터
 */
export async function checkId(userId: string, accountType: AccountType) {
  return ApiFetch(`${userApiBase(accountType)}/check-id`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId }),
  });
}

/**
 * 사용자의 비밀번호를 변경합니다.
 *
 * @param currentPassword - 현재 비밀번호
 * @param newPassword - 새 비밀번호
 * @param accountType - 계정 유형
 * @returns 비밀번호 변경 요청에 대한 응답 데이터
 */
export async function changePassword(
  currentPassword: string,
  newPassword: string,
  accountType: AccountType = getStoredAccountType(),
) {
  return ApiFetch(`${userApiBase(accountType)}/change-password`, {
    method: "PUT",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ currentPassword, newPassword }),
  });
}

/**
 * 사용자 계정을 삭제합니다.
 *
 * @param password - 사용자 비밀번호
 * @param accountType - 계정 유형
 * @returns 삭제 요청에 대한 응답 데이터
 */
export async function deleteUser(password: string, accountType: AccountType = getStoredAccountType()) {
  return ApiFetch(`${userApiBase(accountType)}/delete`, {
    method: "DELETE",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ password }),
  });
}

/**
 * 사용자의 정보를 검색합니다.
 *
 * @param accountType - 계정 유형
 * @returns 사용자 정보를 포함하는 응답 데이터
 */
export async function getUserInfo(accountType: AccountType = getStoredAccountType()) {
  return ApiFetch(`${userApiBase(accountType)}/info`, {
    method: "GET",
    cache: "no-store",
    credentials: "include",
  });
}
