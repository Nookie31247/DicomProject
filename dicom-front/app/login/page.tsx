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
      <section className="auth">
        <div className="auth-visual" aria-hidden="true">
          <ScanVisual />
          <p className="visual-caption">
            DICOM 파일을 안전하게 보관하고
            <br />
            AI 분석 결과를 바로 확인하세요.
          </p>
        </div>

        <div className="auth-card">
          <p className="eyebrow">다시 만나서 반가워요</p>
          <h1 className="auth-title">로그인</h1>

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
                autoComplete="current-password"
              />
            </label>

            <button className="btn btn-primary btn-block" type="button">
              로그인
            </button>
          </form>

          <p className="switch-line">
            아직 계정이 없으신가요?{" "}
            <Link className="switch-link" href="/signup">
              회원가입
            </Link>
          </p>
        </div>
      </section>
    </div>
  );
}
