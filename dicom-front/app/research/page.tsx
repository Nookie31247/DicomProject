"use client"

import { useState, useEffect } from "react";
import { ChevronRight, ChevronDown, Download, ShieldCheck, FileText, AlertCircle, Calendar } from "lucide-react";
import dicomApi from "@/app/api/dicomApi";

interface StudyItem {
    "study-key": number;
    description: string;
    datetime: string;
    "series-num": number;
    "images-num": number;
    "allow-research": boolean;
    hidden: boolean;
    "patient-name": string;
    "patient-birth": string;
}

interface SeriesItem {
    "series-key": number;
    "series-index": number;
    datetime: string;
    "series-num": number;
    bodypart: string;
    hidden: boolean;
}

export default function ResearchDataPage() {
    //상태 : 예시 데이터
    const [studies, setStudies] = useState<StudyItem[]>([]);
    const [loading, setLoading] = useState(true);
    const [selectedStudies, setSelectedStudies] = useState<any[]>([]); // 선택된 연구 자료들
    const [dateRange, setDateRange] = useState({ start: "", end: "" });
    const [anonymizationOptions, setAnonymizationOptions] = useState({
        removeName: true,
        removeId: true,
        removeBirth: true
    });

    // 스터디를 펼쳤을 때만 시리즈를 불러오고, 한 번 불러온 건 캐시해서 재요청 안 함
    // (연구 대상 스터디가 많을 수 있는데 시리즈까지 전부 미리 불러오면 느려지고 불필요한 트래픽 발생)
    const [expanded, setExpanded] = useState<Set<number>>(new Set());
    const [seriesByStudy, setSeriesByStudy] = useState<Record<number, SeriesItem[]>>({});
    const [loadingSeries, setLoadingSeries] = useState<Set<number>>(new Set());

    // 체크된 항목을 스터디/시리즈 구분 없이 하나의 Set으로 관리
    // "study:123", "series:456" 처럼 접두어로 두 종류의 id 네임스페이스를 구분
    const [checked, setChecked] = useState<Set<string>>(new Set());

    // 서버가 zip 하나로 묶는 동안(체크 항목이 많으면 수 초 걸릴 수 있음) 버튼을 잠그기 위한 상태
    const [downloading, setDownloading] = useState(false);

    // 연구원 계정 여부. 연구원은 원칙상 익명화된 데이터만 받아야 하는데
    // 익명화 기능(#11)이 아직 없어서, 그게 붙기 전까지는 이 페이지에서
    // 연구원에게 "익명화 준비 중" 배지를 보여주고 다운로드 버튼을 잠근다.
    // (실제 차단은 백엔드 blockResearcherDownload가 하고, 이건 UX용 안내일 뿐)
    const [isResearcher, setIsResearcher] = useState(false);

    // 연구 허용 스터디 목록 조회
    useEffect(() => {
        setIsResearcher(localStorage.getItem("userType") === "RESEARCHER");
        dicomApi.getResearchStudies()
            .then((data: StudyItem[]) => setStudies(data))
            .finally(() => setLoading(false));
        // eslint-disable-next-line react-hooks/exhaustive-deps
    }, []);

    // 스터디 행 클릭 시 펼치기, 접기 + 처음 펼칠 때만 시리즈 fetch
    const toggleExpand = async (studyKey: number) => {
        const next = new Set(expanded);
        if (next.has(studyKey)) {
            next.delete(studyKey);
            setExpanded(next);
            return;
        }
        next.add(studyKey);
        setExpanded(next);

        if (!seriesByStudy[studyKey]) {
            setLoadingSeries(prev => new Set(prev).add(studyKey));
            const data: SeriesItem[] = await dicomApi.getSeriesForResearch(studyKey);
            setSeriesByStudy(prev => ({ ...prev, [studyKey]: data }));
            setLoadingSeries(prev => {
                const s = new Set(prev);
                s.delete(studyKey);
                return s;
            });
        }
    };

    // 스터디 체크박스: 스터디 자체 + 이미 로드된 하위 시리즈까지 한 번에 체크/해제
    // 시리즈가 아직 로드 안 된 상태로 체크하면 다운로드 할때 이미 studyKey 자체로 study zip을 받으므로 굳이 시리즈를 미리 불러올 필요는 없음
    const toggleStudy = (studyKey: number) => {
        const key = `study:${studyKey}`;
        const next = new Set(checked);
        const willCheck = !next.has(key);

        if (willCheck) next.add(key); else next.delete(key);

        const loadedSeries = seriesByStudy[studyKey] ?? [];
        for (const s of loadedSeries) {
            const sKey = `series:${s["series-key"]}`;
            if (willCheck) next.add(sKey); else next.delete(sKey);
        }
        setChecked(next);
    };

    const toggleSeries = (seriesKey: number) => {
        const key = `series:${seriesKey}`;
        const next = new Set(checked);
        if (next.has(key)) next.delete(key); else next.add(key);
        setChecked(next);
    };

    // 체크된 study/series를 zip 하나로 묶어서 한 번만 다운로드함
    // 백엔드의 /api/dicom/download/batch가 선택된 항목 전체를 zip 하나로 합쳐 다운로드 자체를 1번만 트리거하도록 바꿈
    // 부모 스터디가 체크돼 있는 시리즈는 어차피 study zip에 포함되므로 중복 방지를 위해 제외함
    const startDownload = async () => {
        const checkedStudyKeys = new Set(
            Array.from(checked)
                .filter((item) => item.startsWith("study:"))
                .map((item) => Number(item.split(":")[1]))
        );

        // series-key -> 그 시리즈가 속한 study-key 역방향 조회 맵
        const seriesToStudy = new Map<number, number>();
        Object.entries(seriesByStudy).forEach(([studyKey, seriesList]) => {
            seriesList.forEach((s) => seriesToStudy.set(s["series-key"], Number(studyKey)));
        });

        const studyKeys: number[] = [];
        const seriesKeys: number[] = [];

        checked.forEach((item) => {
            const [type, idStr] = item.split(":");
            const id = Number(idStr);

            if (type === "study") {
                studyKeys.push(id);
                return;
            }
            // 부모 스터디가 이미 포함 대상이면 건너뛴다 (studies.zip에 이미 들어있음)
            if (!checkedStudyKeys.has(seriesToStudy.get(id) ?? -1)) {
                seriesKeys.push(id);
            }
        });

        if (studyKeys.length === 0 && seriesKeys.length === 0) return;

        setDownloading(true);
        try {
            const blob = await dicomApi.downloadBatch(studyKeys, seriesKeys);
            const url = URL.createObjectURL(blob);
            const a = document.createElement("a");
            a.href = url;
            a.download = "download.zip";
            a.click();
            URL.revokeObjectURL(url);
        } catch (e) {
            alert(e instanceof Error ? e.message : "다운로드 중 문제가 발생했습니다.");
        } finally {
            setDownloading(false);
        }
    };

    // 날짜 필터링 로직 검사일자 기준, 대상은 실제 조회된 studies
    const filteredStudies = studies.filter(study => {
        if (!dateRange.start || !dateRange.end) return true;
        return study.datetime >= dateRange.start && study.datetime <= dateRange.end;
    });

    const gridCols = "40px 1fr 1fr 1fr 100px";

    return (
        <div className="page p-8 max-w-[1200px] mx-auto">
            {/* 상단 헤더 영역에 검색 필터 통합 */}
            <div className="flex justify-between items-end mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-ink">연구 자료 다운로드</h1>
                    <p className="text-ink-soft mt-1 mr-5">익명화된 연구 자료를 선택해 다운로드할 수 있습니다.</p>
                </div>

                {/* 날짜 검색 영역 */}
                <div className="flex items-center gap-2 bg-paper border border-line p-2 rounded-xl">
                    <Calendar size={18} className="text-ink-soft ml-2" />
                    <input type="date" className="bg-transparent text-sm p-1 outline-none"
                           onChange={(e) => setDateRange(s => ({...s, start: e.target.value}))} />
                    <span className="text-ink-soft">~</span>
                    <input type="date" className="bg-transparent text-sm p-1 outline-none"
                           onChange={(e) => setDateRange(s => ({...s, end: e.target.value}))} />
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 왼쪽: 선택된 리스트 (기존 Workspace 스타일 활용) */}
                <div className="lg:col-span-2 bg-paper border border-line rounded-[20px] p-6">
                    <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                        <FileText size={20} /> 연구 활용 허용 자료
                        {isResearcher && (
                            <span className="text-[11px] font-bold text-amber-700 bg-amber-100 px-2 py-0.5 rounded-full">
                                익명화 준비 중
                            </span>
                        )}
                    </h2>

                    <div className="grid font-bold text-xs text-ink-soft bg-canvas p-3 rounded-lg mb-2" style={{ gridTemplateColumns: gridCols }}>
                        <span></span><span>유형</span><span>검사 설명</span><span>검사 일자</span><span>상태</span>
                    </div>

                    <ul className="space-y-1 max-h-[500px] overflow-y-auto">
                        {loading && <li className="p-8 text-center text-ink-soft text-sm">불러오는 중...</li>}
                        {/* 목록 아이템 매핑 영역 */}
                        {!loading && filteredStudies.length === 0 && (
                            <li className="p-8 text-center text-ink-soft text-sm">연구 활용이 허용된 자료가 없습니다.</li>
                        )}

                        {filteredStudies.map((study) => {
                            const studyKey = study["study-key"];
                            const isOpen = expanded.has(studyKey);
                            const isChecked = checked.has(`study:${studyKey}`);
                            const series = seriesByStudy[studyKey];

                            return (
                                <li key={studyKey}>
                                    <div className="grid items-center p-2 rounded-lg hover:bg-canvas" style={{ gridTemplateColumns: gridCols }}>
                                        <input type="checkbox" checked={isChecked} onChange={() => toggleStudy(studyKey)} />
                                        {/* 유형 칸에 펼치기 화살표 + "스터디" 라벨을 같이 표시 */}
                                        <button onClick={() => toggleExpand(studyKey)} className="flex items-center gap-1 text-xs font-semibold text-ink-soft">
                                            {isOpen ? <ChevronDown size={14} /> : <ChevronRight size={14} />} 스터디
                                        </button>
                                        <span className="text-sm">{study.description ?? "설명 없음"}</span>
                                        <span className="text-xs text-ink-soft">{study.datetime?.slice(0, 10)}</span>
                                        <span className="text-xs text-mint-deep font-semibold">연구 허용</span>
                                    </div>

                                    {/* 펼쳐진 상태에서만 시리즈 목록 렌더링 (지연 로딩) */}
                                    {isOpen && (
                                        <div className="pl-10 pb-2">
                                            {loadingSeries.has(studyKey) && (
                                                <div className="text-xs text-ink-soft p-2">시리즈 불러오는 중...</div>
                                            )}
                                            {series?.map((s) => (
                                                <div key={s["series-key"]}
                                                     className="grid items-center p-1.5 text-xs text-ink-soft"
                                                     style={{ gridTemplateColumns: "24px 56px 2fr 1fr 1fr 100px" }}>
                                                    <input
                                                        type="checkbox"
                                                        checked={checked.has(`series:${s["series-key"]}`)}
                                                        onChange={() => toggleSeries(s["series-key"])}
                                                    />
                                                    <span className="font-medium">시리즈</span>
                                                    <span>{s.bodypart ?? "N/A"} · 시리즈 {s["series-num"]}</span>
                                                    <span>{s.datetime?.slice(0, 10)}</span>
                                                    <span></span>
                                                    <span>{s.hidden ? "숨김" : "표시"}</span>
                                                </div>
                                            ))}
                                        </div>
                                    )}
                                </li>
                            );
                        })}
                    </ul>
                </div>

                {/* 오른쪽: 익명화 설정 및 액션 패널 */}
                <div className="bg-paper border border-line rounded-[20px] p-6 h-fit">
                    <h2 className="text-lg font-bold mb-6 flex items-center gap-2">
                        <ShieldCheck size={20} /> 익명화 설정
                    </h2>

                    <div className="grid grid-cols-2 gap-3 text-xs mb-4">
                        {[
                            { label: "환자 정보", desc: "익명으로 데이터 치환" },
                            { label: "검사 정보", desc: "익명화 및 데이터의 일반화" },
                            { label: "UID 속 장비번호·날짜", desc: "시퀀스 내부까지 순회" },
                            { label: "파일 정보", desc: "정보 마스킹 처리 및 규칙 변경" }
                        ].map((rule) => (
                            <div key={rule.label} className="bg-paper p-3 rounded-lg border border-line">
                                <div className="font-bold text-ink mb-0.5">{rule.label}</div>
                                <div className="text-ink-soft">{rule.desc}</div>
                            </div>
                        ))}
                    </div>

                    <div className="p-4 bg-canvas rounded-xl mb-6 text-xs text-ink-soft leading-relaxed flex gap-2">
                        <AlertCircle size={16} className="shrink-0 text-mint-deep" />
                        익명화된 자료는 DICOM 헤더 내 개인식별정보가 제거된 후 다운로드됩니다.
                    </div>

                    {/* 연구원 계정 전용 안내: 익명화 기능이 붙기 전까지는 다운로드를 못 받는 이유를 설명 */}
                    {isResearcher && (
                        <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl mb-4 text-xs text-amber-800 leading-relaxed">
                            연구원 계정은 개인정보 보호를 위해 익명화 처리된 자료만 다운로드할 수 있습니다.
                            익명화 기능은 현재 준비 중이며, 완료되는 대로 이 페이지에서 다운로드가 가능해집니다.
                        </div>
                    )}

                    <div className="text-xs text-ink-soft mb-4">선택된 항목: {checked.size}개</div>

                    <button
                        onClick={() => { void startDownload(); }}
                        disabled={checked.size === 0 || downloading || isResearcher}
                        className="w-full btn btn-big bg-mint text-slate font-bold flex items-center justify-center gap-2 disabled:opacity-40 disabled:cursor-not-allowed"
                    >
                        <Download size={20} /> {isResearcher ? "익명화 준비 중" : downloading ? "압축하는 중..." : "다운로드 시작"}
                    </button>
                </div>
            </div>
        </div>
    );
}