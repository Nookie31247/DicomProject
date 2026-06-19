"use client";

import { useState } from "react";
import Link from "next/link";

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
      {/* ───────────── Nav ───────────── */}
      <header className="nav">
        <Link href="/" className="logo">
          DICOM!
        </Link>
        <nav className="nav-links">
          <Link href="/#how-it-works">사용방법</Link>
          <Link href="/#faq">FAQ</Link>
        </nav>
      </header>

      {/* ───────────── Auth section ───────────── */}
      {/* items-start: 의사 선택 시 카드가 길어져도 왼쪽 비주얼이 밀려나지 않게 */}
      <section className="auth items-start">
        <div
          className="auth-visual sticky top-24 max-[900px]:static"
          aria-hidden="true"
        >
          <div className="scan-frame">
            <div className="scan-grid" />
            <div className="scan-line" />
            <div className="scan-corner tl" />
            <div className="scan-corner tr" />
            <div className="scan-corner bl" />
            <div className="scan-corner br" />
            <span className="scan-tag">SLICE 084 / 220</span>
          </div>
          <p className="visual-caption">
            의사와 연구원 모두를 위한
            <br />
            안전한 DICOM 분석 플랫폼입니다.
          </p>
        </div>

        <div className="auth-card px-[clamp(28px,4vw,52px)] py-[52px] max-[900px]:px-[26px] max-[900px]:py-10">
          <p className="eyebrow">DICOM!과 함께 시작해보세요</p>
          <h1 className="auth-title mb-7">회원가입</h1>

          <form className="auth-form gap-[18px]">
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

            <button className="btn btn-primary btn-block" type="button">
              회원가입
            </button>
          </form>

          <p className="switch-line mt-6">
            이미 계정이 있으신가요?{" "}
            <Link className="switch-link" href="/login">
              로그인
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
