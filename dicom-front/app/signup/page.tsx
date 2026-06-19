"use client";

import { useState } from "react";
import Link from "next/link";
import "../styles/auth.css";
import "./signup.css";

export default function SignupPage() {
  const [memberType, setMemberType] = useState<"doctor" | "researcher">(
    "researcher"
  );

  return (
    <div className="page signup-page">
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
      <section className="auth">
        <div className="auth-visual" aria-hidden="true">
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

        <div className="auth-card">
          <p className="eyebrow">DICOM!과 함께 시작해보세요</p>
          <h1 className="auth-title">회원가입</h1>

          <form className="auth-form">
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

            <fieldset className="field field-radio">
              <legend className="field-label">회원유형</legend>
              <div className="radio-row">
                <label
                  className={`radio-pill ${
                    memberType === "doctor" ? "active" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="memberType"
                    value="doctor"
                    checked={memberType === "doctor"}
                    onChange={() => setMemberType("doctor")}
                  />
                  의사
                </label>
                <label
                  className={`radio-pill ${
                    memberType === "researcher" ? "active" : ""
                  }`}
                >
                  <input
                    type="radio"
                    name="memberType"
                    value="researcher"
                    checked={memberType === "researcher"}
                    onChange={() => setMemberType("researcher")}
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

          <p className="switch-line">
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
