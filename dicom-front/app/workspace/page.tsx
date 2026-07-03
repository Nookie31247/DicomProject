"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { studies } from "@/mock-data";
import * as apiDicom from "@/app/api/apiDicom";
import AddPatientModal from "@/app/components/search-dashboard/AddPatientModal";

export default function WorkspaceDashboardPage() {
  const router = useRouter();

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [myPatients, setMyPatients] = useState<any[]>([]);

  // 1. 서버에서 환자 목록 가져오기
  const fetchMyPatients = async () => {
    try {
      const response = await apiDicom.getMyPatients();
      setMyPatients(response.data);
    } catch (error) {
      console.error("환자 목록 로드 실패:", error);
    }
  };

  useEffect(() => {
    fetchMyPatients();
  }, []);

  // ── 상태 ──
  const [selectedPatientId, setSelectedPatientId] = useState<string | null>(null);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<string>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);
  const [checkedStudyIds, setCheckedStudyIds] = useState<Set<string>>(new Set());
  const [showHiddenStudies, setShowHiddenStudies] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");

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
    const kw = searchKeyword.toLowerCase();
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
  const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-10.5 px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
  const modalityColors: Record<string, string> = { ct: "bg-[#2563eb]", mr: "bg-[#7c3aed]", cr: "bg-[#0e7490]", us: "bg-[#c2410c]", pt: "bg-[#be185d]" };
  const studyGridColumns = "30px 84px 1.6fr 1fr 1fr 70px 70px 100px";

  return (
      <div className="page">
        <section className="grid flex-1 items-stretch gap-5 pt-6 px-[clamp(20px,4vw,48px)] pb-8 min-h-0 h-[calc(100vh-93px)]" style={{ gridTemplateColumns: "480px 1fr" }}>

          {/* 1. 환자 목록 */}
          <aside className={`${wsPanelClass} flex flex-col`}>
            <div className={wsPanelHeadClass}>
              <div className={`${wsHeadLeftClass} w-full`}>
                <div className="flex items-center justify-between w-full">
                  <h2 className={wsPanelTitleClass}>환자 목록</h2>
                  <button type="button" className="btn btn-small" onClick={() => setIsModalOpen(true)}>환자 추가</button>
                </div>
                <input className="w-full mt-2 p-2 border rounded-xl text-sm" placeholder="이름 또는 ID 검색" value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)} />
              </div>
            </div>
            <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-2.5">
              {displayedPatients.map((p) => (
                  <li key={p.pId} className="flex items-center pl-2 gap-2">
                    <input
                        type="checkbox"
                        checked={checkedPatientIds.has(p.pId)}
                        onChange={() => togglePatientCheck(p.pId)}
                    />
                    <button
                        className={`${patientRowBase} ${p.pId === selectedPatientId ? patientRowActive : patientRowInactive}`}
                        onClick={() => handleSelectPatient(p.pId)}
                    >
                      <span className={patientAvatarClass}>{p.pName?.charAt(0)}</span>
                      <span className={patientMainClass}>
            <span className={patientNameClass}>{p.pName}</span>
            <span className={patientSubClass}>
                {p.pSex} · {p.pBirth?.split('T')[0] || "정보 없음"} ·
                최근 진료: {p.pTime?.split('T')[0] || "기록 없음"}
            </span>
          </span>
                      <span className={`${patientBadgeBase} ${p.pId === selectedPatientId ? patientBadgeActive : patientBadgeDefault}`}>
            {p.pStudyCount}
          </span>
                    </button>
                  </li>
              ))}
            </ul>
          </aside>

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
            <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
              <div className="grid items-center gap-2.5 shrink-0 font-bold py-3 px-5 text-xs text-ink-soft bg-canvas border-b border-line" style={{ gridTemplateColumns: studyGridColumns }}>
                <span></span><span>모달리티</span><span className={colDescClass}>검사 설명</span><span className="col-body">검사 부위</span><span className={colDateClass}>검사 일자</span><span className={colSeriesClass}>시리즈</span><span className={colImagesClass}>영상 수</span><span>연구 활용</span>
              </div>
              <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
                {displayedStudies.map((it) => (
                    <li key={it["study-key"]}>
                      <div className="study-row grid items-center gap-2.5 w-full cursor-pointer p-3.5 border-b" style={{ gridTemplateColumns: studyGridColumns }}>
                        <input type="checkbox" checked={checkedStudyIds.has(it["study-key"])} onChange={() => toggleStudyCheck(it["study-key"])} />
                        <span className={modalityBadgeClass + " " + modalityColors[it.modality.toLowerCase()]}>{it.modality}</span>
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
        {isModalOpen && <AddPatientModal onClose={() => setIsModalOpen(false)} onRefresh={fetchMyPatients} />}
      </div>
  );
}