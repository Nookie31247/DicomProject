const BASE_URL = typeof window === 'undefined'
    ? (process.env.SERVER_API_URL || 'http://localhost:8080')
    : (process.env.NEXT_PUBLIC_API_URL || "");

export async function apiFetch(path: string, options: RequestInit = {}) {
  const response = await fetch(BASE_URL + path, options);
  const contentType = response.headers.get("Content-Type");
  const text = await response.text();
  const data = contentType?.includes("application/json")
      ? (text ? JSON.parse(text) : null)
      : text;

  if (!response.ok) {
    // 백엔드 GlobalExceptionHandler가 주는 { code, message } 형태를 활용
    const message = (data && typeof data === "object" && "message" in data)
        ? data.message
        : `요청 실패 (status: ${response.status})`;
    throw new Error(message);
  }

  return data;
}