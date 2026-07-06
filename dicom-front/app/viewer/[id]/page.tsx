"use client";

import { useState, useEffect } from "react";
import Link from "next/link";
import { useParams } from "next/navigation";
// import { patients, studies, series as allSeries } from "@/mock-data";
import DicomViewer from "@/app/components/dicom-viewer/DicomViewer";
import { apiFetch } from "@/app/api/apiFetch";

type StudyDto = {
    "study-key": number;
    description: string;
    datetime: string;
    "series-num": number;
    // "images-num": number;
    "allow-research": boolean;
    hidden: boolean;
    "patient-name": string;
    "patient-birth": string;
};

type SeriesDto = {
    "series-key": number;
    "series-index": number;
    datetime: string | null;
    "series-num": number;
    bodypart: string;
    // "images-num": number;
    SeriesDescription: string;
    hidden: boolean;
};

export default function ViewerPage() {
    const params = useParams();
    const studyKey = Number(params?.id);

    const [study, setStudy] = useState<StudyDto | null>(null);
    const [seriesList, setSeriesList] = useState<SeriesDto[]>([]);
    const [selectedSeriesId, setSelectedSeriesId] = useState<number | null>(null);
    const [dicomUrls, setDicomUrls] = useState<string[]>([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState<string | null>(null);

    // 1. Study 상세 + Series 목록 로드
    useEffect(() => {
        if (!studyKey || Number.isNaN(studyKey)) {
            setError(`잘못된 접근입니다. (studyKey: ${params?.id})`);
            setLoading(false);
            return;
        }

        async function loadStudyAndSeries() {
            try {
                setLoading(true);
                setError(null);

                const [studyData, seriesData]: [StudyDto, SeriesDto[]] = await Promise.all([
                    apiFetch(`/api/dicom/studies/${studyKey}`, { credentials: "include" }),
                    apiFetch(`/api/dicom/studies/${studyKey}/series`, { credentials: "include" }),
                ]);

                setStudy(studyData);
                setSeriesList(seriesData);
                setSelectedSeriesId(seriesData.length > 0 ? seriesData[0]["series-key"] : null);
            } catch (e) {
                setError(e instanceof Error ? e.message : "스터디 정보를 불러오지 못했습니다.");
            } finally {
                setLoading(false);
            }
        }

        loadStudyAndSeries();
    }, [studyKey]);

    // 선택된 시리즈의 인스턴스 목록 -> 뷰어용 URL 배열 구성
    useEffect(() => {
        if (!selectedSeriesId) {
            setDicomUrls([]);
            return;
        }

        async function loadInstances() {
            try {
                const instanceIds: string[] = await apiFetch(
                    `/api/dicom/series/${selectedSeriesId}/instances`,
                    { credentials: "include" }
                );

                // DicomViewer가 fetch할 raw DICOM 스트리밍 URL 목록.
                // apiFetch를 거치지 않고 상대경로 문자열만 조립 -> next.config.ts의 rewrites가
                // /api/* 요청을 자동으로 백엔드(8080)로 프록시해줌.
                const urls = instanceIds.map(
                    (id) => `/api/dicom/series/${selectedSeriesId}/instances/${id}/file`
                );
                setDicomUrls(urls);
            } catch (e) {
                setError(e instanceof Error ? e.message : "이미지 목록을 불러오지 못했습니다.");
            }
        }

        loadInstances();
    }, [selectedSeriesId]);

    const handleSelectSeries = (id: number) => {
        setSelectedSeriesId(id);
    };

    if (loading) {
        return (
            <div className="page flex h-screen items-center justify-center">
                <p className="text-slate-500 text-sm">불러오는 중...</p>
            </div>
        );
    }

    if (error) {
        return (
            <div className="page flex h-screen items-center justify-center">
                <p className="text-red-500 text-sm">{error}</p>
            </div>
        );
    }

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
                            {study && (
                                <div className="text-base font-semibold text-slate-700 mt-1 mb-0.5 flex items-center gap-2">
                                    <span className="text-sm text-slate-500 font-normal">
                                        {study.description || "N/A"} · {study.datetime ? study.datetime.replace("T", " ").split("Z")[0] : "N/A"}
                                    </span>
                                </div>
                            )}
                            {study && (
                                <span className="text-left font-medium text-sm text-ink-soft leading-[1.4]">
                                    {study["patient-name"] || "N/A"} · {study["patient-birth"] ? study["patient-birth"].split("T")[0] : "N/A"}
                                </span>
                            )}
                        </div>
                    </div>

                    <div className="flex min-h-0 flex-1 flex-col overflow-y-auto">
                        <div className="gap-2.5 shrink-0 tracking-[0.02em] bg-canvas border-b border-line max-[560px]:hidden flex items-center px-4 py-3 text-sm font-bold text-slate-500">
                            <span className="w-16">시리즈</span>
                            <span className="w-16">검사 설명</span>
                            <span className="flex-1 text-center">부위</span>
                            {/*<span className="w-16 text-right">영상 수</span>*/}
                        </div>

                        <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                            {seriesList.length > 0 ? (
                                seriesList.map((ser) => (
                                    <li key={ser["series-key"]}>
                                        <button
                                            type="button"
                                            onClick={() => handleSelectSeries(ser["series-key"])}
                                            className={`study-row gap-2.5 w-full cursor-pointer text-left p-3.5 border-[1.5px] rounded-xl font-[inherit] text-sm text-ink transition-[background,border-color] duration-150 flex items-center px-4 py-3 ${
                                                ser["series-key"] === selectedSeriesId ? "bg-[rgba(76,255,157,0.14)] border-mint-deep" : "border-transparent hover:bg-canvas"
                                            }`}
                                        >
                                            <span className="w-16 font-mono text-[#14b876] font-bold">#{ser["series-num"] ?? "N/A"}</span>
                                            <span className="w-16 truncate">{ser.SeriesDescription || "N/A"}</span>
                                            <span className="flex-1 text-center pl-2 truncate">{ser.bodypart || "N/A"}</span>
                                            {/*<span className="w-16 text-right">{ser["images-num"]}장</span>*/}
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
                        {dicomUrls.length > 0 ? (
                            <DicomViewer dicomUrls={dicomUrls} />
                        ) : (
                            <div className="text-slate-400 text-sm">시리즈를 선택하면 이미지가 표시됩니다.</div>
                        )}
                    </div>
                </section>
            </section>
        </div>
    );
}