"use client";

import { useState, useMemo, useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
import { patients, studies, series as allSeries } from "@/mock-data";
import DicomViewer from "@/app/components/dicom-viewer/DicomViewer";

export default function ViewerPage() {
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
            }));
        }
        return [];
    }, [currentStudy]);

    const [selectedSeriesId, setSelectedSeriesId] = useState<string | null>(null);

    // 초기 선택 로직
    useEffect(() => {
        if (seriesList.length > 0 && !selectedSeriesId) {
            setSelectedSeriesId(seriesList[0].id);
        }
    }, [seriesList, selectedSeriesId]);

    const handleSelectSeries = (id: string) => {
        setSelectedSeriesId(id);
    };

    // ── Tailwind 스타일 변수 ──
    const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-10.5 px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
    const modalityColors: Record<string, string> = {
        ct: "bg-[#2563eb]",
        mr: "bg-[#7c3aed]",
        cr: "bg-[#0e7490]",
        us: "bg-[#c2410c]",
        pt: "bg-[#be185d]",
    };

    // 샘플 1번부터 4번 파일 목록 (실제 연동 시에는 API에서 받아온 해당 시리즈의 인스턴스 URL 배열로 대체)
    const dicomUrls = [
      "/dicom/CT_brain_001.dcm",
      "/dicom/CT_brain_002.dcm",
      "/dicom/CT_brain_003.dcm",
      "/dicom/CT_brain_004.dcm"
    ];

    return (
        <div className="page flex h-screen flex-col overflow-hidden">
            <section
                className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)] transition-[grid-template-columns] duration-200 max-[1100px]:grid-cols-1 max-[1100px]:h-auto max-[1100px]:auto-rows-[minmax(280px,auto)] max-[560px]:px-4 max-[560px]:pt-4.5 max-[560px]:pb-7 max-[560px]:gap-3.5"
                style={{ gridTemplateColumns: "360px 1fr" }}
            >
                {/* 좌측 시리즈 목록 패널 */}
                <aside className="flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px] h-full">
                    <div className="flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line">
                        <div className="flex min-w-0 flex-1 flex-col gap-1.25">
                            <div className="flex items-center gap-2">
                                {/* useRouter() 대신 Link 태그로 변경하여 서버 컴포넌트로 만듭니다. */}
                                <Link href="/workspace" className="flex shrink-0 cursor-pointer items-center justify-center w-7.5 h-7.5 rounded-lg border border-line bg-paper text-ink-soft text-xl leading-none transition-[background,color,border-color] duration-150 hover:bg-canvas hover:text-ink hover:border-mint-deep transform-none font-bold no-underline">
                                    ←
                                </Link>
                                <h2 className="m-0 font-bold text-xl text-ink tracking-[-0.01em]">시리즈 목록</h2>
                            </div>
                            {currentStudy && (
                                <div className="text-base font-semibold text-slate-700 mt-1 mb-0.5 flex items-center gap-2">
                                    <span className={`${modalityBadgeClass} ${modalityColors[currentStudy.modality.toLowerCase()] || "bg-slate"}`}>{currentStudy.modality}</span>
                                    <span className="text-sm text-slate-500 font-normal">| {currentStudy.datetime.replace("T", " ").split("Z")[0]}</span>
                                </div>
                            )}
                            <span className="text-left font-medium text-sm text-ink-soft leading-[1.4]">{currentPatient["patient-name"]} · {currentPatient["patient-birth"]}</span>
                        </div>
                    </div>

                    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                        <div className="gap-2.5 shrink-0 tracking-[0.02em] bg-canvas border-b border-line max-[560px]:hidden flex items-center px-4 py-3 text-sm font-bold text-slate-500">
                            <span className="w-16">시리즈</span>
                            <span className="flex-1 text-center">부위</span>
                            <span className="w-16 text-right">영상 수</span>
                        </div>

                        <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                            {seriesList.length > 0 ? (
                                seriesList.map((ser) => (
                                    <li key={ser.id}>
                                        <button
                                            type="button"
                                            onClick={() => handleSelectSeries(ser.id)}
                                            className={`study-row gap-2.5 w-full cursor-pointer text-left p-3.5 border-[1.5px] rounded-xl font-[inherit] text-sm text-ink transition-[background,border-color] duration-150 flex items-center px-4 py-3 ${
                                                ser.id === selectedSeriesId ? "bg-[rgba(76,255,157,0.14)] border-mint-deep" : "border-transparent hover:bg-canvas"
                                            }`}
                                        >
                                            <span className="w-16 font-mono text-[#14b876] font-bold">#{ser.seriesNumber}</span>
                                            <span className="flex-1 text-center pl-2 truncate">{ser.bodyPart}</span>
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

                {/* 우측 메인 뷰어 패널 */}
                <section className="flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px] relative h-full p-4">
                    <div className="main-viewer-zone flex flex-1 justify-center items-center h-full w-full relative min-h-0">
                        {/* selectedSeriesId에 따라 다른 URL 배열을 넘겨줄 수 있습니다. 현재는 데모용 고정 배열을 넘깁니다. */}
                        <DicomViewer dicomUrls={dicomUrls}>
                            <button type="button" className="btn btn-small shadow-md">AI 판독</button>
                        </DicomViewer>
                    </div>
                </section>
            </section>
        </div>
    );
}