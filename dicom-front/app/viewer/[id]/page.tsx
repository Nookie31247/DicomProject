"use client";

import { useState, useEffect, useRef, useMemo } from "react";
import Link from "next/link";
import { useParams, useRouter } from "next/navigation";
import { patients, currentUser, type DicomItem } from "@/mock-data";

// 타입 정의
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

export default function WorkspacePage() {
    const router = useRouter();
    const params = useParams();
    const studyIdFromUrl = params?.id as string;

    const currentPatient = patients.find((p) =>
        p.items.some((it) => it.id === studyIdFromUrl)
    ) || patients[0];

    const currentStudy: DicomItem | null =
        currentPatient?.items.find((it) => it.id === studyIdFromUrl) ?? null;

    // [해결] useEffect + useState 대신 useMemo 사용
    // currentStudy가 바뀔 때만 seriesList가 재계산됩니다.
    const seriesList = useMemo(() => {
        if (!currentStudy) return [];
        return [
            {
                id: `${currentStudy.id}-s1`, seriesNumber: 1, description: `${currentStudy.modality} Axial Scan`,
                images: 30, date: currentStudy.studyDate,
                bodyPart: getRandomBodyPart(), thickness: "5.0mm", contrast: false, ww: 80, wl: 40
            },
            {
                id: `${currentStudy.id}-s2`, seriesNumber: 2, description: `${currentStudy.modality} Coronal Scan`,
                images: 20, date: currentStudy.studyDate,
                bodyPart: getRandomBodyPart(), thickness: "3.0mm", contrast: true, ww: 150, wl: 50
            },
            {
                id: `${currentStudy.id}-s3`, seriesNumber: 3, description: `${currentStudy.modality} Sagittal Scan`,
                images: 15, date: currentStudy.studyDate,
                bodyPart: getRandomBodyPart(), thickness: "1.25mm", contrast: true, ww: 350, wl: 40
            },
        ];
    }, [currentStudy]);

    const [selectedSeriesId, setSelectedSeriesId] = useState<string | null>(null);
    const [currentImageIndex, setCurrentImageIndex] = useState<number>(1);
    const thumbnailRefs = useRef<(HTMLButtonElement | null)[]>([]);

    // 초기 선택 로직은 그대로 유지
    useEffect(() => {
        if (seriesList.length > 0 && !selectedSeriesId) {
            setSelectedSeriesId(seriesList[0].id);
        }
    }, [seriesList, selectedSeriesId]);

    const currentSeries = seriesList.find((s) => s.id === selectedSeriesId) || seriesList[0];

    const handleSelectSeries = (id: string) => {
        setSelectedSeriesId(id);
        setCurrentImageIndex(1);
    };

    useEffect(() => {
        const activeThumbnail = thumbnailRefs.current[currentImageIndex];
        if (activeThumbnail) {
            activeThumbnail.scrollIntoView({ behavior: "smooth", block: "center" });
        }
    }, [currentImageIndex]);

    function handleLogout() { router.push("/"); }
    const handleGoBack = () => { router.push("/workspace"); };

    const handleViewerWheel = (e: React.WheelEvent<HTMLDivElement>) => {
        if (!currentSeries) return;
        if (e.deltaY > 0) {
            setCurrentImageIndex((prev) => Math.min(prev + 1, currentSeries.images));
        } else if (e.deltaY < 0) {
            setCurrentImageIndex((prev) => Math.max(prev - 1, 1));
        }
    };

    return (
        <div className="page flex h-screen flex-col overflow-hidden">
            <header className="nav shrink-0">
                <Link href="/" className="logo">DICOM!</Link>
                <div className="nav-user">
                    <span className="user-avatar">{currentUser.name.charAt(0)}</span>
                    <span className="user-name">{currentUser.name}님</span>
                    <button type="button" className="logout-btn" onClick={handleLogout}>로그아웃</button>
                </div>
            </header>

            <section className="workspace grid flex-1 overflow-hidden h-full" style={{ gridTemplateColumns: "3fr 7fr" }}>
                <aside className="ws-panel studies-panel flex h-full flex-col">
                    <div className="ws-panel-head shrink-0">
                        <div className="ws-head-left">
                            <div className="ws-title-row flex items-center gap-2">
                                <button type="button" className="ws-collapse-btn transform-none text-base font-bold" onClick={handleGoBack}>←</button>
                                <h2 className="ws-panel-title">시리즈 목록</h2>
                            </div>
                            <span className="ws-sub-label">{currentPatient.name} · {currentPatient.patientId}</span>
                        </div>
                    </div>

                    <div className="study-table flex-1 overflow-y-auto">
                        <div className="study-head flex items-center px-4 py-3 text-[13px] font-bold text-[#888]">
                            <span className="w-16">시리즈</span>
                            <span className="flex-1">시리즈 정보</span>
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
                                        <span className="w-16 font-mono text-[#00FF66] font-bold">#{ser.seriesNumber}</span>

                                        <div className="flex-1 flex flex-col items-start pr-2 overflow-hidden">
                                            <span className="truncate text-gray-600 w-full font-medium">{ser.description}</span>
                                            <span className="text-[11px] text-[#888] mt-0.5 mb-1 tracking-wide">{ser.date}</span>
                                            <div className="flex gap-1.5 text-[10px] font-mono">
                                                <span className="bg-[#2a2a2a] text-[#aaa] px-1.5 py-0.5 rounded-sm">{ser.bodyPart}</span>
                                                <span className="bg-[#2a2a2a] text-[#aaa] px-1.5 py-0.5 rounded-sm">{ser.thickness}</span>
                                                {ser.contrast && <span className="text-[#00FF66] bg-[#00FF66]/10 px-1.5 py-0.5 rounded-sm border border-[#00FF66]/30">+CONTRAST</span>}
                                            </div>
                                        </div>
                                        <span className="w-16 text-right text-gray-400">{ser.images}장</span>
                                    </button>
                                </li>
                            ))}
                        </ul>
                    </div>
                </aside>

                <section className="ws-panel viewer-panel flex h-full flex-col overflow-hidden">
                    <div className="ws-panel-head flex items-center justify-between shrink-0">
                        <div className="ws-head-left">
                            <h2 className="ws-panel-title">{currentStudy ? `${currentStudy.description} - DICOM VIEWER` : "DICOM VIEWER"}</h2>
                            {currentStudy && currentSeries && (
                                <span className="ws-sub-label">{currentStudy.modality} · 시리즈 #{currentSeries.seriesNumber} · {currentSeries.date}</span>
                            )}
                        </div>
                        <button type="button" className="ai-analyze-btn rounded-sm bg-[#00FF66] px-[18px] py-2 text-[13px] font-bold text-black border-none cursor-pointer transition-colors hover:bg-[#00e65c]">AI 판독</button>
                    </div>

                    <div className="viewer-stage flex flex-row justify-between items-start flex-1 p-10 px-5 overflow-hidden min-h-0 relative" onWheel={handleViewerWheel}>
                        <div className="thumbnail-bar flex flex-col gap-3 w-32 shrink-0 overflow-y-auto overflow-x-hidden pr-1 max-h-full [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] [scrollbar-width:none]" onWheel={(e) => e.stopPropagation()}>
                            {currentSeries ? (
                                Array.from({ length: currentSeries.images }, (_, i) => i + 1).map((idx) => (
                                    <button key={idx} ref={(el) => { thumbnailRefs.current[idx] = el; }} type="button" onClick={() => setCurrentImageIndex(idx)}
                                            className={`thumb-item group flex h-25 w-25 shrink-0 cursor-pointer items-center justify-center rounded-md border text-xs font-bold transition-all duration-150 ${
                                                idx === currentImageIndex ? "border-[#00FF66] bg-white/15 text-[#00FF66] border-2" : "border-[#333] bg-[#1a1a1a] text-[#888]"
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
                                        <div className="absolute top-4 left-4 flex flex-col gap-1 text-[11px] font-mono text-[#00FF66] opacity-80 z-20">
                                            <span>{currentPatient.name}</span>
                                            <span>{currentPatient.patientId}</span>
                                            <span className="mt-1 text-white">{currentSeries.bodyPart}</span>
                                        </div>
                                        <div className="absolute bottom-4 left-4 flex flex-col gap-1 text-[11px] font-mono text-[#888] z-20">
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