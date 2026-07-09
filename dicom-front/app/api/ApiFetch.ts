// dicomApi.ts의 XHR 기반 업로드 진행률 추적 함수에서도 그대로 재사용한다.

export enum ApiServer {
  MEDICAL_SERVER,
  RESEARCH_SERVER
}

export async function ApiFetch(path: string, options: RequestInit = {}) {
  const response = await fetch(path, options);

  // No Content (204) 일 때는 그냥 리턴
  if(response.status === 204) {
    return null;
  }

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

    const error = new Error(message);
    Object.assign(error, { status: response.status });
    throw error;
  }

  return data;
}