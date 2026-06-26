"use client";

import { useState, useEffect, useRef, useMemo, type CSSProperties } from "react";
import { useParams, useRouter } from "next/navigation";
import { patients, studies, series as allSeries } from "@/mock-data";
import "../../styles.css";

// UI 렌더링을 위해 가공된 시리즈 타입 정의
interface Series {
    id: string;
    seriesNumber: number;
    description: string;
    images: number;
    date: string;
    bodyPart: string;
    thickness: string;
    contrast: boolean;
    ww: number;
    wl: number;
}

const BODY_PARTS = [
    "HEAD", "NECK", "CHEST", "ABDOMEN", "PELVIS",
    "L-SPINE", "C-SPINE", "SHOULDER", "KNEE", "ANKLE"
];

const getRandomBodyPart = () => BODY_PARTS[Math.floor(Math.random() * BODY_PARTS.length)];

// ISO 문자열에서 날짜(YYYY-MM-DD)만 잘라내는 유틸리티
const formatDate = (dateString?: string) => {
    if (!dateString) return "-";
    return dateString.split("T")[0];
};

export default function WorkspacePage() {
    const router = useRouter();
    const params = useParams();
    const studyIdFromUrl = params?.id as string;

    // 1. API 명세서 구조(studies 배열, Kebab-case 키)에 맞게 환자와 스터디 찾기
    const currentStudy = studies.find((s) => s["study-key"] === studyIdFromUrl) ?? null;
    const currentPatient = patients.find((p) => p["patient-id"] === currentStudy?.["patient-id"]) || patients[0];

    // 2. 스터디 내부의 series 배열을 기반으로 UI용 목록 생성
    const seriesList = useMemo(() => {
        if (!currentStudy) return [];

        return allSeries
            .filter((s) => s["study-key"] === currentStudy["study-key"])
            .map((s, idx) => ({
                id: s["series-key"],
                seriesNumber: s["series-index"],
                description: `${currentStudy.modality} Scan`,
                images: s["series-num"] > 10 ? s["series-num"] : s["series-num"] * 30, // 임시 영상 수
                date: formatDate(s.datetime),
                bodyPart: s.bodypart,
                // 아래는 UI 표시를 위한 가상 데이터
                thickness: idx % 2 === 0 ? "5.0mm" : "3.0mm",
                contrast: idx % 2 !== 0,
                ww: 80 + idx * 50,
                wl: 40 + idx * 10
            }));
    }, [currentStudy]);

    const [selectedSeriesId, setSelectedSeriesId] = useState<string | null>(null);
    const [currentImageIndex, setCurrentImageIndex] = useState<number>(1);
    const thumbnailRefs = useRef<(HTMLButtonElement | null)[]>([]);

    // 초기 선택 로직
    useEffect(() => {
        if (seriesList.length > 0 && !selectedSeriesId) {
            setSelectedSeriesId(seriesList[0].id);
        }
    }, [seriesList, selectedSeriesId]);

    const currentSeries = seriesList.find((s) => s.id === selectedSeriesId) || seriesList[0];

    const handleSelectSeries = (id: string) => {
        setSelectedSeriesId(id);
        setCurrentImageIndex(1); // 시리즈 변경 시 첫 번째 이미지로 초기화
    };

    // 썸네일 자동 스크롤
    useEffect(() => {
        const activeThumbnail = thumbnailRefs.current[currentImageIndex];
        if (activeThumbnail) {
            activeThumbnail.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }, [currentImageIndex]);

    const handleGoBack = () => { router.push("/workspace"); };

    // 마우스 휠로 이미지 탐색
    const handleViewerWheel = (e: React.WheelEvent<HTMLDivElement>) => {
        if (!currentSeries) return;
        if (e.deltaY > 0) {
            setCurrentImageIndex((prev) => Math.min(prev + 1, currentSeries.images));
        } else if (e.deltaY < 0) {
            setCurrentImageIndex((prev) => Math.max(prev - 1, 1));
        }
    };

    const workspaceStyle = { "--ws-grid": "440px 1fr" } as CSSProperties;

    return (
        <div className="page">
            <section className="workspace" style={workspaceStyle}>
                <aside className="ws-panel studies-panel flex flex-col">
                    <div className="ws-panel-head shrink-0">
                        <div className="ws-head-left">
                            <div className="ws-title-row flex items-center gap-2">
                                <button type="button" className="ws-collapse-btn transform-none text-base font-bold" onClick={handleGoBack}>←</button>
                                <h2 className="ws-panel-title">시리즈 목록</h2>
                            </div>
                            {/* Kebab-case 키로 환자 정보 출력 */}
                            <span className="ws-sub-label">{currentPatient["patient-name"]} · {currentPatient["patient-id"]}</span>
                        </div>
                    </div>

                    <div className="study-table flex-1 overflow-y-auto">
                        <div className="study-head flex items-center px-4 py-3 text-[12.5px] font-bold text-[#45526b]">
                            <span className="w-16">시리즈</span>
                            <span className="flex-1">시리즈 정보</span>
                            {/* 우측 정렬을 풀고 좌측 정렬(text-left)로 변경 */}
                            <span className="w-24 text-left">부위</span>
                            <span className="w-16 text-right">영상 수</span>
                        </div>

                        <ul className="study-list">
                            {seriesList.map((ser) => (
                                <li key={ser.id}>
                                    <button
                                        type="button"
                                        className={`study-row w-full flex items-center px-4 py-3 text-left transition-colors text-sm ${
                                            ser.id === selectedSeriesId ? "active" : ""
                                        }`}
                                        onClick={() => handleSelectSeries(ser.id)}
                                    >
                                        <span className="w-16 font-mono text-[#14b876] font-bold">#{ser.seriesNumber}</span>

                                        <div className="flex-1 flex flex-col items-start pr-2 overflow-hidden">
                                            <div>
                                                <span className="truncate text-[#0f1f3d] w-full font-semibold mr-2">{ser.description}</span>
                                            </div>
                                            <span className="text-[12px] text-[#45526b] mt-0.5 mb-1 tracking-wide">{ser.date}</span>
                                            <div className="flex gap-1.5 text-[10px] font-mono">
                                                {ser.contrast && <span className="text-[#14b876] bg-[#14b876]/10 px-1.5 py-0.5 rounded-sm border border-[#14b876]/30">+CONTRAST</span>}
                                            </div>
                                        </div>
                                        {/* 텍스트가 좌측에 정렬되도록 text-left로 수정 */}
                                        <span className="w-24 text-left text-[#45526b] truncate">{ser.bodyPart}</span>
                                        <span className="w-16 text-right text-[#45526b]">{ser.images}장</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>
                </aside>

                <section className="ws-panel viewer-panel flex flex-col">
                    <div className="ws-panel-head flex items-center justify-between shrink-0">
                        <div className="ws-head-left">
                            <h2 className="ws-panel-title">{currentStudy ? `${currentStudy.description} - DICOM VIEWER` : "DICOM VIEWER"}</h2>
                            {currentStudy && currentSeries && (
                                <span className="ws-sub-label">{currentStudy.modality} · 시리즈 #{currentSeries.seriesNumber} · {currentSeries.date}</span>
                            )}
                        </div>
                        <button type="button" className="minibtn text-[13px] px-[18px] py-2">AI 판독</button>
                    </div>

                    <div className="viewer-stage flex flex-row justify-between items-start flex-1 p-10 px-5 overflow-hidden min-h-0 relative" onWheel={handleViewerWheel}>
                        <div className="thumbnail-bar flex flex-col gap-3 w-32 shrink-0 overflow-y-auto overflow-x-hidden pr-1 max-h-full [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]" onWheel={(e) => e.stopPropagation()}>
                            {currentSeries ? (
                                Array.from({ length: currentSeries.images }, (_, i) => i + 1).map((idx) => (
                                    <button key={idx} ref={(el) => { thumbnailRefs.current[idx] = el; }} type="button" onClick={() => setCurrentImageIndex(idx)}
                                            className={`thumb-item group flex h-25 w-25 shrink-0 cursor-pointer items-center justify-center rounded-md border text-xs font-bold transition-all duration-150 ${
                                                idx === currentImageIndex ? "border-[#4cff9d] bg-white/15 text-[#4cff9d] border-2" : "border-[#333] bg-[#1a1a1a] text-[#888]"
                                            }`}>#{idx}</button>
                                ))
                            ) : null}
                        </div>

                        <div className="main-viewer-zone flex flex-1 justify-center items-start h-full px-4">
                            <div className="scan-frame relative flex w-full max-w-[960px] max-h-[540px] min-w-0 shrink flex-col items-center justify-center aspect-video bg-black border border-[#222]">
                                {currentSeries && (
                                    <>
                                        <div className="scan-grid" />
                                        <div className="scan-line" />
                                        <div className="scan-corner tl" />
                                        <div className="scan-corner tr" />
                                        <div className="scan-corner bl" />
                                        <div className="scan-corner br" />
                                        <div className="absolute top-4 left-4 flex flex-col gap-1 text-[11px] font-mono text-[#4cff9d] opacity-80 z-20">
                                            {/* Kebab-case 키로 환자 정보 출력 */}
                                            <span>{currentPatient["patient-name"]}</span>
                                            <span>{currentPatient["patient-id"]}</span>
                                            <span className="mt-1 text-white">{currentSeries.bodyPart}</span>
                                        </div>
                                        <div className="absolute bottom-4 left-4 flex flex-col gap-1 text-[11px] font-mono text-[#888   ] z-20">
                                            <span>W: {currentSeries.ww} / L: {currentSeries.wl}</span>
                                        </div>
                                    </>
                                )}
                                <div className="relative z-10 flex flex-col items-center">
                                    <span className="text-xl font-bold tracking-[0.5px] text-white">
                                        {currentStudy ? `${currentStudy.modality} IMAGE #${currentImageIndex}` : "NO IMAGE SELECTED"}
                                    </span>
                                </div>
                            </div>
                        </div>
                    </div>
                </section>
            </section>
        </div>
    );
}