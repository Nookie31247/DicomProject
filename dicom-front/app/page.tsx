import type { Metadata } from "next";
import Link from "next/link";
import "./home.css";

export const metadata: Metadata = {
  title: "DICOM! — DICOM 파일을 보다 간편하게",
};

export default function Home() {
  return (
    <div className="page">
      {/* ───────────── Nav ───────────── */}
      <header className="nav">
        <span className="logo">DICOM!</span>
        <nav className="nav-links">
          <Link href="#how-it-works">사용방법</Link>
          <Link href="#faq">FAQ</Link>
        </nav>
      </header>

      {/* ───────────── Hero ───────────── */}
      <section className="hero">
        <div className="hero-copy">
          <p className="eyebrow">DICOM 파일을 보다 간편하게!</p>

          <ul className="hero-points">
            <li>DICOM 파일을 웹에서 손쉽게 볼 수 있습니다.</li>
            <li>DICOM 파일을 저장할 수 있습니다.</li>
            <li>AI가 자동으로 DICOM 파일을 분석해줍니다.</li>
            <li>등록된 자료는 자동으로 익명화되서 연구 기관에 전송됩니다.</li>
          </ul>

          <div className="cta-row">
            <Link href="/login" className="btn btn-primary">
              로그인
            </Link>
            <Link href="/signup" className="btn btn-secondary">
              시작하기
            </Link>
          </div>
        </div>

        <div className="hero-visual" aria-hidden="true">
          <div className="scan-frame">
            <div className="scan-grid" />
            <div className="scan-line" />
            <div className="scan-corner tl" />
            <div className="scan-corner tr" />
            <div className="scan-corner bl" />
            <div className="scan-corner br" />
            <span className="scan-tag">SLICE 084 / 220</span>
          </div>
        </div>
      </section>

      {/* ───────────── Promo / Intro panel ───────────── */}
      <section className="promo" id="how-it-works">
        <div className="promo-pattern" aria-hidden="true" />
        <p className="promo-text">
          여기에 서비스 소개(홍보) 내용을 작성합니다
        </p>
      </section>
    </div>
  );
}
