"use client";

import { useState, useEffect } from "react";
import dicomApi from "../api/dicomApi";

// ── 스타일 변수 ──
const wsPanelClass = "flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px]";
const wsPanelHeadClass = "flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line";
const wsHeadLeftClass = "flex min-w-0 flex-1 flex-col gap-1.25";
const wsPanelTitleClass = "m-0 font-bold text-xl text-ink tracking-[-0.01em]";
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
const patientInputClass = "w-full rounded-xl border border-line bg-paper px-3 py-2 text-sm text-ink outline-none focus:border-mint-deep";
const patientActionButtonClass = "cursor-pointer border-none bg-transparent px-1.5 py-1 text-xs font-semibold text-ink-soft transition-colors duration-150 hover:text-ink disabled:cursor-not-allowed disabled:opacity-40";
const colDescClass = "col-desc overflow-hidden whitespace-nowrap font-semibold text-ellipsis";
const colDateClass = "col-date text-ink-soft";
const colSeriesClass = "col-series text-right tabular-nums text-ink-soft";
const colImagesClass = "col-images text-right tabular-nums text-ink-soft";
const studyGridColumns = "30px 1.8fr 1fr 70px 70px 100px";

interface PatientDto {
  "patient-id": number;
  "patient-name": string;
  "patient-sex": string;
  "patient-birth": string | null;
  "latest-study-datetime": string | null;
  "study-count": number;
  hidden: boolean;
}

interface StudyDto {
  "study-key": number;
  description: string;
  datetime: string;
  "series-num": number;
  "images-num": number;
  "allow-research": boolean;
  hidden: boolean;
}

