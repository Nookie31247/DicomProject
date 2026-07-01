import type { Metadata } from "next";
import Link from "next/link";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";
import LoginForm from "./LoginForm";

export const metadata: Metadata = {
  title: "로그인 — DICOM!",
};

export default function LoginPage() {

  return (
    <div className="page">
      {/* ───────────── Auth section ───────────── */}
      <section className="grid flex-1 items-center grid-cols-[1.05fr_0.95fr] gap-14 pt-18 px-[clamp(24px,5vw,62px)] pb-24 max-[900px]:grid-cols-1 max-[560px]:px-5 max-[560px]:pt-10 max-[560px]:pb-16 max-[560px]:gap-9">
        <div className="flex flex-col items-center gap-7 max-[900px]:-order-1" aria-hidden="true">
          <ScanVisual />
          <p className="text-center font-semibold text-lg text-ink-soft leading-normal max-w-90 max-[560px]:text-base">
            DICOM 파일을 안전하게 보관하고
            <br />
            AI 분석 결과를 바로 확인하세요.
          </p>
        </div>

        <div className="mx-auto w-full bg-paper rounded-3xl py-14 px-[clamp(32px,4vw,56px)] shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-w-120 max-[900px]:py-11 max-[900px]:px-7">
          <p className="eyebrow">다시 만나서 반가워요</p>
          <h1 className="font-bold text-4xl tracking-[-0.01em] m-0 mb-8 text-ink max-[560px]:text-3xl">로그인</h1>

          <LoginForm />

          <p className="text-center mt-7 text-base text-ink-soft">
            아직 계정이 없으신가요?{" "}
            <Link className="font-bold no-underline text-mint-deep hover:underline" href="/signup">
              회원가입
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
