"use client";

import { useState } from "react";
import { useRouter } from "next/navigation";
import { patients, studies } from "@/mock-data";

export default function WorkspaceDashboardPage() {
    const router = useRouter();

    // ── 환자(Patient) 관련 상태 ──
    const [selectedPatientId, setSelectedPatientId] = useState(patients[0]?.["patient-id"]);
    const [checkedPatientIds, setCheckedPatientIds] = useState<Set<string>>(new Set());
    const [showHiddenPatients, setShowHiddenPatients] = useState(false);

    // ── 검사(Study/DICOM) 관련 상태 ──
    const [checkedStudyIds, setCheckedStudyIds] = useState<Set<string>>(new Set());
    const [showHiddenStudies, setShowHiddenStudies] = useState(false);

    // ==========================================
    // 1. 환자 목록 컨트롤 로직
    // ==========================================
    const handleSelectPatient = (id: string) => {
        setSelectedPatientId(id);
        setCheckedStudyIds(new Set()); // 환자 변경 시 검사 선택 초기화
    };

    const togglePatientCheck = (id: string) => {
        setCheckedPatientIds((prev) => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const executePatientAction = (logMessage: string) => {
        console.log(logMessage, Array.from(checkedPatientIds));
        setCheckedPatientIds(new Set());
    };

    const [searchKeyword, setSearchKeyword] = useState("");
    const [startDate, setStartDate] = useState("");
    const [endDate, setEndDate] = useState("");
    const [appliedFilters, setAppliedFilters] = useState({ keyword: "", start: "", end: "" });

    const handleSearch = () => {
        setAppliedFilters({ keyword: searchKeyword, start: startDate, end: endDate });
    };

    const toggleShowHiddenPatients = () => {
        setShowHiddenPatients((prev) => !prev);
        setCheckedPatientIds(new Set());
    };

    const displayedPatients = patients.filter(p => {
        if (p.hidden !== showHiddenPatients) return false;

        if (appliedFilters.keyword) {
            const kw = appliedFilters.keyword.toLowerCase();
            if (!p["patient-name"].toLowerCase().includes(kw) && !p["patient-id"].toLowerCase().includes(kw)) {
                return false;
            }
        }

        if (appliedFilters.start) {
            if (!p["latest-study-datetime"]) return false;
            const studyDate = p["latest-study-datetime"].split("T")[0];
            if (studyDate < appliedFilters.start) return false;
        }

        if (appliedFilters.end) {
            if (!p["latest-study-datetime"]) return false;
            const studyDate = p["latest-study-datetime"].split("T")[0];
            if (studyDate > appliedFilters.end) return false;
        }

        return true;
    });
    const selectedPatient = patients.find((p) => p["patient-id"] === selectedPatientId) ?? null;

    // ==========================================
    // 2. 검사(DICOM) 목록 컨트롤 로직
    // ==========================================
    const toggleStudyCheck = (id: string) => {
        setCheckedStudyIds((prev) => {
            const next = new Set(prev);
            next.has(id) ? next.delete(id) : next.add(id);
            return next;
        });
    };

    const executeStudyAction = (logMessage: string) => {
        console.log(logMessage, Array.from(checkedStudyIds));
        setCheckedStudyIds(new Set());
    };

    const toggleShowHiddenStudies = () => {
        setShowHiddenStudies((prev) => !prev);
        setCheckedStudyIds(new Set());
    };

    const displayedStudies = !selectedPatient
        ? []
        : studies.filter(s => s["patient-id"] === selectedPatientId && s.hidden === showHiddenStudies);

    // ==========================================
    // 기타 헬퍼 및 스타일
    // ==========================================
    const sexLabel = (s: "M" | "F") => (s === "M" ? "남" : "여");

    // ── Tailwind 스타일 변수 (globals.css에서 이관) ──
    const wsPanelClass = "flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px]";
    const wsPanelHeadClass = "flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line";
    const wsHeadLeftClass = "flex min-w-0 flex-1 flex-col gap-1.25";
    const wsPanelTitleClass = "m-0 font-bold text-xl text-ink tracking-[-0.01em]";
    const wsCountClass = "font-semibold text-sm text-mint-deep";
    const wsSubLabelClass = "text-left font-medium text-sm text-ink-soft leading-[1.4]";
    const patientRowBase = "flex w-full cursor-pointer items-center text-left gap-3 p-3 border-[1.5px] rounded-[14px] bg-transparent font-[inherit] transition-[background,border-color] duration-150 hover:bg-canvas";
    const patientRowActive = "bg-[rgba(76,255,157,0.14)] border-mint-deep";
    const patientRowInactive = "border-transparent";
    const patientAvatarClass = "flex shrink-0 items-center justify-center rounded-full font-bold w-9.5 h-9.5 text-base text-slate bg-mint";
    const patientMainClass = "flex min-w-0 flex-1 flex-col gap-[3px]";
    const patientNameClass = "font-semibold text-base text-ink";
    const patientSubClass = "overflow-hidden whitespace-nowrap text-ellipsis text-xs text-ink-soft";
    const patientBadgeBase = "flex items-center justify-center font-bold shrink-0 min-w-6 h-6 px-1.5 rounded-xl text-sm";
    const patientBadgeDefault = "bg-canvas text-ink-soft";
    const patientBadgeActive = "bg-mint-deep text-paper";
    const colDescClass = "col-desc overflow-hidden whitespace-nowrap font-semibold text-ellipsis";
    const colDateClass = "col-date text-ink-soft";
    const colSeriesClass = "col-series text-right tabular-nums text-ink-soft";
    const colImagesClass = "col-images text-right tabular-nums text-ink-soft";
    const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-10.5 px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
    const modalityColors: Record<string, string> = {
        ct: "bg-[#2563eb]",
        mr: "bg-[#7c3aed]",
        cr: "bg-[#0e7490]",
        us: "bg-[#c2410c]",
        pt: "bg-[#be185d]",
    };

    // ★ 수정 포인트 1: 기존 "30px 84px 1.6fr 1fr 1fr 70px 70px 100px" 에서 검사 부위에 해당하는 1fr 제거
    const studyGridColumns = "30px 84px 1.6fr 1fr 70px 70px 100px";

    return (
        <div className="page">
            {/* ───────────── Workspace ───────────── */}
            <section className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)] transition-[grid-template-columns] duration-200 max-[1100px]:grid-cols-1 max-[1100px]:h-auto max-[1100px]:auto-rows-[minmax(280px,auto)] max-[560px]:px-4 max-[560px]:pt-4.5 max-[560px]:pb-7 max-[560px]:gap-3.5"
                     style={{ gridTemplateColumns: "480px 1fr" }}>

                {/* ── 1. 환자 목록 패널 ── */}
                <aside className={`${wsPanelClass} flex flex-col`}>
                    <div className={wsPanelHeadClass}>
                        <div className={`${wsHeadLeftClass} w-full`}>
                            <div className="flex items-center justify-between w-full gap-2">
                                <div className="flex items-center gap-2">
                                    <h2 className={wsPanelTitleClass}>
                                        {showHiddenPatients ? "숨긴 환자 목록" : "환자 목록"}
                                    </h2>
                                    <span className={wsCountClass}>{displayedPatients.length}명</span>
                                </div>
                                <button type="button" className="btn btn-small" onClick={() => {}}>환자 추가</button>
                            </div>

                            {/* 검색 필터 UI */}
                            <div className="flex gap-2 mt-2 items-stretch">
                                {/* 왼쪽: 검색창 & 날짜 */}
                                <div className="flex flex-col flex-1 gap-2">
                                    <input
                                        type="text"
                                        placeholder="환자 이름 또는 ID 검색"
                                        className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm text-slate-800 focus:outline-none focus:border-[#14b876]"
                                        value={searchKeyword}
                                        onChange={(e) => setSearchKeyword(e.target.value)}
                                        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                                    />
                                    <div className="flex items-center gap-1">
                                        <input
                                            type="date"
                                            className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-xl text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                                            value={startDate}
                                            onChange={(e) => setStartDate(e.target.value)}
                                        />
                                        <span className="text-slate-400">-</span>
                                        <input
                                            type="date"
                                            className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-xl text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                                            value={endDate}
                                            onChange={(e) => setEndDate(e.target.value)}
                                        />
                                    </div>
                                </div>

                                {/* 오른쪽: 검색 버튼 */}
                                <button
                                    type="button"
                                    className="w-18 bg-slate-500 hover:bg-slate-600 text-white font-bold rounded-xl text-sm transition-colors flex items-center justify-center cursor-pointer"
                                    onClick={handleSearch}
                                >
                                    검색
                                </button>
                            </div>
                        </div>
                    </div>

                    <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-2.5">
                        {displayedPatients.length > 0 ? (
                            displayedPatients.map((p) => (
                                <li key={p["patient-id"]} className="flex items-center pl-2 gap-2">
                                    <input
                                        type="checkbox"
                                        checked={checkedPatientIds.has(p["patient-id"])}
                                        onChange={() => togglePatientCheck(p["patient-id"])}
                                        className="cursor-pointer"
                                    />
                                    <button
                                        type="button"
                                        className={`${patientRowBase} flex-1 ${
                                            p["patient-id"] === selectedPatientId ? patientRowActive : patientRowInactive
                                        }`}
                                        onClick={() => handleSelectPatient(p["patient-id"])}
                                    >
                                        <span className={patientAvatarClass}>{p["patient-name"].charAt(0)}</span>
                                        <span className={patientMainClass}>
                      <span className={patientNameClass}>{p["patient-name"]}</span>
                      <span className={patientSubClass}>
                        {sexLabel(p["patient-sex"])} · {p["patient-birth"]} · 최근 진료: {p["latest-study-datetime"]?.split('T')[0]}
                      </span>
                    </span>
                                        <span className={`${patientBadgeBase} ${p["patient-id"] === selectedPatientId ? patientBadgeActive : patientBadgeDefault}`}>{p["study-count"]}</span>
                                    </button>
                                </li>
                            ))
                        ) : (
                            <li className="p-4 text-center text-slate-500 text-sm">
                                표시할 환자가 없습니다.
                            </li>
                        )}
                    </ul>

                    <div className="ws-panel-footer p-3 border-t border-[#eee] flex gap-2 flex-wrap">
                        <button
                            type="button"
                            onClick={() => setCheckedPatientIds(new Set())}
                            disabled={checkedPatientIds.size === 0}
                            className="px-2 py-1 text-xs cursor-pointer"
                        >
                            선택 해제
                        </button>
                        <button
                            type="button"
                            onClick={() => executePatientAction("백엔드로 전송할 환자 ID 리스트:")}
                            disabled={checkedPatientIds.size === 0}
                            className="px-2 py-1 text-xs cursor-pointer"
                        >
                            {showHiddenPatients ? "숨기기 해제" : "숨기기"}
                        </button>
                        <button
                            type="button"
                            onClick={toggleShowHiddenPatients}
                            className="px-2 py-1 text-xs cursor-pointer ml-auto"
                        >
                            {showHiddenPatients ? "일반 환자 보기" : "숨긴 환자 보기"}
                        </button>
                    </div>
                </aside>

                {/* ── 2. 검사(DICOM) 목록 패널 ── */}
                <section className={`${wsPanelClass} flex flex-col`}>
                    <div className={wsPanelHeadClass}>
                        <div className={`${wsHeadLeftClass} w-full`}>
                            <div className="flex items-center justify-between w-full gap-2">
                                <div>
                                    <div className="flex items-center gap-2">
                                        <h2 className={wsPanelTitleClass}>
                                            {showHiddenStudies ? "숨긴 검사 목록" : "검사 목록"}
                                        </h2>
                                        <span className={wsCountClass}>{displayedStudies.length}건</span>
                                    </div>
                                    {selectedPatient ? (
                                        <span className={wsSubLabelClass}>
                      {selectedPatient["patient-name"]} · {sexLabel(selectedPatient["patient-sex"])} · {selectedPatient["patient-birth"]}
                    </span>
                                    ) : (
                                        <span className={wsSubLabelClass}>환자를 선택하세요</span>
                                    )}
                                </div>

                                <div className="flex items-center gap-4">
                                    {selectedPatient && (
                                        <div className="flex gap-2">
                                            <button
                                                type="button"
                                                onClick={() => setCheckedStudyIds(new Set())}
                                                disabled={checkedStudyIds.size === 0}
                                                className="px-2 py-1 text-xs cursor-pointer"
                                            >
                                                선택 해제
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => executeStudyAction("백엔드로 전송할 검사 ID(숨기기) 리스트:")}
                                                disabled={checkedStudyIds.size === 0}
                                                className="px-2 py-1 text-xs cursor-pointer"
                                            >
                                                {showHiddenStudies ? "숨기기 해제" : "숨기기"}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={toggleShowHiddenStudies}
                                                className="px-2 py-1 text-xs cursor-pointer"
                                            >
                                                {showHiddenStudies ? "일반 파일 보기" : "숨긴 파일 보기"}
                                            </button>
                                            <button
                                                type="button"
                                                onClick={() => executeStudyAction("연구 목적 활용 허용할 검사 ID 리스트:")}
                                                disabled={checkedStudyIds.size === 0}
                                                className="px-2 py-1 text-xs cursor-pointer"
                                            >
                                                연구 목적 활용 허용
                                            </button>
                                        </div>
                                    )}

                                    <button type="button" className="btn btn-medium whitespace-nowrap">
                                        파일 추가
                                    </button>
                                </div>
                            </div>
                        </div>
                    </div>

                    {selectedPatient ? (
                        <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
                            <div className="grid items-center gap-2.5 shrink-0 font-bold py-3 px-5 text-xs tracking-[0.02em] text-ink-soft bg-canvas border-b border-line max-[560px]:hidden" style={{ gridTemplateColumns: studyGridColumns }}>
                                <span></span>
                                <span className="col-modality">모달리티</span>
                                <span className={colDescClass}>검사 설명</span>
                                {/* ★ 수정 포인트 2: <span className="col-body">검사 부위</span> 제거됨 */}
                                <span className={colDateClass}>검사 일자</span>
                                <span className={colSeriesClass}>시리즈</span>
                                <span className={colImagesClass}>영상 수</span>
                                <span className="text-center">연구 활용</span>
                            </div>

                            <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                                {displayedStudies.length > 0 ? (
                                    displayedStudies.map((it, idx) => (
                                        <li key={it["study-key"]}>
                                            <div
                                                className="study-row grid items-center gap-2.5 w-full cursor-pointer text-left p-3.5 border-[1.5px] border-transparent rounded-xl bg-transparent font-[inherit] text-sm text-ink transition-[background,border-color] duration-150 hover:bg-canvas"
                                                onDoubleClick={() => router.push(`/viewer/${it["study-key"]}`)}
                                                style={{ gridTemplateColumns: studyGridColumns }}
                                                title="더블클릭하면 DICOM 뷰어 화면으로 이동합니다."
                                            >
                                                <div onClick={(e) => e.stopPropagation()} className="flex items-center">
                                                    <input
                                                        type="checkbox"
                                                        checked={checkedStudyIds.has(it["study-key"])}
                                                        onChange={() => toggleStudyCheck(it["study-key"])}
                                                        className="cursor-pointer"
                                                    />
                                                </div>
                                                <span className="col-modality">
                          <span className={`${modalityBadgeClass} ${modalityColors[it.modality.toLowerCase()] || "bg-slate"}`}>
                            {it.modality}
                          </span>
                        </span>
                                                <span className={colDescClass}>{it.description}</span>
                                                {/* ★ 수정 포인트 3: <span className="col-body">{it.bodyPart}</span> 제거됨 */}
                                                <span className={colDateClass}>{it.datetime.split("T")[0]}</span>
                                                <span className={colSeriesClass}>#{it["series-num"]}</span>
                                                <span className={colImagesClass}>{it["images-num"]}</span>
                                                <span
                                                    className={`text-center font-bold ${
                                                        it["allow-research"] ? "text-[#28a745]" : "text-[#dc3545]"
                                                    }`}
                                                >
                          {it["allow-research"] ? "예" : "아니오"}
                        </span>
                                            </div>
                                        </li>
                                    ))
                                ) : (
                                    <li className="p-4 text-center text-slate-500 text-sm">
                                        표시할 검사 파일이 없습니다.
                                    </li>
                                )}
                            </ul>
                        </div>
                    ) : (
                        <div className="flex flex-1 items-center justify-center text-ink-soft text-base p-8">왼쪽에서 환자를 선택해 주세요.</div>
                    )}
                </section>
            </section>
        </div>
    );
}