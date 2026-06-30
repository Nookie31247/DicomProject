import Link from "next/link";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";
import SignupForm from "@/app/signup/SignupForm";

export default function SignupPage() {
  return (
    <div className="page">
      {/* ───────────── Auth section ───────────── */}
      {/* items-start: 의사 선택 시 카드가 길어져도 왼쪽 비주얼이 밀려나지 않게 */}
      <section className="grid flex-1 items-start grid-cols-[1.05fr_0.95fr] gap-14 pt-18 px-[clamp(24px,5vw,62px)] pb-24 max-[900px]:grid-cols-1 max-[560px]:px-5 max-[560px]:pt-10 max-[560px]:pb-16 max-[560px]:gap-9">
        <div
          className="flex flex-col items-center gap-7 max-[900px]:-order-1 sticky top-24 max-[900px]:static"
          aria-hidden="true"
        >
          <ScanVisual />
          <p className="text-center font-semibold text-lg text-ink-soft leading-normal max-w-90 max-[560px]:text-base">
            의사와 연구원 모두를 위한
            <br />
            안전한 DICOM 분석 플랫폼입니다.
          </p>
        </div>
        <div className="mx-auto w-full bg-paper rounded-3xl shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-w-120 px-[clamp(28px,4vw,52px)] py-13 max-[900px]:px-6.5 max-[900px]:py-10">
          <p className="eyebrow">DICOM!과 함께 시작해보세요</p>
          <h1 className="font-bold text-4xl tracking-[-0.01em] m-0 text-ink max-[560px]:text-3xl mb-7">회원가입</h1>
            <SignupForm />
          <p className="text-center text-base text-ink-soft mt-6">
            이미 계정이 있으신가요?{" "}
            <Link className="font-bold no-underline text-mint-deep hover:underline" href="/login">
              로그인
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
