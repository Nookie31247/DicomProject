"use client";

import { useState, type CSSProperties } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { patients, studies, currentUser } from "@/mock-data";

export default function WorkspaceDashboardPage() {
  const router = useRouter();

  // ── 환자(Patient) 관련 상태 ──
  const [selectedPatientId, setSelectedPatientId] = useState(patients[0]?.["patient-id"]);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<string>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);

  // ── 검사(Study/DICOM) 관련 상태 ──
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);
  const [checkedStudyIds, setCheckedStudyIds] = useState<Set<string>>(new Set());
  const [showHiddenStudies, setShowHiddenStudies] = useState(false);

  // ==========================================
  // 1. 환자 목록 컨트롤 로직
  // ==========================================
  const handleSelectPatient = (id: string) => {
    setSelectedPatientId(id);
    setSelectedItemId(null);
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

  const displayedPatients = showHiddenPatients ? [] : patients.filter(p => {
    if (p.hidden) return false;
    
    if (appliedFilters.keyword) {
      const kw = appliedFilters.keyword.toLowerCase();
      if (!p["patient-name"].toLowerCase().includes(kw) && !p["patient-id"].toLowerCase().includes(kw)) {
        return false;
      }
    }
    
    if (appliedFilters.start) {
      const studyDate = p["latest-study-datetime"].split("T")[0];
      if (studyDate < appliedFilters.start) return false;
    }
    
    if (appliedFilters.end) {
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

  const displayedStudies = (!selectedPatient || showHiddenStudies)
      ? []
      : studies.filter(s => s["patient-id"] === selectedPatientId && !s.hidden);

  // ==========================================
  // 기타 헬퍼 및 스타일
  // ==========================================
  const sexLabel = (s: "M" | "F") => (s === "M" ? "남" : "여");

  const workspaceStyle = { "--ws-grid": "480px 1fr" } as CSSProperties;

  // ★ 수정 포인트 1: 기존 "30px 84px 1.6fr 1fr 1fr 70px 70px 100px" 에서 검사 부위에 해당하는 1fr 제거
  const studyGridColumns = "30px 84px 1.6fr 1fr 70px 70px 100px";

  return (
      <div className="page">
        {/* ───────────── Workspace ───────────── */}
        <section className="workspace" style={workspaceStyle}>

          {/* ── 1. 환자 목록 패널 ── */}
          <aside className="ws-panel patients-panel flex flex-col">
            <div className="ws-panel-head">
              <div className="ws-head-left w-full">
                <div className="ws-title-row flex items-center justify-between w-full">
                  <div className="flex items-center gap-2">
                    <h2 className="ws-panel-title">
                      {showHiddenPatients ? "숨긴 환자 목록" : "환자 목록"}
                    </h2>
                    <span className="ws-count">{displayedPatients.length}명</span>
                  </div>
                  <button type="button" className="logout-btn" style={{ padding: '6px 12px', fontSize: '13px' }}>환자 추가</button>
                </div>

                {/* 검색 필터 UI */}
                <div className="flex gap-2 mt-2 items-stretch">
                  {/* 왼쪽: 검색창 & 날짜 */}
                  <div className="flex flex-col flex-1 gap-2">
                    <input
                      type="text"
                      placeholder="환자 이름 또는 ID 검색"
                      className="w-full px-3 py-2 border border-slate-200 rounded-[12px] text-[13px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                      value={searchKeyword}
                      onChange={(e) => setSearchKeyword(e.target.value)}
                      onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                    />
                    <div className="flex items-center gap-1">
                      <input
                        type="date"
                        className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-[12px] text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                        value={startDate}
                        onChange={(e) => setStartDate(e.target.value)}
                      />
                      <span className="text-slate-400">-</span>
                      <input
                        type="date"
                        className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-[12px] text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                        value={endDate}
                        onChange={(e) => setEndDate(e.target.value)}
                      />
                    </div>
                  </div>
                  
                  {/* 오른쪽: 검색 버튼 */}
                  <button
                    type="button"
                    className="w-[72px] bg-slate-500 hover:bg-slate-600 text-white font-bold rounded-[12px] text-[14px] transition-colors flex items-center justify-center cursor-pointer"
                    onClick={handleSearch}
                  >
                    검색
                  </button>
                </div>
              </div>
            </div>

            <ul className="patient-list flex-1 overflow-y-auto">
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
                            className={`patient-row flex-1 ${
                                p["patient-id"] === selectedPatientId ? "active" : ""
                            }`}
                            onClick={() => handleSelectPatient(p["patient-id"])}
                        >
                          <span className="patient-avatar">{p["patient-name"].charAt(0)}</span>
                          <span className="patient-main">
                      <span className="patient-name">{p["patient-name"]}</span>
                      <span className="patient-sub">
                        {sexLabel(p["patient-sex"])} · {p["patient-birth"]} · 최근 진료: {p["latest-study-datetime"]?.split('T')[0]}
                      </span>
                    </span>
                          <span className="patient-badge">{p["study-count"]}</span>
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
          <section className="ws-panel studies-panel flex flex-col">
            <div className="ws-panel-head">
              <div className="ws-head-left w-full">
                <div className="ws-title-row flex items-center justify-between w-full">
                  <div>
                    <div className="flex items-center gap-2">
                      <h2 className="ws-panel-title">
                        {showHiddenStudies ? "숨긴 검사 목록" : "검사 목록"}
                      </h2>
                      <span className="ws-count">{displayedStudies.length}건</span>
                    </div>
                    {selectedPatient ? (
                        <span className="ws-sub-label">
                      {selectedPatient["patient-name"]} · {sexLabel(selectedPatient["patient-sex"])} · {selectedPatient["patient-birth"]}
                    </span>
                    ) : (
                        <span className="ws-sub-label">환자를 선택하세요</span>
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

                    <button type="button" className="logout-btn whitespace-nowrap">
                      파일 추가
                    </button>
                  </div>
                </div>
              </div>
            </div>

            {selectedPatient ? (
                <div className="study-table flex-1 flex flex-col overflow-hidden">
                  <div className="study-head" style={{ gridTemplateColumns: studyGridColumns }}>
                    <span></span>
                    <span className="col-modality">모달리티</span>
                    <span className="col-desc">검사 설명</span>
                    {/* ★ 수정 포인트 2: <span className="col-body">검사 부위</span> 제거됨 */}
                    <span className="col-date">검사 일자</span>
                    <span className="col-series">시리즈</span>
                    <span className="col-images">영상 수</span>
                    <span className="col-research text-center">연구 활용</span>
                  </div>

                  <ul className="study-list">
                    {displayedStudies.length > 0 ? (
                        displayedStudies.map((it, idx) => (
                            <li key={it["study-key"]}>
                              <div
                                  className={`study-row ${it["study-key"] === selectedItemId ? "active" : ""}`}
                                  onClick={() => setSelectedItemId(it["study-key"])}
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
                          <span className={`modality-badge mod-${it.modality.toLowerCase()}`}>
                            {it.modality}
                          </span>
                        </span>
                                <span className="col-desc">{it.description}</span>
                                {/* ★ 수정 포인트 3: <span className="col-body">{it.bodyPart}</span> 제거됨 */}
                                <span className="col-date">{it.datetime.split("T")[0]}</span>
                                <span className="col-series">#{it["series-num"]}</span>
                                <span className="col-images">{it["images-num"]}</span>
                                <span
                                    className={`col-research text-center font-bold ${
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
                <div className="ws-empty">왼쪽에서 환자를 선택해 주세요.</div>
            )}
          </section>
        </section>
      </div>
  );
}