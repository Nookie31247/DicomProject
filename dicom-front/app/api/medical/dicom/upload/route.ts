export const runtime = "nodejs";

const medicalApiOrigin = (process.env.MEDICAL_API_ORIGIN ?? "http://localhost:8080").replace(/\/$/, "");

export async function POST(request: Request) {
  const contentType = request.headers.get("content-type") ?? "";
  const bodyBuffer = await request.arrayBuffer();

  const backendResponse = await fetch(`${medicalApiOrigin}/api/medical/dicom/upload`, {
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
