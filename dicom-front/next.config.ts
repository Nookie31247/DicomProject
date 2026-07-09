import type { NextConfig } from "next";

const medicalApiOrigin = (process.env.MEDICAL_API_ORIGIN ?? "http://localhost:8080").replace(/\/$/, "");
const researchApiOrigin = (process.env.RESEARCH_API_ORIGIN ?? "http://localhost:8081").replace(/\/$/, "");

const nextConfig: NextConfig = {
  reactStrictMode: false,
  async rewrites() {
    return [
      {
        source: "/api/medical/:path*",
        destination: `${medicalApiOrigin}/api/medical/:path*`,
      },
      {
        source: "/api/research/:path*",
        destination: `${researchApiOrigin}/api/research/:path*`,
      },
    ];
  },
};

export default nextConfig;