export default function WorkspaceDashboardPage() {
  // =========================== 환자 설정 ==================================
  const [, setIsAddPatientModalOpen] = useState(false);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(null);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<number>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);
  const [patientSearchKeyword, setPatientSearchKeyword] = useState("");
  const [patientStartDate, setPatientStartDate] = useState("");
  const [patientEndDate, setPatientEndDate] = useState("");

  // 서버에서 환자 목록 가져오기
  const fetchPatients = async (
      search: string | null = patientSearchKeyword,
      start: string | null = patientStartDate,
      end: string | null = patientEndDate,
  ) => {
      const res = await dicomApi.getPatients(start || null, end || null, search?.trim() || null);

      setPatients(res as PatientDto[]);
    };

  // 환자 선택하기 (선택한 환자는 하이라이팅되며, 환자의 스터디가 로딩된다.)
  const handleSelectPatient = (id: number) => {
    setSelectedPatientId(id);
    setCheckedStudyIds(new Set());
    setShowHiddenStudies(false);
    setStudies([]);
    void fetchStudies(id);
  };

  // 환자 체크박스로 선택하기
  const togglePatientCheck = (id: number) => {
    setCheckedPatientIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const selectedPatient = patients.find((p) => p["patient-id"] === selectedPatientId) ?? null;
  const displayedPatients = patients.filter((p) => p.hidden === showHiddenPatients);

  // ================================== 스터디 영역 =========================================
  const [studies, setStudies] = useState<StudyDto[]>([]);
  const [checkedStudyIds, setCheckedStudyIds] = useState<Set<number>>(new Set());
  const [showHiddenStudies, setShowHiddenStudies] = useState(false);
  const [isStudyLoading, setIsStudyLoading] = useState(false);
  const [studyError, setStudyError] = useState<string | null>(null);

  // 서버에서 스터디 목록 가져오기
  const fetchStudies = async (patientId: number) => {
    setIsStudyLoading(true);
    setStudyError(null);

    try {
      const res = await dicomApi.getStudies(patientId, null, null, null);

      setStudies(Array.isArray(res) ? (res as StudyDto[]) : []);
    } catch (error) {
      console.error("스터디 목록 조회 실패", error);
      setStudies([]);
      setStudyError("검사 목록을 불러오지 못했습니다.");
    } finally {
      setIsStudyLoading(false);
    }
  };

  const toggleStudyCheck = (id: number) => {
    setCheckedStudyIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {
        next.delete(id);
      } else {
        next.add(id);
      }
      return next;
    });
  };

  const displayedStudies = !selectedPatient
      ? []
      : studies.filter(s => s.hidden === showHiddenStudies);

  const handlePatientSearch = () => {
    setCheckedPatientIds(new Set());
    setSelectedPatientId(null);
    setCheckedStudyIds(new Set());
    setStudies([]);
    setStudyError(null);
    void fetchPatients(patientSearchKeyword, patientStartDate, patientEndDate);
  };

  const clearCheckedPatients = () => {
    setCheckedPatientIds(new Set());
  };

  const switchPatientVisibilityView = (showHidden: boolean) => {
    setShowHiddenPatients(showHidden);
    setCheckedPatientIds(new Set());
    setSelectedPatientId(null);
    setCheckedStudyIds(new Set());
    setStudies([]);
    setStudyError(null);
  };

  const updateCheckedPatientsHidden = async (hidden: boolean) => {
    const selectedIds = Array.from(checkedPatientIds);

    if (selectedIds.length === 0) {
      return;
    }

    try {
      await dicomApi.setPatientHide(
          selectedIds.map((id) => ({
            "patient-id": id,
            hidden,
          })),
      );

      const selectedIdSet = new Set(selectedIds);
      setPatients((prev) =>
          prev.map((patient) =>
              selectedIdSet.has(patient["patient-id"]) ? { ...patient, hidden } : patient,
          ),
      );
      setCheckedPatientIds(new Set());

      if (selectedPatientId !== null && selectedIdSet.has(selectedPatientId)) {
        setSelectedPatientId(null);
        setCheckedStudyIds(new Set());
        setStudies([]);
        setStudyError(null);
      }
    } catch (error) {
      console.error("환자 숨김 상태 변경 실패", error);
    }
  };


  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchPatients(null);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
      <div className="page">
        <section className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)]" style={{ gridTemplateColumns: "480px 1fr" }}>

          {/* 1. 환자 목록 */}
          <aside className={`${wsPanelClass} flex flex-col`}>
            <div className={wsPanelHeadClass}>
              <div className={`${wsHeadLeftClass} w-full`}>
                <div className="flex items-center justify-between w-full">
                  <h2 className={wsPanelTitleClass}>환자 목록</h2>
                  <button type="button" className="btn btn-small" onClick={() => setIsAddPatientModalOpen(true)}>환자 추가</button>
                </div>
                <div className="mt-3 grid grid-cols-[1fr_auto] gap-2">
                  <div className="flex min-w-0 flex-col gap-2">
                    <input
                        className={patientInputClass}
                        placeholder="이름 또는 ID 검색"
                        value={patientSearchKeyword}
                        onChange={(e) => setPatientSearchKeyword(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            handlePatientSearch();
                          }
                        }}
                    />
                    <div className="grid grid-cols-2 gap-2">
                      <input
                          type="date"
                          aria-label="시작일"
                          className={patientInputClass}
                          value={patientStartDate}
                          onChange={(e) => setPatientStartDate(e.target.value)}
                      />
                      <input
                          type="date"
                          aria-label="종료일"
                          className={patientInputClass}
                          value={patientEndDate}
                          onChange={(e) => setPatientEndDate(e.target.value)}
                      />
                    </div>
                  </div>
                  <button type="button" className="btn btn-small h-full min-w-18 self-stretch px-4" onClick={handlePatientSearch}>
                    검색
                  </button>
                </div>
              </div>
            </div>
            <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-2.5">
              {displayedPatients.length === 0 ? (
                  <li key="1" className="p-6 text-center text-sm text-ink-soft">
                    {showHiddenPatients ? "숨긴 환자가 없습니다." : "표시할 환자가 없습니다."}
                  </li>
              ) : displayedPatients.map((p) => (
                  <li key={p["patient-id"]} className="flex items-center pl-2 gap-2">
                    <input
                        type="checkbox"
                        checked={checkedPatientIds.has(p["patient-id"])}
                        onChange={() => togglePatientCheck(p["patient-id"])}
                    />
                    <button
                        className={`${patientRowBase} ${p["patient-id"] === selectedPatientId ? patientRowActive : patientRowInactive}`}
                        onClick={() => handleSelectPatient(p["patient-id"])}
                    >
                      <span className={patientAvatarClass}>{p["patient-name"]?.charAt(0)}</span>
                      <span className={patientMainClass}>
            <span className={patientNameClass}>{p["patient-name"]}</span>
            <span className={patientSubClass}>
                {p["patient-sex"]} · {p["patient-birth"]?.split('T')[0] || "정보 없음"} ·
                최근 진료: {p["latest-study-datetime"]?.split('T')[0] || "기록 없음"}
            </span>
          </span>
                      <span className={`${patientBadgeBase} ${p["patient-id"] === selectedPatientId ? patientBadgeActive : patientBadgeDefault}`}>
            {p["study-count"]}
          </span>
                    </button>
                  </li>
              ))}
            </ul>
            <div className="flex shrink-0 flex-wrap items-center gap-2 border-t border-line px-4 py-3">
              <button
                  type="button"
                  className={patientActionButtonClass}
                  disabled={checkedPatientIds.size === 0}
                  onClick={clearCheckedPatients}
              >
                선택 해제
              </button>
              <button
                  type="button"
                  className={patientActionButtonClass}
                  disabled={checkedPatientIds.size === 0}
                  onClick={() => {
                    void updateCheckedPatientsHidden(!showHiddenPatients);
                  }}
              >
                {showHiddenPatients ? "숨김 해제하기" : "숨기기"}
              </button>
              <button
                  type="button"
                  className={patientActionButtonClass}
                  onClick={() => switchPatientVisibilityView(!showHiddenPatients)}
              >
                {showHiddenPatients ? "일반 환자 보기" : "숨긴 환자 표시"}
              </button>
            </div>
          </aside>

          {/* 2. 검사 목록 */}
          <section className={`${wsPanelClass} flex flex-col`}>
            <div className={wsPanelHeadClass}>
              <div className={`${wsHeadLeftClass} w-full`}>
                <div className="flex items-center justify-between w-full">
                  <h2 className={wsPanelTitleClass}>검사 목록</h2>
                  <button type="button" className="btn btn-medium">파일 추가</button>
                </div>
                {selectedPatient && <span className={wsSubLabelClass}>{selectedPatient["patient-name"]} 선택됨</span>}
              </div>
            </div>
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
              <div className="grid items-center gap-2.5 shrink-0 font-bold py-3 px-5 text-xs text-ink-soft bg-canvas border-b border-line" style={{ gridTemplateColumns: studyGridColumns }}>
                <span></span><span className={colDescClass}>검사 설명</span><span className={colDateClass}>검사 일자</span><span className={colSeriesClass}>시리즈</span><span className={colImagesClass}>영상 수</span><span>연구 활용</span>
              </div>
              <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                {!selectedPatient ? (
                    <li className="p-8 text-center text-sm text-ink-soft">환자를 선택해주세요.</li>
                ) : isStudyLoading ? (
                    <li className="p-8 text-center text-sm text-ink-soft">검사 목록을 불러오는 중입니다.</li>
                ) : studyError ? (
                    <li className="p-8 text-center text-sm text-red-600">{studyError}</li>
                ) : displayedStudies.length === 0 ? (
                    <li className="p-8 text-center text-sm text-ink-soft">
                      {showHiddenStudies ? "숨긴 검사가 없습니다." : "표시할 검사가 없습니다."}
                    </li>
                ) : displayedStudies.map((it) => (
                    <li key={it["study-key"]}>
                      <div className="study-row grid items-center gap-2.5 w-full cursor-pointer p-3.5 border-b" style={{ gridTemplateColumns: studyGridColumns }}>
                        <input type="checkbox" checked={checkedStudyIds.has(it["study-key"])} onChange={() => toggleStudyCheck(it["study-key"])} />
                        <span className={colDescClass}>{it.description}</span>
                        <span className={colDateClass}>{it.datetime.split("T")[0]}</span>
                        <span className={colSeriesClass}>{it["series-num"]}</span>
                        <span className={colImagesClass}>{it["images-num"]}</span>
                        <span className={it["allow-research"] ? "text-green-600" : "text-red-600"}>{it["allow-research"] ? "예" : "아니오"}</span>
                      </div>
                    </li>
                ))}
              </ul>
            </div>
          </section>
        </section>
        {/*{isAddPatientModalOpen && <AddPatientModal onClose={() => setIsAddPatientModalOpen(false)} onRefresh={fetchMyPatients} />}*/}
      </div>
  );
}
