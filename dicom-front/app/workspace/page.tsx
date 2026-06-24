"use client";

import { useState, type CSSProperties } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { patients, currentUser } from "./mock-data"; // 경로 상황에 맞게 조정

export default function WorkspaceDashboardPage() {
  const router = useRouter();

  // ── 환자(Patient) 관련 상태 ──
  const [selectedPatientId, setSelectedPatientId] = useState(patients[0]?.id);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<string>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);

  // ── 검사(Study/DICOM) 관련 상태 ──
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);
  const [checkedStudyIds, setCheckedStudyIds] = useState<Set<string>>(new Set());
  const [showHiddenStudies, setShowHiddenStudies] = useState(false);

  const handleLogout = () => router.push("/");

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

  const toggleShowHiddenPatients = () => {
    setShowHiddenPatients((prev) => !prev);
    setCheckedPatientIds(new Set());
  };

  const displayedPatients = showHiddenPatients ? [] : patients;
  const selectedPatient = patients.find((p) => p.id === selectedPatientId) ?? null;

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
      : selectedPatient.items;

  // ==========================================
  // 기타 헬퍼 및 스타일
  // ==========================================
  const sexLabel = (s: "M" | "F") => (s === "M" ? "남" : "여");

  const workspaceStyle = { "--ws-grid": "380px 1fr" } as CSSProperties;

  // ★ 수정 포인트 1: 기존 "30px 84px 1.6fr 1fr 1fr 70px 70px 100px" 에서 검사 부위에 해당하는 1fr 제거
  const studyGridColumns = "30px 84px 1.6fr 1fr 70px 70px 100px";

  return (
      <div className="page">
        {/* ───────────── Nav ───────────── */}
        <header className="nav">
          <Link href="/" className="logo">
            DICOM!
          </Link>
          <div className="nav-user">
            <span className="user-avatar">{currentUser.name.charAt(0)}</span>
            <span className="user-name">{currentUser.name}님</span>
            <button type="button" className="logout-btn" onClick={handleLogout}>
              로그아웃
            </button>
          </div>
        </header>

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
                  <button type="button" className="logout-btn">
                    환자 추가
                  </button>
                </div>
              </div>
            </div>

            <ul className="patient-list flex-1 overflow-y-auto">
              {displayedPatients.length > 0 ? (
                  displayedPatients.map((p) => (
                      <li key={p.id} className="flex items-center pl-2 gap-2">
                        <input
                            type="checkbox"
                            checked={checkedPatientIds.has(p.id)}
                            onChange={() => togglePatientCheck(p.id)}
                            className="cursor-pointer"
                        />
                        <button
                            type="button"
                            className={`patient-row flex-1 ${
                                p.id === selectedPatientId ? "active" : ""
                            }`}
                            onClick={() => handleSelectPatient(p.id)}
                        >
                          <span className="patient-avatar">{p.name.charAt(0)}</span>
                          <span className="patient-main">
                      <span className="patient-name">{p.name}</span>
                      <span className="patient-sub">
                        {sexLabel(p.sex)} · {p.birthDate}
                      </span>
                    </span>
                          <span className="patient-badge">{p.items.length}</span>
                        </button>
                      </li>
                  ))
              ) : (
                  <li className="p-4 text-center text-[#888] text-sm">
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
                      {selectedPatient.name} · {sexLabel(selectedPatient.sex)} ·{" "}
                          {selectedPatient.birthDate} · {selectedPatient.patientId}
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
                          <button
                              type="button"
                              onClick={() => executeStudyAction("연구 목적 활용 허용 안할 검사 ID 리스트:")}
                              disabled={checkedStudyIds.size === 0}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            연구 목적 활용 허용 안함
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
                            <li key={it.id}>
                              <div
                                  className={`study-row ${it.id === selectedItemId ? "active" : ""}`}
                                  onClick={() => setSelectedItemId(it.id)}
                                  onDoubleClick={() => router.push(`/viewer/${it.id}`)}
                                  style={{ gridTemplateColumns: studyGridColumns }}
                                  title="더블클릭하면 DICOM 뷰어 화면으로 이동합니다."
                              >
                                <div onClick={(e) => e.stopPropagation()} className="flex items-center">
                                  <input
                                      type="checkbox"
                                      checked={checkedStudyIds.has(it.id)}
                                      onChange={() => toggleStudyCheck(it.id)}
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
                                <span className="col-date">{it.studyDate}</span>
                                <span className="col-series">#{it.seriesNumber}</span>
                                <span className="col-images">{it.images}</span>
                                <span
                                    className={`col-research text-center font-bold ${
                                        idx % 2 === 0 ? "text-[#28a745]" : "text-[#dc3545]"
                                    }`}
                                >
                          {idx % 2 === 0 ? "예" : "아니오"}
                        </span>
                              </div>
                            </li>
                        ))
                    ) : (
                        <li className="p-4 text-center text-[#888] text-sm">
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