export const runtime = "nodejs";

export async function POST(request: Request) {
  const contentType = request.headers.get("content-type") ?? "";
  const bodyBuffer = await request.arrayBuffer();

  const backendResponse = await fetch("http://localhost:8080/api/medical/dicom/upload", {
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
