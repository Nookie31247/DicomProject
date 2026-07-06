"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { studies } from "@/mock-data";
import * as apiDicom from "../api/dicomApi";
import AddPatientModal from "./AddPatientModal";
import dicomApi from "../api/dicomApi";


// ── 스타일 변수 ──
const wsPanelClass = "flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px]";
const wsPanelHeadClass = "flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line";
const wsHeadLeftClass = "flex min-w-0 flex-1 flex-col gap-1.25";
const wsPanelTitleClass = "m-0 font-bold text-xl text-ink tracking-[-0.01em]";
const wsSubLabelClass = "text-left font-medium text-sm text-ink-soft leading-[1.4]";
const colDescClass = "col-desc overflow-hidden whitespace-nowrap font-semibold text-ellipsis";
const colDateClass = "col-date text-ink-soft";
const colSeriesClass = "col-series text-right tabular-nums text-ink-soft";
const colImagesClass = "col-images text-right tabular-nums text-ink-soft";
const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-10.5 px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
const modalityColors: Record<string, string> = { ct: "bg-[#2563eb]", mr: "bg-[#7c3aed]", cr: "bg-[#0e7490]", us: "bg-[#c2410c]", pt: "bg-[#be185d]" };
const studyGridColumns = "30px 84px 1.6fr 1fr 1fr 70px 70px 100px";

interface patient {
  patientName: string;
  patientKey: string;
  patientSex: string;
  patientBirth: Date;
  latestStudyDateTime: Date;
  studyCount: number;
  hidden: boolean;
}

export default function WorkspaceDashboardPage() {
  const router = useRouter();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [myPatients, setMyPatients] = useState<any[]>([]);

  // 1. 서버에서 환자 목록 가져오기
  const fetchPatients = async () => {
    try {
      const response = await dicomApi.getPatients();
      setMyPatients(response.data);
    } catch (error) {
      console.error("환자 목록 로드 실패:", error);
    }
  };

  useEffect(() => {
    fetchMyPatients();
  }, []);

  // ── 상태 ──
  const [checkedStudyIds, setCheckedStudyIds] = useState<Set<string>>(new Set());
  const [showHiddenStudies, setShowHiddenStudies] = useState(false);


  // ── 로직 ──
  const handleSelectPatient = (id: string) => {
    setSelectedPatientId(id);
    setCheckedStudyIds(new Set());
  };

  const togglePatientCheck = (id: string) => {
    setCheckedPatientIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const displayedPatients = myPatients.filter((p) => {
    const kw = searchPatientKeyword.toLowerCase();
    return p.pName?.toLowerCase().includes(kw) || p.pId?.toLowerCase().includes(kw);
  });

  const selectedPatient = myPatients.find((p) => p.pId === selectedPatientId) ?? null;

  const toggleStudyCheck = (id: string) => {
    setCheckedStudyIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const displayedStudies = !selectedPatient
      ? []
      : studies.filter(s => s["patient-id"] === selectedPatientId && s.hidden === showHiddenStudies);

  return (
      <div className="page">
        <section className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)]" style={{ gridTemplateColumns: "480px 1fr" }}>


          {/* 2. 검사 목록 */}
          <section className={`${wsPanelClass} flex flex-col`}>
            <div className={wsPanelHeadClass}>
              <div className={`${wsHeadLeftClass} w-full`}>
                <div className="flex items-center justify-between w-full">
                  <h2 className={wsPanelTitleClass}>검사 목록</h2>
                  <button type="button" className="btn btn-medium">파일 추가</button>
                </div>
                {selectedPatient && <span className={wsSubLabelClass}>{selectedPatient.pname} 선택됨</span>}
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
                          <span className={`${modalityBadgeClass} ${modalityColors[(it.modality || "").toLowerCase()] || "bg-slate"}`}>
                            {it.modality || "N/A"}
                          </span>
                        </span>
                                <span className={colDescClass}>{it.description || "N/A"}</span>
                                {/* ★ 수정 포인트 3: <span className="col-body">{it.bodyPart}</span> 제거됨 */}
                                <span className={colDateClass}>{it.datetime ? it.datetime.split("T")[0] : "N/A"}</span>
                                <span className={colSeriesClass}>#{it["series-num"] ?? "N/A"}</span>
                                <span className={colImagesClass}>{it["images-num"] ?? "N/A"}</span>
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