"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import dicomApi from "../api/dicomApi";
import AddPatientModal from "@/app/workspace/AddPatientModal";

// ── 스타일 변수 ──
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
const studyGridColumns = "30px minmax(260px,2.7fr) 104px 70px 74px 100px";

interface PatientDto {
  "patient-key": number;
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

const sexLabel = (sex: string | null | undefined) => {
  if (sex === "M") {
    return "남";
  }

  if (sex === "F") {
    return "여";
  }

  return sex || "정보 없음";
};

const formatDate = (value: string | null | undefined, fallback = "기록 없음") => {
  return value?.split("T")[0] || fallback;
};

const formatDateInputValue = (date: Date) => {
  const year = date.getFullYear();
  const month = `${date.getMonth() + 1}`.padStart(2, "0");
  const day = `${date.getDate()}`.padStart(2, "0");

  return `${year}-${month}-${day}`;
};

const getDefaultPatientStartDate = () => {
  const date = new Date();
  date.setMonth(date.getMonth() - 3);

  return formatDateInputValue(date);
};

const getDefaultPatientEndDate = () => {
  return formatDateInputValue(new Date());
};

export default function WorkspaceDashboardPage() {
  const router = useRouter();

  // =========================== 환자 설정 ==================================
  const [isAddPatientModalOpen, setIsAddPatientModalOpen] = useState(false);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(null);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<number>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);
  const [patientSearchKeyword, setPatientSearchKeyword] = useState("");
  const [patientStartDate, setPatientStartDate] = useState(getDefaultPatientStartDate);
  const [patientEndDate, setPatientEndDate] = useState(getDefaultPatientEndDate);

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

  const selectedPatient = patients.find((p) => p["patient-key"] === selectedPatientId) ?? null;
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

  const clearCheckedStudies = () => {
    setCheckedStudyIds(new Set());
  };

  const switchStudyVisibilityView = () => {
    setShowHiddenStudies((prev) => !prev);
    setCheckedStudyIds(new Set());
  };

  const updateCheckedStudiesHidden = async (hidden: boolean) => {
    const selectedIds = Array.from(checkedStudyIds);

    if (selectedIds.length === 0) {
      return;
    }

    try {
      await dicomApi.setStudyHide(
          selectedIds.map((id) => ({
            "study-key": id,
            hidden,
          })),
      );

      const selectedIdSet = new Set(selectedIds);
      setStudies((prev) =>
          prev.map((study) =>
              selectedIdSet.has(study["study-key"]) ? { ...study, hidden } : study,
          ),
      );
      setCheckedStudyIds(new Set());
    } catch (error) {
      console.error("검사 숨김 상태 변경 실패", error);
    }
  };

  const requestResearchAllowApi = () => {
    console.warn("연구 목적 활용 허용 API가 아직 연결되지 않았습니다.", Array.from(checkedStudyIds));
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

  // 한 요청에 파일을 너무 많이 담으면 Tomcat의 멀티파트 파트 개수 제한에 걸려 파싱 자체가 실패하므로,
  // 파일을 이 개수 단위로 나눠서 순차적으로(하나씩 끝나면 다음 배치) 업로드한다.
  const UPLOAD_BATCH_SIZE = 10;

  const handleFileUpload = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0 || !selectedPatientId) {
      alert("환자를 먼저 선택해주세요.");
      return;
    }

    const fileList = Array.from(files);
    const batches: File[][] = [];
    for (let i = 0; i < fileList.length; i += UPLOAD_BATCH_SIZE) {
      batches.push(fileList.slice(i, i + UPLOAD_BATCH_SIZE));
    }

