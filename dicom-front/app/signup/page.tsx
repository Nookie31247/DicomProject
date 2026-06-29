"use client";

import { useState } from "react";
import Link from "next/link";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";

export default function SignupPage() {
  const [memberType, setMemberType] = useState<"doctor" | "researcher">(
    "researcher"
  );

  // 라디오 필 공통 스타일 + 선택 상태 스타일 (테두리 두께는 2px로 고정해 흔들림 방지)
  const pillBase =
    "flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl border-2 px-4 py-[13px] text-base transition-[border-color,background,color] duration-150";
  const pillActive =
    "border-mint-deep bg-[rgba(76,255,157,0.3)] text-ink font-bold";
  const pillIdle = "border-line bg-canvas text-ink-soft font-semibold";

  // 네이티브 라디오는 완전히 숨김 (선택 표시는 필 테두리/배경으로 대체)
  const hiddenRadio =
    "pointer-events-none absolute m-0 h-px w-px border-0 p-0 opacity-0 [clip:rect(0_0_0_0)] [clip-path:inset(50%)]";

  return (
    <div className="page">
      {/* ───────────── Auth section ───────────── */}
      {/* items-start: 의사 선택 시 카드가 길어져도 왼쪽 비주얼이 밀려나지 않게 */}
      <section className="grid flex-1 items-start grid-cols-[1.05fr_0.95fr] gap-14 pt-[72px] px-[clamp(24px,5vw,62px)] pb-24 max-[900px]:grid-cols-1 max-[560px]:px-5 max-[560px]:pt-10 max-[560px]:pb-16 max-[560px]:gap-9">
        <div
          className="flex flex-col items-center gap-7 max-[900px]:order-[-1] sticky top-24 max-[900px]:static"
          aria-hidden="true"
        >
          <ScanVisual />
          <p className="text-center font-semibold text-lg text-ink-soft leading-[1.5] max-w-[360px] max-[560px]:text-base">
            의사와 연구원 모두를 위한
            <br />
            안전한 DICOM 분석 플랫폼입니다.
          </p>
        </div>

        <div className="mx-auto w-full bg-paper rounded-3xl shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-w-[480px] px-[clamp(28px,4vw,52px)] py-[52px] max-[900px]:px-6.5 max-[900px]:py-10">
          <p className="eyebrow">DICOM!과 함께 시작해보세요</p>
          <h1 className="font-bold text-4xl tracking-[-0.01em] m-0 text-ink max-[560px]:text-3xl mb-7">회원가입</h1>

          <form className="flex flex-col gap-4.5">
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
                autoComplete="new-password"
              />
            </label>

            <label className="field">
              <span className="field-label">비밀번호 확인</span>
              <input
                type="password"
                name="passwordConfirm"
                placeholder="비밀번호를 다시 입력하세요"
                autoComplete="new-password"
              />
            </label>

            <label className="field">
              <span className="field-label">이름</span>
              <input type="text" name="name" placeholder="이름을 입력하세요" />
            </label>

            <fieldset className="field gap-2.5">
              <legend className="field-label">회원유형</legend>
              <div className="flex gap-3 max-[560px]:flex-col">
                <label
                  className={`${pillBase} ${
                    memberType === "doctor" ? pillActive : pillIdle
                  }`}
                >
                  <input
                    type="radio"
                    name="memberType"
                    value="doctor"
                    checked={memberType === "doctor"}
                    onChange={() => setMemberType("doctor")}
                    className={hiddenRadio}
                  />
                  의사
                </label>
                <label
                  className={`${pillBase} ${
                    memberType === "researcher" ? pillActive : pillIdle
                  }`}
                >
                  <input
                    type="radio"
                    name="memberType"
                    value="researcher"
                    checked={memberType === "researcher"}
                    onChange={() => setMemberType("researcher")}
                    className={hiddenRadio}
                  />
                  연구원
                </label>
              </div>
            </fieldset>

            {memberType === "doctor" && (
              <label className="field">
                <span className="field-label">의사면허번호</span>
                <input
                  type="text"
                  name="medicalLicenseNumber"
                  placeholder="의사면허번호를 입력하세요"
                />
              </label>
            )}

            <button className="btn btn-big w-full mt-2" type="button">
              회원가입
            </button>
          </form>

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
