"use client";

import { useState } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { patients, currentUser, type DicomItem } from "./mock-data";

export default function WorkspacePage() {
    const currentPatient = patients[0];

    // 선택된 DICOM 항목 상태 (첫 항목 기본 선택)
    const [selectedItemId, setSelectedItemId] = useState<string | null>(
        currentPatient?.items[0]?.id || null
    );

    // 현재 선택된 시리즈 내의 "몇 번째 이미지"인지 관리하는 상태 (기본값: 1번째)
    const [currentImageIndex, setCurrentImageIndex] = useState<number>(1);

    const selectedItem: DicomItem | null =
        currentPatient?.items.find((it) => it.id === selectedItemId) ?? null;

    // 검사 항목 클릭 시, 이미지 번호 초기화까지 이벤트 핸들러 한 곳에서 처리
    const handleSelectStudy = (id: string) => {
        setSelectedItemId(id);
        setCurrentImageIndex(1);
    };

    const router = useRouter();

    function handleLogout() {
        router.push("/");
    }

    // 돌아가기
    const handleGoBack = () => {
        router.push("./workspace");
    };

    return (
        <div className="page flex h-screen flex-col overflow-hidden">
            {/* ───────────── Header (상단 바) ───────────── */}
            <header className="nav shrink-0">
                <Link href="/" className="logo">
                    DICOM!
                </Link>
                <div className="nav-user">
                    <span className="user-avatar">{currentUser.name.charAt(0)}</span>
                    <span className="user-name">{currentUser.name}님</span>
                    <button type="button" className="logout-btn" onClick={handleLogout}>
                        로그아웃
                    </button>
                </div>
            </header>

            {/* ───────────── Workspace (메인 영역) ───────────── */}
            <section className="workspace grid flex-1 overflow-hidden h-full" style={{ gridTemplateColumns: "3fr 7fr" }}>

                {/* ── [3] 검사 목록 패널 ── */}
                <aside className="ws-panel studies-panel flex h-full flex-col">
                    <div className="ws-panel-head shrink-0">
                        <div className="ws-head-left">
                            <div className="ws-title-row flex items-center gap-2">
                                <button
                                    type="button"
                                    className="ws-collapse-btn transform-none text-base font-bold"
                                    onClick={handleGoBack}
                                    title="뒤로가기"
                                >
                                    ←
                                </button>
                                <h2 className="ws-panel-title">검사 목록</h2>
                            </div>
                            <span className="ws-sub-label">
                {currentPatient.name} · {currentPatient.patientId}
              </span>
                        </div>
                    </div>

                    <div className="study-table flex-1 overflow-y-auto">
                        <div className="study-head">
                            <span className="col-modality">모달리티</span>
                            <span className="col-date">검사 일자</span>
                            <span className="col-images">영상 수</span>
                            <span className="col-series text-center">비고</span>
                        </div>

                        <ul className="study-list">
                            {currentPatient?.items.map((it) => (
                                <li key={it.id}>
                                    <button
                                        type="button"
                                        className={`study-row ${it.id === selectedItemId ? "active" : ""}`}
                                        onClick={() => handleSelectStudy(it.id)}
                                    >
                    <span className="col-modality">
                      <span className={`modality-badge mod-${it.modality.toLowerCase()}`}>
                        {it.modality}
                      </span>
                    </span>
                                        <span className="col-date">{it.studyDate}</span>
                                        <span className="col-images">{it.images}</span>
                                        <span className="col-series text-center text-[#999]">-</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>
                </aside>

                {/* ── [7] 상세 뷰어 화면 패널 ── */}
                <section className="ws-panel viewer-panel flex h-full flex-col overflow-hidden">
                    <div className="ws-panel-head flex items-center justify-between shrink-0">
                        <div className="ws-head-left">
                            <div className="ws-title-row">
                                <h2 className="ws-panel-title">
                                    {selectedItem ? `${selectedItem.description} - DICOM VIEWER` : "DICOM VIEWER"}
                                </h2>
                            </div>
                            {selectedItem && (
                                <span className="ws-sub-label">
                  {selectedItem.modality} · 시리즈 #{selectedItem.seriesNumber}
                </span>
                            )}
                        </div>

                        {/* AI 판독 버튼 */}
                        <button
                            type="button"
                            className="ai-analyze-btn rounded-sm bg-[#00FF66] px-[18px] py-2 text-[13px] font-bold text-black border-none cursor-pointer transition-colors duration-200 hover:bg-[#00e65c]"
                        >
                            AI 판독
                        </button>
                    </div>

                    {/* 내부 뷰어 콘텐츠 영역 */}
                    <div className="viewer-stage flex flex-row justify-between items-start flex-1 p-10 px-5 overflow-hidden min-h-0 relative">

                        {/* ── [좌] 사이드 썸네일 바 ── */}
                        <div className="thumbnail-bar flex flex-col gap-3 w-26 shrink-0 overflow-y-auto overflow-x-hidden pr-1 max-h-full">
                            {selectedItem ? (
                                Array.from({ length: Math.min(selectedItem.images, 5) }, (_, i) => i + 1).map((idx) => (
                                    <button
                                        key={idx}
                                        type="button"
                                        onClick={() => setCurrentImageIndex(idx)}
                                        className={`thumb-item group flex h-25 w-25 shrink-0 cursor-pointer items-center justify-center rounded-md border text-xs font-bold transition-all duration-150 ${
                                            idx === currentImageIndex
                                                ? "border-[#00FF66] bg-white/15 text-[#00FF66] border-2"
                                                : "border-[#333] bg-[#1a1a1a] text-[#888]"
                                        }`}
                                    >
                                        #{idx}
                                    </button>
                                ))
                            ) : null}
                        </div>

                        {/* ── [우] 메인 화면 컨테이너 (16:9 와이드 비율 적용) ── */}
                        <div className="main-viewer-zone flex flex-1 justify-center items-start h-full px-4">
                            {/* ★ 교정: aspect-video(16:9) 적용 및 높이/너비 최적화 유연 가드 설정 */}
                            <div className="scan-frame relative flex w-full max-w-[960px] max-h-[540px] min-w-0 shrink flex-col items-center justify-center aspect-video">
                                <div className="scan-grid" />
                                <div className="scan-line" />
                                <div className="scan-corner tl" />
                                <div className="scan-corner tr" />
                                <div className="scan-corner bl" />
                                <div className="scan-corner br" />
                                <div className="relative z-10 flex flex-col items-center">
                                    <span className="text-xl font-bold tracking-[0.5px] text-white">
                                        {selectedItem ? `${selectedItem.modality} IMAGE #${currentImageIndex}` : "NO IMAGE SELECTED"}
                                    </span>
                                    {selectedItem && (
                                        <span className="mt-1.5 text-[13px] text-[#888]">
                                            ({currentImageIndex} / {selectedItem.images} Images)
                                        </span>
                                    )}
                                </div>
                                <span className="scan-tag">
                                    {selectedItem ? `${selectedItem.modality} · SERIES ${selectedItem.seriesNumber} · FRAME ${currentImageIndex}` : "PREVIEW"}
                                </span>
                            </div>
                        </div>
                    </div>
                </section>
            </section>
        </div>
    );
}