const BASE_URL = typeof window === 'undefined'
  ? (process.env.SERVER_API_URL || 'http://localhost:8080')   // 서버 사이드용 내부망 주소
  : (process.env.NEXT_PUBLIC_API_URL || "");                  // 브라우저용 프록시 상대 경로

export async function apiFetch(path: string, options: RequestInit = {}) {
  const response = await fetch(BASE_URL + path, options);
  const contentType = response.headers.get("Content-Type");
  const text = await response.text();

  if (contentType?.includes("application/json")) {
    return text ? JSON.parse(text) : null;
  }
  return text;
}