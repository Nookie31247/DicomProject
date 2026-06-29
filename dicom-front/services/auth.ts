const BASE_URL = process.env.NEXT_PUBLIC_API_URL || "http://localhost:8080";

// 공통 fetch wrapper (토큰 처리 및 헤더 관리)
const api = async (endpoint: string, options: RequestInit = {}) => {
    const headers = {
        "Content-Type": "application/json",
        ...options.headers,
    };

    const response = await fetch(`${BASE_URL}${endpoint}`, { ...options, headers });

    if (!response.ok) {
        const errorData = await response.json().catch(() => ({}));
        throw new Error(errorData.message || "API 요청 실패");
    }

    // 204 No Content는 json 파싱을 시도하면 에러가 나므로 처리
    if(response.status === 201 || response.status === 204) {
        return null;
    }

    return await response.json();
};

export const userApi = {
    login: (data: any) => api("/api/users/login", { method: "POST", body: JSON.stringify(data) }),

    signup: (data: any) => api("/api/users/signup", { method: "POST", body: JSON.stringify(data) }),

    logout: (token: string) => api("/api/users/logout", {
        method: "POST",
        headers: { "Authorization": token }
    }),

    checkId: (userId: string) => api("/api/users/check-id", {
        method: "POST",
        body: JSON.stringify({ userId })
    }),

    changePassword: (token: string, data: any) => api("/api/users/change-password", {
        method: "PUT",
        headers: { "Authorization": token },
        body: JSON.stringify(data)
    }),

    deleteUser: (token: string, data: any) => api("/api/users/delete", {
        method: "DELETE",
        headers: { "Authorization": token },
        body: JSON.stringify(data)
    }),
};