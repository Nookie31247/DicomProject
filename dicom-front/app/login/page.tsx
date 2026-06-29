import type { Metadata } from "next";
import Link from "next/link";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";

export const metadata: Metadata = {
  title: "로그인 — DICOM!",
};

export default function LoginPage() {
  return (
    <div className="page">
      {/* ───────────── Auth section ───────────── */}
      <section className="grid flex-1 items-center grid-cols-[1.05fr_0.95fr] gap-14 pt-[72px] px-[clamp(24px,5vw,62px)] pb-24 max-[900px]:grid-cols-1 max-[560px]:px-5 max-[560px]:pt-10 max-[560px]:pb-16 max-[560px]:gap-9">
        <div className="flex flex-col items-center gap-7 max-[900px]:order-[-1]" aria-hidden="true">
          <ScanVisual />
          <p className="text-center font-semibold text-lg text-ink-soft leading-[1.5] max-w-[360px] max-[560px]:text-base">
            DICOM 파일을 안전하게 보관하고
            <br />
            AI 분석 결과를 바로 확인하세요.
          </p>
        </div>

        <div className="mx-auto w-full bg-paper rounded-3xl py-14 px-[clamp(32px,4vw,56px)] shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-w-[480px] max-[900px]:py-11 max-[900px]:px-7">
          <p className="eyebrow">다시 만나서 반가워요</p>
          <h1 className="font-bold text-4xl tracking-[-0.01em] m-0 mb-8 text-ink max-[560px]:text-3xl">로그인</h1>

          <form className="flex flex-col gap-5">
            <label className="field">
              <span className="field-label">아이디</span>
              <input
                type="text"
                name="username"
                placeholder="아이디를 입력하세요"
                autoComplete="username"
              />
            </label>

            <label className="field">
              <span className="field-label">비밀번호</span>
              <input
                type="password"
                name="password"
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
              />
            </label>

            <button className="btn btn-big w-full mt-2" type="button">
              로그인
            </button>
          </form>

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
