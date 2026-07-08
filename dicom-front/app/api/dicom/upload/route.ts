// /api/dicom/upload 전용 Route Handler.
//
// 원래는 next.config.ts의 rewrites()가 /api/:path* 요청을 백엔드(8080)로 그대로 넘겨줬는데,
// 이 자동 rewrite 프록시가 용량이 좀 되는 multipart(파일 업로드) 요청에서 뒷부분 필드를
// 유실시키는 문제가 확인됐다. 그래서 업로드 요청만 이 라우트 핸들러가 직접 받아서,
// 우리가 통제할 수 있는 방식으로 body를 스트리밍 전달한다.
//
// 브라우저 입장에서는 여전히 localhost:3000(같은 오리진)에만 요청하는 것이므로
// 쿠키/CORS 문제도 생기지 않는다. Next.js는 실제 파일 라우트(이 route.ts)를 rewrites보다
// 우선 매칭하므로, 다른 /api/dicom/** 경로들은 지금처럼 rewrite를 그대로 탄다.

// Edge 런타임은 body 크기 제한이 더 빡빡할 수 있어, 스트리밍 프록시 용도로 Node 런타임을 명시한다.
export const runtime = "nodejs";

export async function POST(request: Request) {
    const contentType = request.headers.get("content-type") ?? "";

    const backendResponse = await fetch("http://localhost:8080/api/dicom/upload", {
        method: "POST",
        headers: {
            "Content-Type": contentType,
        },
        body: request.body,
        // Node의 fetch(undici)로 스트리밍 body를 보낼 때 필수 옵션.
        // TS 기본 RequestInit 타입에 아직 없어서 캐스팅 처리.
        duplex: "half",
    } as RequestInit);

    const responseText = await backendResponse.text();

    return new Response(responseText, {
        status: backendResponse.status,
        headers: {
            "Content-Type": backendResponse.headers.get("content-type") ?? "text/plain",
        },
    });
}
