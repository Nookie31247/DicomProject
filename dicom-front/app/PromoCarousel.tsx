"use client";

import {
  ChevronLeft,
  ChevronRight,
  Download,
  Monitor,
  ShieldCheck,
  UsersRound,
} from "lucide-react";
import { useEffect, useRef, useState } from "react";

const features = [
  {
    number: "01",
    label: "WEB DICOM VIEWER",
    title: "설치 없이, 브라우저에서 바로 확인",
    description:
      "별도의 전용 프로그램을 실행하지 않아도 웹에서 DICOM 영상을 빠르게 열고 살펴볼 수 있습니다.",
    visualTitle: "웹 뷰어",
    visualCaption: "언제 어디서나 바로 확인",
    icon: Monitor,
  },
  {
    number: "02",
    label: "EASY ANONYMIZATION",
    title: "복잡한 익명화를 클릭 한 번으로",
    description:
      "여러 단계의 번거로운 작업을 줄이고, 연구에 필요한 DICOM 파일을 손쉽게 익명화할 수 있습니다.",
    visualTitle: "익명화 완료",
    visualCaption: "간결하게 이어지는 처리 과정",
    icon: ShieldCheck,
  },
  {
    number: "03",
    label: "QUICK DOWNLOAD",
    title: "필요한 영상은 원하는 순간에 간편하게",
    description:
      "관리 중인 DICOM 파일을 찾고 내려받는 과정을 단순하게 구성해 필요한 자료를 빠르게 활용할 수 있습니다.",
    visualTitle: "다운로드 준비",
    visualCaption: "필요한 파일을 빠르게 저장",
    icon: Download,
  },
  {
    number: "04",
    label: "PATIENT MANAGEMENT",
    title: "환자 정보 관리를 더 선명하고 편리하게",
    description:
      "현대적이고 직관적인 화면에서 환자, 검사, 시리즈 정보를 한눈에 파악하고 효율적으로 관리할 수 있습니다.",
    visualTitle: "환자 정보",
    visualCaption: "검사와 시리즈를 한눈에 관리",
    icon: UsersRound,
  },
] as const;

const SLIDE_DURATION = 5000;

