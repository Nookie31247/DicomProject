import type { Metadata } from "next";
import Link from "next/link";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";
import { HomeRedirect } from "@/app/components/auth/RouteAccess";

export const metadata: Metadata = {
  title: "DICOM! — DICOM 파일을 보다 간편하게",
};

/**
 * 서비스 기능을 설명하는 홈페이지 컴포넌트입니다.
 * 히어로 섹션과 홍보 콘텐츠를 표시합니다.
 *
 * @returns 홈페이지 인터페이스
 */
export default function Home() {
  const heroPoints = [
    "DICOM 파일을 웹에서 손쉽게 볼 수 있습니다.",
    "DICOM 파일을 저장할 수 있습니다.",
    "AI가 자동으로 DICOM 파일을 분석해줍니다.",
    "등록된 자료는 자동으로 익명화되서 연구 기관에 전송됩니다.",
  ];

  return (
    <div className="page">
      <HomeRedirect />


      {/* ───────────── Hero ───────────── */}
      <section className="grid grid-cols-[1.15fr_0.85fr] items-center gap-12 bg-paper px-[clamp(24px,5vw,62px)] pt-16 pb-22 max-[900px]:grid-cols-1 max-[560px]:px-5 max-[560px]:pt-10 max-[560px]:pb-14">
        <div className="max-w-155 max-[900px]:order-1">
          <p className="eyebrow text-2xl tracking-[-0.01em] mb-4.5 max-[560px]:text-lg">
            DICOM 파일을 보다 간편하게!
          </p>

          <ul className="m-0 mb-10 flex list-none flex-col gap-4.5 p-0">
            {heroPoints.map((text) => (
              <li
                key={text}
                className="relative pl-5.5 text-3xl font-semibold leading-[1.4] text-ink before:absolute before:left-0 before:top-3.25 before:h-2 before:w-2 before:rounded-xs before:bg-mint before:content-[''] max-[900px]:text-2xl max-[560px]:text-xl"
              >
                {text}
              </li>
            ))}
          </ul>

          <div className="flex gap-4.5 max-[560px]:flex-wrap">
            <Link
              href="/login"
              className="btn btn-big text-xl px-9 py-4.5 max-[560px]:flex-1 max-[560px]:px-5 max-[560px]:py-4 max-[560px]:text-lg"
            >
              로그인
            </Link>
            <Link
              href="/signup"
              className="btn btn-big text-paper bg-slate hover:-translate-y-0.5 hover:bg-[#0f1722] text-xl px-9 py-4.5 max-[560px]:flex-1 max-[560px]:px-5 max-[560px]:py-4 max-[560px]:text-lg"
            >
              시작하기
            </Link>
          </div>
        </div>

        <div className="flex justify-center max-[900px]:-order-1" aria-hidden="true">
          <ScanVisual />
        </div>
      </section>

      {/* ───────────── Promo / Intro panel ───────────── */}
      <section
        className="relative mx-[clamp(24px,5vw,62px)] mt-12 mb-20 flex min-h-140 flex-1 items-center justify-center overflow-hidden rounded-3xl p-12 max-[560px]:min-h-90 max-[560px]:px-6 max-[560px]:py-8"
        id="how-it-works"
        style={{
          background: "linear-gradient(160deg, #e2e5ea 0%, #d9d9d9 100%)",
        }}
      >
        <div
          className="absolute inset-0 opacity-50"
          aria-hidden="true"
          style={{
            backgroundImage:
              "radial-gradient(circle, rgba(255,255,255,0.6) 1.5px, transparent 1.5px)",
            backgroundSize: "26px 26px",
          }}
        />
        <p className="relative m-0 max-w-180 text-center text-4xl font-semibold text-ink-soft max-[560px]:text-2xl">
          여기에 서비스 소개(홍보) 내용을 작성합니다
        </p>
      </section>
    </div>
  );
}
