// /api/dicom/upload 전용 Route Handler.
//
// 원래는 next.config.ts의 rewrites()가 /api/:path* 요청을 백엔드(8080)로 그대로 넘겨줬는데,
// 이 자동 rewrite 프록시가 용량이 좀 되는 multipart(파일 업로드) 요청에서 뒷부분 필드를
// 유실시키는 문제가 확인됐다. 그래서 업로드 요청만 이 라우트 핸들러가 직접 받아서 전달한다.
//
// 처음엔 request.body를 duplex:"half"로 스트리밍 전달했는데, 파일이 여러 개 섞인 요청에서
// Tomcat 쪽 멀티파트 파싱이 깨지는 문제가 재현됐다(파일 1개는 되고 여러 개는 실패).
// Content-Length 없이 청크 전송(chunked)으로 흘려보내는 과정에서 문제가 생기는 것으로 보여,
// body를 통째로 버퍼에 담아 정확한 Content-Length를 붙여 전달하는 방식으로 바꿨다.
// 최대 요청 크기가 500MB로 제한돼 있어 메모리에 잠깐 올리는 것도 무리 없다.
//
// 브라우저 입장에서는 여전히 localhost:3000(같은 오리진)에만 요청하는 것이므로
// 쿠키/CORS 문제도 생기지 않는다. Next.js는 실제 파일 라우트(이 route.ts)를 rewrites보다
// 우선 매칭하므로, 다른 /api/dicom/** 경로들은 지금처럼 rewrite를 그대로 탄다.

// Edge 런타임은 body 크기 제한이 더 빡빡할 수 있어 Node 런타임을 명시한다.
export const runtime = "nodejs";

export async function POST(request: Request) {
    const contentType = request.headers.get("content-type") ?? "";
    const bodyBuffer = await request.arrayBuffer();

    // 로그아웃 등으로 프론트에서 업로드를 취소(AbortController.abort())하면
    // request.signal도 같이 취소되는데, 이걸 백엔드로 가는 요청에도 그대로 넘겨줘야
    // 브라우저 쪽만 끊기고 백엔드/Orthanc 쪽 처리는 계속 진행되는 상황을 막을 수 있다.
    const backendResponse = await fetch("http://localhost:8080/api/dicom/upload", {
        method: "POST",
        headers: {
            "Content-Type": contentType,
            "Content-Length": String(bodyBuffer.byteLength),
        },
        body: bodyBuffer,
        signal: request.signal,
    });

    const responseText = await backendResponse.text();

    return new Response(responseText, {
        status: backendResponse.status,
        headers: {
            "Content-Type": backendResponse.headers.get("content-type") ?? "text/plain",
        },
    });
}