    try {
      for (const batch of batches) {
        const formData = new FormData();
        formData.append("patientKey", selectedPatientId.toString());
        batch.forEach(file => formData.append("files", file));

        // eslint-disable-next-line no-await-in-loop
        await dicomApi.uploadDicomFiles(formData);
      }

      alert("파일 업로드 및 태그 추출 완료!");
      // 업로드 후 스터디 목록 새로고침
      void fetchStudies(selectedPatientId);
    } catch (error) {
      console.error("업로드 실패:", error);
      alert("업로드 중 문제가 발생했습니다.");
    }
  };

  const updateCheckedPatientsHidden = async (hidden: boolean) => {
    const selectedIds = Array.from(checkedPatientIds);

    if (selectedIds.length === 0) {
      return;
    }

    try {
      await dicomApi.setPatientHide(
          selectedIds.map((id) => ({
            "patient-key": id,
            hidden,
          })),
      );

      const selectedIdSet = new Set(selectedIds);
      setPatients((prev) =>
          prev.map((patient) =>
              selectedIdSet.has(patient["patient-key"]) ? { ...patient, hidden } : patient,
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
    void fetchPatients(null, patientStartDate, patientEndDate);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  return (
      <div className="page">
        <section
            className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)] transition-[grid-template-columns] duration-200 max-[1100px]:grid-cols-1 max-[1100px]:h-auto max-[1100px]:auto-rows-[minmax(280px,auto)] max-[560px]:px-4 max-[560px]:pt-4.5 max-[560px]:pb-7 max-[560px]:gap-3.5"
            style={{ gridTemplateColumns: "480px 1fr" }}
        >
          {/* 1. 환자 목록 패널 */}
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
                  <button type="button" className="btn btn-small w-20 h-[48px] text-sm" onClick={() => setIsAddPatientModalOpen(true)}>
                    환자 추가
                  </button>
                </div>


                <div className="flex gap-2 mt-2 items-stretch">
                  <div className="flex flex-col flex-1 gap-2">
                    <input
                        type="text"
                        placeholder="환자 이름 또는 ID 검색"
                        className="w-full px-3 py-2 border border-slate-200 rounded-xl text-sm text-slate-800 focus:outline-none focus:border-[#14b876]"
                        value={patientSearchKeyword}
                        onChange={(e) => setPatientSearchKeyword(e.target.value)}
                        onKeyDown={(e) => {
                          if (e.key === "Enter") {
                            handlePatientSearch();
                          }
                        }}
                    />
                    <div className="flex items-center gap-1">
                      <input
                          type="date"
                          aria-label="시작일"
                          className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-xl text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                          value={patientStartDate}
                          onChange={(e) => setPatientStartDate(e.target.value)}
                      />
                      <span className="text-slate-400">-</span>
                      <input
                          type="date"
                          aria-label="종료일"
                          className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-xl text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                          value={patientEndDate}
                          onChange={(e) => setPatientEndDate(e.target.value)}
                      />
                    </div>
                  </div>

                  <button
                      type="button"
                      className="w-20 bg-slate-500 hover:bg-slate-600 text-white font-bold rounded-xl text-sm transition-colors flex items-center justify-center cursor-pointer"
                      onClick={handlePatientSearch}
                  >
                    검색
                  </button>
                </div>
              </div>
            </div>

            <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-2.5">
              {displayedPatients.length > 0 ? (
                  displayedPatients.map((p) => (
                      <li key={p["patient-key"]} className="flex items-center pl-2 gap-2">
                        <input
                            type="checkbox"
                            checked={checkedPatientIds.has(p["patient-key"])}
                            onChange={() => togglePatientCheck(p["patient-key"])}
                            className="cursor-pointer"
                        />
                        <button
                            type="button"
                            className={`${patientRowBase} flex-1 ${
                                p["patient-key"] === selectedPatientId ? patientRowActive : patientRowInactive
                            }`}
                            onClick={() => handleSelectPatient(p["patient-key"])}
                        >
                          <span className={patientAvatarClass}>{p["patient-name"]?.charAt(0)}</span>
                          <span className={patientMainClass}>
                            <span className={patientNameClass}>{p["patient-name"]}</span>
                            <span className={patientSubClass}>
                              {sexLabel(p["patient-sex"])} · {formatDate(p["patient-birth"], "정보 없음")} · 최근 진료: {formatDate(p["latest-study-datetime"])}
                            </span>
                          </span>
                          <span className={`${patientBadgeBase} ${p["patient-key"] === selectedPatientId ? patientBadgeActive : patientBadgeDefault}`}>
                            {p["study-count"]}
                          </span>
                        </button>
                      </li>
                  ))
              ) : (
                  <li className="p-4 text-center text-slate-500 text-sm">
                    {showHiddenPatients ? "숨긴 환자가 없습니다." : "표시할 환자가 없습니다."}
                  </li>
              )}
            </ul>

            <div className="ws-panel-footer p-3 border-t border-[#eee] flex gap-2 flex-wrap">
              <button
                  type="button"
                  onClick={clearCheckedPatients}
                  disabled={checkedPatientIds.size === 0}
                  className="px-2 py-1 text-xs cursor-pointer"
              >
                선택 해제
              </button>
              <button
                  type="button"
                  onClick={() => {
                    void updateCheckedPatientsHidden(!showHiddenPatients);
                  }}
                  disabled={checkedPatientIds.size === 0}
                  className="px-2 py-1 text-xs cursor-pointer"
              >
                {showHiddenPatients ? "숨기기 해제" : "숨기기"}
              </button>
              <button
                  type="button"
                  onClick={() => switchPatientVisibilityView(!showHiddenPatients)}
                  className="px-2 py-1 text-xs cursor-pointer ml-auto"
              >
                {showHiddenPatients ? "일반 환자 보기" : "숨긴 환자 보기"}
              </button>
            </div>
          </aside>

          {/* 2. 검사(DICOM) 목록 패널 */}
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
                          {selectedPatient["patient-name"]} · {sexLabel(selectedPatient["patient-sex"])} · {formatDate(selectedPatient["patient-birth"], "정보 없음")}
                        </span>
                    ) : (
                        <span className={wsSubLabelClass}>환자를 선택하세요</span>
                    )}
                  </div>

                  <div className="flex items-center gap-4">
                    {selectedPatient && (
                        <div className="flex gap-2">
                          {/*다운로드 버튼(주소 이동)*/}
                          <button
                              type="button"
                              onClick={() => router.push("/research")}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            다운로드
                          </button>
                          <button
                              type="button"
                              onClick={clearCheckedStudies}
                              disabled={checkedStudyIds.size === 0}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            선택 해제
                          </button>
                          <button
                              type="button"
                              onClick={() => {
                                void updateCheckedStudiesHidden(!showHiddenStudies);
                              }}
                              disabled={checkedStudyIds.size === 0}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            {showHiddenStudies ? "숨기기 해제" : "숨기기"}
                          </button>
                          <button
                              type="button"
                              onClick={switchStudyVisibilityView}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            {showHiddenStudies ? "일반 파일 보기" : "숨긴 파일 보기"}
                          </button>
                          <button
                              type="button"
                              onClick={requestResearchAllowApi}
                              disabled={checkedStudyIds.size === 0}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            연구 목적 활용 허용
                          </button>
                        </div>
                    )}

                    {/* 파일 업로드용 숨겨진 input */}
                    <input
                        type="file"
                        id="dicom-upload"
                        className="hidden"
                        multiple
                        onChange={handleFileUpload}
                    />
                    {/* 이 라벨을 누르면 input이 트리거됨 */}
                    <label htmlFor="dicom-upload" className="btn btn-medium cursor-pointer">
                      파일 추가
                    </label>
                  </div>
                </div>
              </div>
            </div>

            {selectedPatient ? (
                <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
                  <div
                      className="grid items-center gap-2.5 shrink-0 font-bold py-3 px-5 text-xs tracking-[0.02em] text-ink-soft bg-canvas border-b border-line max-[560px]:hidden"
                      style={{ gridTemplateColumns: studyGridColumns }}
                  >
                    <span></span>
                    <span className={colDescClass}>검사 설명</span>
                    <span className={colDateClass}>검사 일자</span>
                    <span className={colSeriesClass}>시리즈</span>
                    <span className={colImagesClass}>영상 수</span>
                    <span className="text-center">연구 활용</span>
                  </div>

                  <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                    {isStudyLoading ? (
                        <li className="p-4 text-center text-slate-500 text-sm">검사 목록을 불러오는 중입니다.</li>
                    ) : studyError ? (
                        <li className="p-4 text-center text-sm text-red-600">{studyError}</li>
                    ) : displayedStudies.length > 0 ? (
                        displayedStudies.map((it) => (
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
                                  <span className={colDescClass}>{it.description ? it.description : "-"}</span>
                                  <span className={colDateClass}>{formatDate(it.datetime)}</span>
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
                          {showHiddenStudies ? "숨긴 검사가 없습니다." : "표시할 검사 파일이 없습니다."}
                        </li>
                    )}
                  </ul>
                </div>
            ) : (
                <div className="flex flex-1 items-center justify-center text-ink-soft text-base p-8">
                  왼쪽에서 환자를 선택해 주세요.
                </div>
            )}
          </section>
        </section>
        {isAddPatientModalOpen && <AddPatientModal onClose={() => setIsAddPatientModalOpen(false)} onRefresh={fetchPatients} />}
      </div>
  );
}
