type AccountType = "MEDICAL" | "RESEARCHER";

const BASE_URLS: Record<AccountType, string> = {
  MEDICAL: process.env.NEXT_PUBLIC_MEDICAL_API_URL || "",
  RESEARCHER: process.env.NEXT_PUBLIC_RESEARCH_API_URL || "",
};

const USER_PATHS: Record<AccountType, string> = {
  MEDICAL: "/api/medical/users",
  RESEARCHER: "/api/research/users",
};

const api = async (accountType: AccountType, endpoint: string, options: RequestInit = {}) => {
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  const response = await fetch(`${BASE_URLS[accountType]}${endpoint}`, { ...options, headers });

  if (!response.ok) {
    const errorData = await response.json().catch(() => ({}));
    throw new Error(errorData.message || "API 요청 실패");
  }

  if (response.status === 201 || response.status === 204) {
    return null;
  }

  return response.json();
};

const userPath = (accountType: AccountType) => USER_PATHS[accountType];

export const userApi = {
  login: (data: { userId: string; password: string }, accountType: AccountType = "MEDICAL") =>
    api(accountType, `${userPath(accountType)}/login`, { method: "POST", body: JSON.stringify(data) }),

  signup: (
    data: { userId: string; password: string; name: string },
    accountType: AccountType = "MEDICAL",
  ) =>
    api(accountType, `${userPath(accountType)}/signup`, {
      method: "POST",
      body: JSON.stringify(data),
    }),

  logout: (token: string, accountType: AccountType = "MEDICAL") =>
    api(accountType, `${userPath(accountType)}/logout`, {
      method: "POST",
      headers: { Authorization: token },
    }),

  checkId: (userId: string, accountType: AccountType = "MEDICAL") =>
    api(accountType, `${userPath(accountType)}/check-id`, {
      method: "POST",
      body: JSON.stringify({ userId }),
    }),

  changePassword: (token: string, data: { currentPassword: string; newPassword: string }, accountType: AccountType = "MEDICAL") =>
    api(accountType, `${userPath(accountType)}/change-password`, {
      method: "PUT",
      headers: { Authorization: token },
      body: JSON.stringify(data),
    }),

  deleteUser: (token: string, data: { password: string }, accountType: AccountType = "MEDICAL") =>
    api(accountType, `${userPath(accountType)}/delete`, {
      method: "DELETE",
      headers: { Authorization: token },
      body: JSON.stringify(data),
    }),
};
