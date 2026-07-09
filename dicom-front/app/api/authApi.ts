import {
  AccountType,
  ApiFetch,
  getStoredAccountType,
} from "./ApiFetch";

const USER_API_BASE = {
  MEDICAL: "/api/medical/users",
  RESEARCHER: "/api/research/users",
} satisfies Record<AccountType, string>;

function userApiBase(accountType: AccountType = getStoredAccountType()) {
  return USER_API_BASE[accountType];
}

export async function login(userId: string, password: string, accountType: AccountType) {
  return ApiFetch(`${userApiBase(accountType)}/login`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, password }),
  });
}

export async function signup(userId: string, password: string, name: string, accountType: AccountType) {
  return ApiFetch(`${userApiBase(accountType)}/signup`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId, password, name }),
  });
}

export async function logout(accountType: AccountType = getStoredAccountType()) {
  return ApiFetch(`${userApiBase(accountType)}/logout`, {
    method: "POST",
    credentials: "include",
  });
}

export async function checkId(userId: string, accountType: AccountType) {
  return ApiFetch(`${userApiBase(accountType)}/check-id`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ userId }),
  });
}

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

export async function deleteUser(password: string, accountType: AccountType = getStoredAccountType()) {
  return ApiFetch(`${userApiBase(accountType)}/delete`, {
    method: "DELETE",
    credentials: "include",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ password }),
  });
}

export async function getUserInfo(accountType: AccountType = getStoredAccountType()) {
  return ApiFetch(`${userApiBase(accountType)}/info`, {
    method: "GET",
    cache: "no-store",
    credentials: "include",
  });
}
