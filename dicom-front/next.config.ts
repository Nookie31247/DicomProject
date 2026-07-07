import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: false,
  async rewrites() {
    return [
      {
        source: "/api/:path*",
        destination: "http://localhost:8080/api/:path*", // 프론트엔드에서 /api/xxx 로 보내면 실제로는 8080 백엔드로 전달됨
      },
    ];
  },
};

export default nextConfig;
