import type { NextConfig } from "next";

const nextConfig: NextConfig = {
  reactStrictMode: false,
  async rewrites() {
    return [
      {
        source: "/api/medical/:path*",
        destination: "http://localhost:8080/api/medical/:path*",
      },
      {
        source: "/api/research/:path*",
        destination: "http://localhost:8081/api/research/:path*",
      },
    ];
  },
};

export default nextConfig;
