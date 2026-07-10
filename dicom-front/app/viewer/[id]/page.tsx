"use client";

import {useEffect, useState} from "react";
import {useParams, useRouter} from "next/navigation";
import DicomViewer from "@/app/components/dicom-viewer/DicomViewer";
import {RoleGuard} from "@/app/components/auth/RouteAccess";
import {medicalApiFetch} from "../../api/ApiFetch";

type StudyDto = {
    "study-key": number;
    description: string;
    datetime: string;
    "series-num": number;

    "allow-research": boolean;
    hidden: boolean;
    patient: {
        name: string;
        birth: string;
    } | null;
};

type SeriesDto = {
    "series-key": number;
    "series-index": number;
    datetime: string | null;
    "series-num": number;
    bodypart: string;

    description: string;
    hidden: boolean;
};

type InstanceInfo = {
    "instance-id": string;
    "number-of-frames": number;
};

// 시리즈 내 인스턴스들을 뷰어에 넘길 클립단위로 묶은 결과.
// 클립 하나 = 뷰어에서 연속으로 스크롤/재생되는 이미지 묶음 하나.
type Clip = {
    label: string;   // 클립 선택 UI에 보여줄 이름
    urls: string[];  // 이 클립에 속한 이미지 URL들 (cornerstone에 그대로 넘어감)
};

// 시리즈의 인스턴스 목록을 클립 단위로 재구성한다.
// - 프레임이 1장인 인스턴스(CT/MR 슬라이스, 일반 X-ray 등)는 서로 이어지는 하나의 연속 스택으로 묶는다.
//   시리즈 안의 단일 프레임 인스턴스들은 전부 한 줄로 스크롤된다 이거는 이전이랑 똑같은 방식이고 x-ray같은 애들만 특이 케이스

// - 프레임이 2장 이상인 인스턴스(초음파 시네 루프, 혈관조영 시네 등)는 그 자체로 독립된 클립이 된다.
//   같은 시리즈 안에 서로 다른 각도/시점의 시네이 여러 개 섞여 있어도 하나로 뭉치지 않도록 분리하기 위함.
//   ex: 시리즈 하나에 100프레임/16프레임/150프레임짜리 혈관조영 시네이 3개 섞여 있으면 클립 3개로 분리됨
/**
 * 인스턴스 목록을 클립 목록으로 재구성합니다.
 *
 * @param seriesKey - 시리즈 식별자
 * @param instances - 인스턴스 목록
 * @returns 구성된 클립 목록
 */
function buildClips(seriesKey: number, instances: InstanceInfo[]): Clip[] {
    const clips: Clip[] = [];
    let currentStack: string[] = [];

    const flushStack = () => {
        if (currentStack.length > 0) {
            clips.push({ label: `스택 (${currentStack.length}장)`, urls: currentStack });
            currentStack = [];
        }
    };

    instances.forEach((inst) => {
        const baseUrl = `/api/medical/dicom/series/${seriesKey}/instances/${inst["instance-id"]}/file`;
        const frameCount = inst["number-of-frames"] || 1;

        if (frameCount <= 1) {
            currentStack.push(baseUrl);
            return;
        }

        // 멀티프레임 인스턴스를 만나면 지금까지 쌓인 단일 프레임 스택을 먼저 끊어서 클립으로 내보내고
        // 이 인스턴스는 자기 프레임들만으로 별도의 클립을 새로 만든다.
        flushStack();
        const frameUrls = Array.from({ length: frameCount }, (_, frame) => `${baseUrl}?frame=${frame}`);
        clips.push({ label: `시네 (${frameCount}프레임)`, urls: frameUrls });
    });

    flushStack();
    return clips;
}

/**
 * 역할 기반 접근 제어가 있는 뷰어 페이지 래퍼입니다.
 * MEDICAL 계정으로의 접근을 제한합니다.
 *
 * @returns 권한이 있는 경우 뷰어 페이지
 */
export default function ViewerPage() {
    return (
        <RoleGuard allow="MEDICAL">
            <ViewerPageInner />
        </RoleGuard>
    );
}

/**
 * 길이 자르기
 * @param text 원본 설명 텍스트 (없을 수 있음)
 * @param maxLength 이 길이를 넘으면 자름 (기본 10자)
 * @returns 잘린 텍스트, 원본이 없으면 "N/A"
 */
function truncateDescription(text: string | null | undefined, maxLength = 10): string {
    if (!text) {
        return "N/A";
    }
    return text.length > maxLength ? `${text.slice(0, maxLength)}...` : text;
}