export default function PromoCarousel() {
  const [activeIndex, setActiveIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const touchStartX = useRef<number | null>(null);

  useEffect(() => {
    if (isPaused) return;

    const timer = window.setTimeout(() => {
      setActiveIndex((current) => (current + 1) % features.length);
    }, SLIDE_DURATION);

    return () => window.clearTimeout(timer);
  }, [activeIndex, isPaused]);

  const showPrevious = () => {
    setActiveIndex((current) =>
      current === 0 ? features.length - 1 : current - 1,
    );
  };

  const showNext = () => {
    setActiveIndex((current) => (current + 1) % features.length);
  };

  const handleTouchEnd = (event: React.TouchEvent<HTMLElement>) => {
    if (touchStartX.current === null) return;

    const distance = event.changedTouches[0].clientX - touchStartX.current;
    touchStartX.current = null;

    if (Math.abs(distance) < 45) return;
    if (distance > 0) showPrevious();
    else showNext();
  };

  return (
    <section
      className="relative mx-[clamp(24px,5vw,62px)] mt-12 mb-20 min-h-140 overflow-hidden rounded-3xl bg-[linear-gradient(145deg,#eef1f4_0%,#e4e8eb_52%,#dce1e5_100%)] max-[560px]:min-h-120"
      id="how-it-works"
      aria-label="DICOM 서비스 주요 기능"
      onMouseEnter={() => setIsPaused(true)}
      onMouseLeave={() => setIsPaused(false)}
      onFocusCapture={() => setIsPaused(true)}
      onBlurCapture={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setIsPaused(false);
      }}
      onTouchStart={(event) => {
        touchStartX.current = event.touches[0].clientX;
      }}
      onTouchEnd={handleTouchEnd}
    >
      <div
        className="pointer-events-none absolute inset-0 opacity-60"
        aria-hidden="true"
        style={{
          backgroundImage:
            "radial-gradient(circle, rgba(255,255,255,0.85) 1.5px, transparent 1.5px)",
          backgroundSize: "26px 26px",
        }}
      />

      <div
        className="relative flex min-h-140 transition-transform duration-700 ease-[cubic-bezier(0.22,1,0.36,1)] motion-reduce:transition-none max-[560px]:min-h-120"
        style={{ transform: `translateX(-${activeIndex * 100}%)` }}
      >
        {features.map((feature, index) => {
          const Icon = feature.icon;

          return (
            <article
              key={feature.number}
              className="grid min-w-full grid-cols-[1.05fr_0.95fr] items-center gap-14 px-[clamp(64px,8vw,128px)] py-18 max-[900px]:grid-cols-1 max-[900px]:gap-9 max-[900px]:px-18 max-[900px]:py-16 max-[560px]:px-7 max-[560px]:pt-14 max-[560px]:pb-24"
              aria-hidden={activeIndex !== index}
            >
              <div className="relative z-10 max-w-165">
                <div className="mb-7 flex items-center gap-4">
                  <span className="font-mono text-sm font-bold tracking-[0.16em] text-mint-deep">
                    {feature.number}
                  </span>
                  <span className="h-px w-10 bg-mint-deep/45" aria-hidden="true" />
                  <span className="text-xs font-bold tracking-[0.14em] text-slate-500">
                    {feature.label}
                  </span>
                </div>

                <h2 className="m-0 max-w-150 text-[clamp(2rem,3.5vw,3.5rem)] font-bold leading-[1.16] tracking-[-0.035em] text-ink">
                  {feature.title}
                </h2>
                <p className="mt-6 mb-0 max-w-145 text-lg leading-[1.8] text-ink-soft max-[560px]:text-base max-[560px]:leading-[1.7]">
                  {feature.description}
                </p>
              </div>

              <div className="relative mx-auto flex aspect-[1.1/1] w-full max-w-110 items-center justify-center max-[900px]:hidden" aria-hidden="true">
                <div className="absolute inset-[8%] rounded-[38px] border border-white/80 bg-white/45 shadow-[0_30px_80px_rgba(15,31,61,0.10)] backdrop-blur-sm" />
                <div className="relative w-[76%] rounded-[28px] border border-white bg-white/90 p-7 shadow-[0_18px_45px_rgba(15,31,61,0.12)]">
                  <div className="mb-8 flex items-center justify-between">
                    <div className="flex h-14 w-14 items-center justify-center rounded-2xl bg-mint text-slate shadow-[0_8px_22px_rgba(76,255,157,0.35)]">
                      <Icon size={28} strokeWidth={2.2} />
                    </div>
                    <span className="rounded-full bg-[#e9fff3] px-3 py-1.5 text-xs font-bold text-mint-deep">
                      AI Doctor
                    </span>
                  </div>
                  <p className="m-0 text-2xl font-bold tracking-[-0.02em] text-ink">
                    {feature.visualTitle}
                  </p>
                  <p className="mt-2 mb-7 text-sm text-slate-500">
                    {feature.visualCaption}
                  </p>
                  <div className="space-y-3">
                    <div className="h-2.5 w-full rounded-full bg-slate-100" />
                    <div className="h-2.5 w-[82%] rounded-full bg-slate-100" />
                    <div className="h-2.5 w-[58%] rounded-full bg-mint/70" />
                  </div>
                </div>
              </div>
            </article>
          );
        })}
      </div>

      <button
        type="button"
        className="absolute top-1/2 left-5 z-20 flex h-11 w-11 -translate-y-1/2 cursor-pointer items-center justify-center rounded-full border border-white/80 bg-white/75 text-ink shadow-sm backdrop-blur-sm transition hover:bg-white focus-visible:outline-3 focus-visible:outline-mint-deep max-[560px]:top-auto max-[560px]:bottom-5 max-[560px]:left-7 max-[560px]:translate-y-0"
        onClick={showPrevious}
        aria-label="이전 서비스 소개"
      >
        <ChevronLeft size={22} />
      </button>

      <button
        type="button"
        className="absolute top-1/2 right-5 z-20 flex h-11 w-11 -translate-y-1/2 cursor-pointer items-center justify-center rounded-full border border-white/80 bg-white/75 text-ink shadow-sm backdrop-blur-sm transition hover:bg-white focus-visible:outline-3 focus-visible:outline-mint-deep max-[560px]:top-auto max-[560px]:right-7 max-[560px]:bottom-5 max-[560px]:translate-y-0"
        onClick={showNext}
        aria-label="다음 서비스 소개"
      >
        <ChevronRight size={22} />
      </button>

      <div className="absolute bottom-7 left-1/2 z-20 flex -translate-x-1/2 items-center gap-2.5 max-[560px]:bottom-8" role="tablist" aria-label="서비스 소개 선택">
        {features.map((feature, index) => (
          <button
            key={feature.number}
            type="button"
            role="tab"
            aria-selected={activeIndex === index}
            aria-label={`${index + 1}번째 서비스 소개 보기`}
            className={`h-2.5 cursor-pointer rounded-full transition-all duration-300 ${
              activeIndex === index
                ? "w-8 bg-mint-deep"
                : "w-2.5 bg-slate-400/45 hover:bg-slate-500/65"
            }`}
            onClick={() => setActiveIndex(index)}
          />
        ))}
      </div>
    </section>
  );
}
