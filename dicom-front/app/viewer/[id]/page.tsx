"use client";

import { useState, useEffect, useRef, useMemo, type CSSProperties } from "react";
import { useParams, useRouter } from "next/navigation";
import { patients, studies, series as allSeries } from "@/mock-data";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";


export default function WorkspacePage() {
    const router = useRouter();
    const params = useParams();
    const studyIdFromUrl = params?.id as string;

    const currentStudy = studies.find((s) => s["study-key"] === studyIdFromUrl) ?? null;
    const currentPatient = patients.find((p) => p["patient-id"] === currentStudy?.["patient-id"]) || patients[0];

    const seriesList = useMemo(() => {
        if (!currentStudy) return [];
        const filteredSeries = allSeries.filter((s) => s["study-key"] === currentStudy["study-key"]);
        
        if (filteredSeries.length > 0) {
            return filteredSeries.map((s) => ({
                id: s["series-key"],
                seriesNumber: s["series-index"],
                description: `${currentStudy.modality} Scan`,
                images: s["images-num"],
                date: s.datetime.replace("T", " ").split("Z")[0],
                bodyPart: s.bodypart,
                thickness: "5.0mm",
                contrast: false,
                ww: 80,
                wl: 40,
            }));
        }

        return [];
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
            activeThumbnail.scrollIntoView({ behavior: "auto", block: "center" });
        }
    }, [currentImageIndex]);

    const handleGoBack = () => { router.push("/workspace"); };

    const handleViewerWheel = (e: React.WheelEvent<HTMLDivElement>) => {
        if (!currentSeries) return;
        if (e.deltaY > 0) {
            setCurrentImageIndex((prev) => Math.min(prev + 1, currentSeries.images));
        } else if (e.deltaY < 0) {
            setCurrentImageIndex((prev) => Math.max(prev - 1, 1));
        }
    };

    const workspaceStyle = { "--ws-grid": "480px 1fr" } as CSSProperties;

    return (
        <div className="page flex h-screen flex-col overflow-hidden">

            <section className="workspace" style={workspaceStyle}>
                <aside className="ws-panel studies-panel flex h-full flex-col">
                    <div className="ws-panel-head shrink-0">
                        <div className="ws-head-left flex flex-col">
                            <div className="ws-title-row flex items-center gap-2">
                                <button type="button" className="ws-collapse-btn transform-none text-base font-bold" onClick={handleGoBack}>←</button>
                                <h2 className="ws-panel-title">시리즈 목록</h2>
                            </div>
                            {currentStudy && (
                                <div className="text-[15px] font-semibold text-slate-700 mt-1 mb-0.5 flex items-center gap-2">
                                    <span className={`modality-badge mod-${currentStudy.modality.toLowerCase()}`}>{currentStudy.modality}</span>
                                    <span>{currentStudy.description}</span>
                                    <span className="text-sm text-slate-500 font-normal">| {currentStudy.datetime.replace("T", " ").split("Z")[0]}</span>
                                </div>
                            )}
                            <span className="ws-sub-label">{currentPatient["patient-name"]} · {currentPatient["patient-birth"]}</span>
                        </div>
                    </div>

                    <div className="study-table flex-1 overflow-y-auto">
                        <div className="study-head flex items-center px-4 py-3 text-[13px] font-bold text-slate-500">
                            <span className="w-16">시리즈</span>
                            <span className="flex-1">촬영 일시</span>
                            <span className="w-16 text-center">부위</span>
                            <span className="w-16 text-right">영상 수</span>
                        </div>

                        <ul className="study-list">
                            {seriesList.length > 0 ? (
                                seriesList.map((ser) => (
                                    <li key={ser.id}>
                                        <button
                                            type="button"
                                            className={`study-row w-full flex items-center px-4 py-3 text-left transition-colors text-sm ${
                                                ser.id === selectedSeriesId ? "active" : ""
                                            }`}
                                            onClick={() => handleSelectSeries(ser.id)}
                                        >
                                            <span className="w-16 font-mono text-[#14b876] font-bold">#{ser.seriesNumber}</span>

                                            <div className="flex-1 flex flex-col items-start pr-2 justify-center overflow-hidden">
                                                <span className="text-[13px]  font-medium tracking-wide">{ser.date}</span>
                                            </div>
                                            <span className="w-16 text-center pl-2 truncate">{ser.bodyPart}</span>
                                            <span className="w-16 text-right ">{ser.images}장</span>
                                        </button>
                                    </li>
                                ))
                            ) : (
                                <li className="p-4 text-center text-slate-500 text-sm">
                                    시리즈가 없습니다.
                                </li>
                            )}
                        </ul>
                    </div>
                </aside>

                <section className="ws-panel viewer-panel relative flex h-full flex-col overflow-hidden">
                    <div className="flex items-center justify-center p-4 shrink-0">
                        <h2 className="text-4xl font-bold text-slate-600 tracking-wider">DICOM VIEWER</h2>
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
                            <ScanVisual />
                        </div>
                    </div>

                    <div className="absolute bottom-6 right-6 z-50">
                        <button type="button" className="logout-btn shadow-md">AI 판독</button>
                    </div>
                </section>
            </section>
        </div>
    );
}