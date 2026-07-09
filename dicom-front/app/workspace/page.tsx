"use client";

import {useState, useEffect, useRef, Suspense} from "react";
import { useRouter, usePathname, useSearchParams } from "next/navigation";
import dicomApi from "../api/dicomApi";
import AddPatientModal from "@/app/workspace/AddPatientModal";
import { useToast } from "@/app/context/ToastContext";
import { useUpload } from "@/app/context/UploadContext";
import { useConfirm } from "@/app/context/ConfirmContext";
import { RoleGuard } from "@/app/components/auth/RouteAccess";
import { clampDateInputValue, formatDateInputValue, getMaxDateInputValue, getMinDateInputValue } from "@/services/dateInputValue";

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

const getDefaultPatientStartDate = () => {
  const date = new Date();
  date.setMonth(date.getMonth() - 12);

  return formatDateInputValue(date);
};

const getDefaultPatientEndDate = () => {
  return formatDateInputValue(new Date());
};

// useSearchParams()를 쓰는 페이지는 Suspense 경계로 감싸야 한다(안 그러면 빌드 시
// "useSearchParams() should be wrapped in a suspense boundary" 에러가 난다).
export default function WorkspaceDashboardPage() {
  return (
    <RoleGuard allow="MEDICAL">
      <Suspense fallback={null}>
        <WorkspaceDashboardPageInner />
      </Suspense>
    </RoleGuard>
  );
}