/**
 * 뷰어 페이지의 내부 컴포넌트입니다.
 * DICOM 데이터를 가져오고 뷰어를 렌더링하는 것을 처리합니다.
 *
 * @returns 뷰어 인터페이스
 */
function ViewerPageInner() {
    const params = useParams();
    const router = useRouter();
    const studyKey = Number(params?.id);

    const [study, setStudy] = useState<StudyDto | null>(null);
    const [seriesList, setSeriesList] = useState<SeriesDto[]>([]);
    const [selectedSeriesId, setSelectedSeriesId] = useState<number | null>(null);
    const [clips, setClips] = useState<Clip[]>([]); // 선택된 시리즈를 클립 단위로 나눈 목록
    const [selectedClipIndex, setSelectedClipIndex] = useState(0); // 그중 현재 뷰어에 보여줄 클립
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
                    medicalApiFetch(`/api/medical/dicom/studies/${studyKey}`, { credentials: "include" }),
                    medicalApiFetch(`/api/medical/dicom/studies/${studyKey}/series`, { credentials: "include" }),
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

    // 선택된 시리즈의 인스턴스 목록 -> 클립 목록 구성
    // 같은 파일 URL에 ?frame=N만 다르게 붙이면, cornerstone-wado-image-loader가 파일을 한 번만 받아서 그 안에서 N번째 프레임만 잘라 보여준다 — 이 부분은 buildClips 안에서 그대로 처리됨
    useEffect(() => {
        if (!selectedSeriesId) {
            setClips([]);
            return;
        }

        async function loadInstances() {
            try {
                const instances: InstanceInfo[] = await medicalApiFetch(
                    `/api/medical/dicom/series/${selectedSeriesId}/instances`,
                    { credentials: "include" }
                );
                setClips(buildClips(selectedSeriesId as number, instances));
                setSelectedClipIndex(0); // 시리즈를 바꿨으니 항상 첫 번째 클립부터 보여준다
            } catch (e) {
                setError(e instanceof Error ? e.message : "이미지 목록을 불러오지 못했습니다.");
            }
        }

        loadInstances();
    }, [selectedSeriesId]);

    // 실제 뷰어(DicomViewer)에 넘기는 배열은 현재 선택된 이미지의 URL들뿐이다.
    const dicomUrls = clips[selectedClipIndex]?.urls ?? [];

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
                                {/* workspace로 그냥 이동(<Link href="/workspace">)하면 검색/선택했던 상태가
                                    다 초기화된 채로 이동하게 된다. router.back()은 브라우저 뒤로가기와
                                    똑같이 동작해서, workspace 페이지가 URL 쿼리로 들고 있던 선택 상태를
                                    그대로 복원한다. */}
                                <button
                                    type="button"
                                    onClick={() => router.back()}
                                    className="flex shrink-0 cursor-pointer items-center justify-center w-7.5 h-7.5 rounded-lg border border-line bg-paper text-ink-soft text-xl leading-none transition-[background,color,border-color] duration-150 hover:bg-canvas hover:text-ink hover:border-mint-deep transform-none font-bold no-underline"
                                >
                                    ←
                                </button>
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
                                    {study.patient?.name || "N/A"} · {study.patient?.birth ? study.patient.birth.split("T")[0] : "N/A"}
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
                                            <span className="w-16" title={ser.description || "N/A"}>{truncateDescription(ser.description)}</span>
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
                    {/* 클립 선택 바 — 시리즈 안에 클립이 2개 이상일 때만 노출된다.
                        (CT/MR처럼 클립이 1개(=하나의 연속 스택)뿐인 일반적인 경우엔 기존과 동일하게 아무 UI도 안 보임) */}
                    {clips.length > 1 && (
                        <div className="flex shrink-0 gap-2 mb-3 overflow-x-auto">
                            {clips.map((clip, idx) => (
                                <button
                                    key={idx}
                                    type="button"
                                    onClick={() => setSelectedClipIndex(idx)}
                                    className={`shrink-0 px-3 py-1.5 rounded-lg text-xs font-semibold border transition-colors duration-150 cursor-pointer ${
                                        idx === selectedClipIndex
                                            ? "bg-[rgba(76,255,157,0.14)] border-mint-deep text-ink"
                                            : "border-line text-ink-soft hover:bg-canvas"
                                    }`}
                                >
                                    {clip.label}
                                </button>
                            ))}
                        </div>
                    )}
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
