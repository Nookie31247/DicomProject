"use client";

import { useState, useEffect, useRef, useMemo } from "react";
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

    // ── Tailwind 스타일 변수 (globals.css에서 이관) ──
    const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-[42px] px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
    const modalityColors: Record<string, string> = {
        ct: "bg-[#2563eb]",
        mr: "bg-[#7c3aed]",
        cr: "bg-[#0e7490]",
        us: "bg-[#c2410c]",
        pt: "bg-[#be185d]",
    };

    return (
        <div className="page flex h-screen flex-col overflow-hidden">

            <section
                className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)] transition-[grid-template-columns] duration-200 max-[1100px]:grid-cols-1 max-[1100px]:h-auto max-[1100px]:auto-rows-[minmax(280px,auto)] max-[560px]:px-4 max-[560px]:pt-4.5 max-[560px]:pb-7 max-[560px]:gap-3.5"
                style={{ gridTemplateColumns: "480px 1fr" }}
            >
                <aside className="flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px] h-full">
                    <div className="flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line">
                        <div className="flex min-w-0 flex-1 flex-col gap-1.25">
                            <div className="flex items-center gap-2">
                                <button type="button" className="flex shrink-0 cursor-pointer items-center justify-center w-7.5 h-7.5 rounded-lg border border-line bg-paper text-ink-soft text-xl leading-none transition-[background,color,border-color] duration-150 hover:bg-canvas hover:text-ink hover:border-mint-deep transform-none font-bold"
                                    onClick={handleGoBack}>←
                                </button>
                                <h2 className="m-0 font-bold text-[19px] text-ink tracking-[-0.01em]">시리즈 목록</h2>
                            </div>
                            {currentStudy && (
                                <div className="text-[15px] font-semibold text-slate-700 mt-1 mb-0.5 flex items-center gap-2">
                                    <span className={`${modalityBadgeClass} ${modalityColors[currentStudy.modality.toLowerCase()] || "bg-slate"}`}>{currentStudy.modality}</span>
                                    <span>{currentStudy.description}</span>
                                    <span className="text-sm text-slate-500 font-normal">| {currentStudy.datetime.replace("T", " ").split("Z")[0]}</span>
                                </div>
                            )}
                            <span className="text-left font-medium text-[13.5px] text-ink-soft leading-[1.4]">{currentPatient["patient-name"]} · {currentPatient["patient-birth"]}</span>
                        </div>
                    </div>

                    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                        <div className="gap-2.5 shrink-0 tracking-[0.02em] bg-canvas border-b border-line max-[560px]:hidden flex items-center px-4 py-3 text-[13px] font-bold text-slate-500">
                            <span className="w-16">시리즈</span>
                            <span className="flex-1">촬영 일시</span>
                            <span className="w-16 text-center">부위</span>
                            <span className="w-16 text-right">영상 수</span>
                        </div>

                        <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                            {seriesList.length > 0 ? (
                                seriesList.map((ser) => (
                                    <li key={ser.id}>
                                        <button
                                            type="button"
                                            className={`study-row gap-2.5 w-full cursor-pointer text-left p-3.5 border-[1.5px] rounded-xl bg-transparent font-[inherit] text-sm text-ink transition-[background,border-color] duration-150 hover:bg-canvas flex items-center px-4 py-3 ${
                                                ser.id === selectedSeriesId ? "bg-[rgba(76,255,157,0.14)] border-mint-deep" : "border-transparent"
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

                <section className="flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px] relative h-full">
                    <div className="flex items-center justify-center p-4 shrink-0">
                        <h2 className="text-4xl font-bold text-slate-600 tracking-wider">DICOM VIEWER</h2>
                    </div>

                    <div className="flex flex-row justify-between items-start flex-1 p-10 px-5 overflow-hidden min-h-0 relative gap-5.5" onWheel={handleViewerWheel}>
                        <div className="thumbnail-bar flex flex-col gap-3 w-32 shrink-0 overflow-y-auto overflow-x-hidden pr-1 max-h-full [&::-webkit-scrollbar]:hidden [-ms-overflow-style:none] scrollbar-none" onWheel={(e) => e.stopPropagation()}>
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
                        <button type="button" className="btn btn-medium shadow-md">AI 판독</button>
                    </div>
                </section>
            </section>
        </div>
    );
}