function WorkspaceDashboardPageInner() {
  const router = useRouter();
  const pathname = usePathname();
  const searchParams = useSearchParams();
  const { showToast } = useToast();
  const { isUploading, uploadProgress, uploadResult, startUpload } = useUpload();
  const confirm = useConfirm();

  // 뷰어 페이지 갔다가 뒤로 돌아왔을 때 선택했던 환자/날짜 필터가 유지되도록 URL 쿼리에서
  // 초기값을 복원한다. 검색어(patientSearchKeyword)는 환자 이름 등 민감한 텍스트가 그대로
  // URL/브라우저 히스토리/서버 로그에 남을 수 있어서 일부러 URL에는 안 싣는다.
  const initialPatientId = (() => {
    const raw = searchParams.get("patientId");
    const parsed = raw ? Number(raw) : NaN;
    return Number.isFinite(parsed) ? parsed : null;
  })();

  // =========================== 환자 설정 ==================================
  const [isAddPatientModalOpen, setIsAddPatientModalOpen] = useState(false);
  const [patients, setPatients] = useState<PatientDto[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(initialPatientId);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<number>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);
  const [patientSearchKeyword, setPatientSearchKeyword] = useState("");
  const [patientStartDate, setPatientStartDate] = useState(() => searchParams.get("start") || getDefaultPatientStartDate());
  const [patientEndDate, setPatientEndDate] = useState(() => searchParams.get("end") || getDefaultPatientEndDate());
  const [showUploadMenu, setShowUploadMenu] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const folderInputRef = useRef<HTMLInputElement>(null);

  // 서버에서 환자 목록 가져오기
  const fetchPatients = async (
      search: string | null = patientSearchKeyword,
      start: string | null = patientStartDate,
      end: string | null = patientEndDate,
  ) => {
    const res = await dicomApi.getPatients(start || null, end || null, search?.trim() || null);

    setPatients(res as PatientDto[]);
  };

  // 뷰어 갔다가 뒤로 돌아왔을 때 복원할 수 있도록 선택된 환자/날짜 범위를 URL에 반영한다.
  // 검색어는 일부러 안 싣는다(민감 정보 노출 방지). push가 아니라 replace를 쓰는 이유는,
  // push로 하면 필터를 바꿀 때마다 브라우저 히스토리가 쌓여서 뒤로가기를 눌러도 뷰어로 안
  // 가고 이전 필터 상태들을 하나씩 거슬러 올라가게 되기 때문이다.
  const updateUrlParams = (next: { patientId?: number | null; start?: string; end?: string }) => {
    const patientId = next.patientId !== undefined ? next.patientId : selectedPatientId;
    const start = next.start !== undefined ? next.start : patientStartDate;
    const end = next.end !== undefined ? next.end : patientEndDate;

    const params = new URLSearchParams();
    if (patientId !== null) {
      params.set("patientId", String(patientId));
    }
    if (start) {
      params.set("start", start);
    }
    if (end) {
      params.set("end", end);
    }

    router.replace(`${pathname}?${params.toString()}`, { scroll: false });
  };

  // 환자 선택하기 (선택한 환자는 하이라이팅되며, 환자의 스터디가 로딩된다.)
  const handleSelectPatient = (id: number) => {
    setSelectedPatientId(id);
    setCheckedStudyIds(new Set());
    setShowHiddenStudies(false);
    setStudies([]);
    void fetchStudies(id);
    updateUrlParams({ patientId: id });
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

  const requestResearchAllowApi = async () => {
    const selectedIds = Array.from(checkedStudyIds);

    if (selectedIds.length === 0) {
      return;
    }

    try {
      await dicomApi.setStudyResearch(
          selectedIds.map((id) => ({
            "study-key": id,
            "allow-research": true,
          })),
      );

      const selectedIdSet = new Set(selectedIds);
      setStudies((prev) =>
          prev.map((study) =>
              selectedIdSet.has(study["study-key"]) ? { ...study, "allow-research": true } : study,
          ),
      );
      setCheckedStudyIds(new Set());
    } catch (error) {
      console.error("연구 목적 활용 허용 설정 실패", error);
      showToast("연구 목적 활용 허용 설정에 실패했습니다.");
    }
  };

  // 연구 목적 활용 허용은 한번 누르면 데이터가 익명화되어 다운로드 가능해지고,
  // 이 익명화 자체는 되돌릴 수 없는 작업이라 실수로 누르지 않도록 확인창을 한 번 더 띄운다.
  const handleClickResearchAllow = async () => {
    const ok = await confirm({
      title: "연구 목적 데이터 활용 여부",
      message: "선택한 검사를 연구 목적으로 활용하도록 허용합니다.\n허용 시 데이터를 익명화 후 다운로드 할 수 있으며,\n이 작업은 되돌릴 수 없습니다.\n\n계속하시겠습니까?",
      confirmLabel: "허용",
      cancelLabel: "취소",
    });

    if (ok) {
      void requestResearchAllowApi();
    }
  };

  const displayedStudies = !selectedPatient
      ? []
      : studies.filter(s => s.hidden === showHiddenStudies);

  // displayedStudies는 이미 showHiddenStudies로 필터링돼 있어서, 이 값만 보고
  // 전체 선택 여부를 판단하면 일반/숨김 뷰가 서로 섞이지 않는다. 뷰를 전환할 때도
  // 이미 checkedStudyIds를 비우고 있어서(switchStudyVisibilityView) 두 뷰의
  // 선택 상태가 겹칠 일이 없다.
  const isAllDisplayedStudiesChecked =
      displayedStudies.length > 0 && displayedStudies.every((s) => checkedStudyIds.has(s["study-key"]));

  const toggleSelectAllStudies = () => {
    setCheckedStudyIds((prev) => {
      const next = new Set(prev);
      if (isAllDisplayedStudiesChecked) {
        displayedStudies.forEach((s) => next.delete(s["study-key"]));
      } else {
        displayedStudies.forEach((s) => next.add(s["study-key"]));
      }
      return next;
    });
  };

  const handlePatientSearch = () => {
    setCheckedPatientIds(new Set());
    setSelectedPatientId(null);
    setCheckedStudyIds(new Set());
    setStudies([]);
    setStudyError(null);
    void fetchPatients(patientSearchKeyword, patientStartDate, patientEndDate);
    updateUrlParams({ patientId: null, start: patientStartDate, end: patientEndDate });
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
    updateUrlParams({ patientId: null });
  };

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    const files = e.target.files;
    if (!files || files.length === 0) {
      return;
    }
    if (!selectedPatientId) {
      showToast("환자를 먼저 선택해주세요.");
      return;
    }

    const fileArray = Array.from(files);

    // 같은 파일(목록)을 다시 선택해도 change 이벤트가 발생하도록 값 초기화.
    // startUpload에는 이미 배열로 복사한 파일 목록을 넘기므로 안전하다.
    e.target.value = "";

    // 업로드는 전역 UploadContext가 들고 있는다. 페이지를 이동해도(workspace를 벗어나도)
    // 업로드 자체와 완료/실패 토스트 표시는 계속 진행된다.
    startUpload(selectedPatientId, fileArray);
  };

  // 업로드가 끝났을 때, 지금 보고 있는 환자가 그 업로드 대상과 같으면 검사 목록을 새로고침한다.
  // 환자 목록의 검사 개수 뱃지도 같이 새로고침해야 새로고침 없이 바로 반영된다.
  useEffect(() => {
    if (uploadResult && uploadResult.success) {
      if (uploadResult.patientKey === selectedPatientId) {
        void fetchStudies(selectedPatientId);
      }
      void fetchPatients();
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [uploadResult]);

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
        updateUrlParams({ patientId: null });
      }
    } catch (error) {
      console.error("환자 숨김 상태 변경 실패", error);
    }
  };


  useEffect(() => {
    // eslint-disable-next-line react-hooks/set-state-in-effect
    void fetchPatients(null, patientStartDate, patientEndDate);
    // URL에 patientId가 남아있으면(뷰어에서 뒤로 돌아온 경우) 그 환자의 검사 목록도 같이 복원한다.
    if (selectedPatientId !== null) {
      void fetchStudies(selectedPatientId);
    }
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
                          min={getMinDateInputValue()}
                          max={getMaxDateInputValue()}
                          onChange={(e) => setPatientStartDate(e.target.value)}
                          onBlur={(e) => setPatientStartDate(
                              clampDateInputValue(e.target.value, getMinDateInputValue(), getMaxDateInputValue()),
                          )}
                      />
                      <span className="text-slate-400">-</span>
                      <input
                          type="date"
                          aria-label="종료일"
                          className="flex-1 w-0 px-2 py-1.5 border border-slate-200 rounded-xl text-[12px] text-slate-800 focus:outline-none focus:border-[#14b876]"
                          value={patientEndDate}
                          min={getMinDateInputValue()}
                          max={getMaxDateInputValue()}
                          onChange={(e) => setPatientEndDate(e.target.value)}
                          onBlur={(e) => setPatientEndDate(
                              clampDateInputValue(e.target.value, getMinDateInputValue(), getMaxDateInputValue()),
                          )}
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

                  {isUploading && (
                      <div className="flex flex-1 items-center gap-2 px-4 min-w-[120px]">
                        <div className="h-2 flex-1 overflow-hidden rounded-full bg-canvas">
                          {uploadProgress >= 0 ? (
                              <div
                                  className="h-full rounded-full bg-mint-deep transition-[width] duration-200"
                                  style={{ width: `${Math.round(uploadProgress * 100)}%` }}
                              />
                          ) : (
                              <div className="h-full w-1/3 animate-pulse rounded-full bg-mint-deep" />
                          )}
                        </div>
                        <span className="shrink-0 text-xs font-semibold text-ink-soft tabular-nums">
                          {uploadProgress >= 0 ? `${Math.round(uploadProgress * 100)}%` : "업로드 중..."}
                        </span>
                      </div>
                  )}

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
                              onClick={() => {
                                void handleClickResearchAllow();
                              }}
                              disabled={checkedStudyIds.size === 0}
                              className="px-2 py-1 text-xs cursor-pointer"
                          >
                            연구 목적 활용 허용
                          </button>
                        </div>
                    )}

                    {/* 파일 업로드용 숨겨진 input */}
                    <div className="relative">
                      <button
                          type="button"
                          className="btn btn-medium cursor-pointer disabled:opacity-50 disabled:cursor-not-allowed"
                          onClick={() => setShowUploadMenu(prev => !prev)}
                          disabled={isUploading}
                      >
                        {isUploading ? "업로드 중..." : "파일 추가"}
                      </button>

                      {/* 버튼 눌렀을 때만 아래에 뜨는 작은 선택 메뉴 */}
                      {showUploadMenu && (
                          <div className="absolute mt-1 flex flex-col border rounded bg-white shadow z-10">
                            <button
                                type="button"
                                className="px-3 py-1 text-xs text-left hover:bg-gray-100 cursor-pointer"
                                onClick={() => {
                                  setShowUploadMenu(false);
                                  if (!selectedPatientId) {
                                    showToast("환자를 먼저 선택해주세요.");
                                    return;
                                  }
                                  fileInputRef.current?.click(); // 숨겨진 일반 파일 input을 대신 클릭
                                }}
                            >
                              파일로 추가
                            </button>
                            <button
                                type="button"
                                className="px-3 py-1 text-xs text-left hover:bg-gray-100 cursor-pointer"
                                onClick={() => {
                                  setShowUploadMenu(false);
                                  if (!selectedPatientId) {
                                    showToast("환자를 먼저 선택해주세요.");
                                    return;
                                  }
                                  folderInputRef.current?.click(); // 숨겨진 폴더 input을 대신 클릭
                                }}
                            >
                              폴더로 추가
                            </button>
                          </div>
                      )}

                      {/* 일반 파일 여러 개 선택용 화면엔 안 보임 */}
                      <input
                          ref={fileInputRef}
                          type="file"
                          className="hidden"
                          multiple
                          onChange={handleFileUpload}
                      />

                      {/* 폴더 전체(하위 폴더 포함) 선택용, 화면엔 안 보임 */}
                      <input
                          ref={folderInputRef}
                          type="file"
                          className="hidden"
                          multiple
                          // @ts-expect-error - React 타입 정의에 webkitdirectory가 없어서 타입 에러 무시
                          webkitdirectory=""
                          directory=""
                          onChange={handleFileUpload}
                      />
                    </div>
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
                    <span onClick={(e) => e.stopPropagation()}>
                      <input
                          type="checkbox"
                          checked={isAllDisplayedStudiesChecked}
                          onChange={toggleSelectAllStudies}
                          disabled={displayedStudies.length === 0}
                          className="cursor-pointer"
                          aria-label="전체 선택"
                      />
                    </span>
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
