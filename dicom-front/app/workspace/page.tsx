"use client";

import { useState, useEffect } from "react";
import { useRouter } from "next/navigation";
import { studies } from "@/mock-data";
import AddPatientModal from "@/app/components/search-dashboard/AddPatientModal";
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
const colDescClass = "col-desc overflow-hidden whitespace-nowrap font-semibold text-ellipsis";
const colDateClass = "col-date text-ink-soft";
const colSeriesClass = "col-series text-right tabular-nums text-ink-soft";
const colImagesClass = "col-images text-right tabular-nums text-ink-soft";
const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-10.5 px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
const modalityColors: Record<string, string> = { ct: "bg-[#2563eb]", mr: "bg-[#7c3aed]", cr: "bg-[#0e7490]", us: "bg-[#c2410c]", pt: "bg-[#be185d]" };
const studyGridColumns = "30px 84px 1.6fr 1fr 1fr 70px 70px 100px";

interface patientType {
  name: string;
  key: number;
  sex: string;
  birth: string;
  latestStudy: string;
  studyCount: number;
  hidden: boolean;
}

interface studyType {
  key: number;
  description: string;
  datetime: string;
  seriesNum: number;
  imagesNum: number;
  allowResearch: boolean;
  hidden: boolean;
}

export default function WorkspaceDashboardPage() {
  const router = useRouter();

  // =========================== 환자 설정 ==================================
  const [isAddPatientModalOpen, setIsAddPatientModalOpen] = useState(false);
  const [patients, setPatients] = useState<patientType[]>([]);
  const [selectedPatientId, setSelectedPatientId] = useState<number | null>(null);
  const [checkedPatientIds, setCheckedPatientIds] = useState<Set<number>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);
  const [patientSearchKeyword, setPatientSearchKeyword] = useState("");

  // 서버에서 환자 목록 가져오기
  const fetchPatients = async (search: string | null = patientSearchKeyword) => {
      const res = await dicomApi.getPatients(null, null, search);

      const pList: patientType[] = res.map((item) => ({
        name: item["patient-name"],
        key: item["patient-key"],
        sex: item["patient-sex"],
        birth: item["patient-birth"],
        latestStudy: item["latest-study-datetime"],
        studyCount: item["study-count"],
        hidden: item["hidden"],
      }));

      setPatients(pList);
    };

  // 환자 선택하기 (선택한 환자는 하이라이팅되며, 환자의 스터디가 로딩된다.)
  const handleSelectPatient = (id: number) => {
    setSelectedPatientId(id);
    setCheckedStudyIds(new Set());
  };

  // 환자 체크박스로 선택하기
  const togglePatientCheck = (id: number) => {
    setCheckedPatientIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const selectedPatient = patients.find((p) => p.key === selectedPatientId) ?? null;

  // ================================== 스터디 영역 =========================================
  const [studies, setStudies] = useState<studyType[]>([]);
  const [checkedStudyIds, setCheckedStudyIds] = useState<Set<string>>(new Set());
  const [showHiddenStudies, setShowHiddenStudies] = useState(false);

  // 서버에서 스터디 목록 가져오기
  const fetchStudies = async () => {
    const res = await dicomApi.getStudies(1, null, null, null);

    const sList: studyType[] = res.map((item) => ({
      key: item["study-key"],
      description: item["description"],
      datetime: item["datetime"],
      seriesNum: item["series-num"],
      imagesNum: item["images-num"],
      allowResearch: item["allow-research"],
      hidden: item["hidden"],
    }));

    setStudies(sList);
  };

  const toggleStudyCheck = (id: number) => {
    setCheckedStudyIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const displayedStudies = !selectedPatient
      ? []
      : studies.filter(s => s["patient-id"] === selectedPatientId && s.hidden === showHiddenStudies);


  useEffect(() => {
    fetchPatients(null);
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
                <input className="w-full mt-2 p-2 border rounded-xl text-sm" placeholder="이름 또는 ID 검색" value={patientSearchKeyword} onChange={(e) => setPatientSearchKeyword(e.target.value)} />
              </div>
            </div>
            <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-2.5">
              {patients.map((p) => (
                  <li key={p.key} className="flex items-center pl-2 gap-2">
                    <input
                        type="checkbox"
                        checked={checkedPatientIds.has(p.key)}
                        onChange={() => togglePatientCheck(p.key)}
                    />
                    <button
                        className={`${patientRowBase} ${p.key === selectedPatientId ? patientRowActive : patientRowInactive}`}
                        onClick={() => handleSelectPatient(p.key)}
                    >
                      <span className={patientAvatarClass}>{p.name?.charAt(0)}</span>
                      <span className={patientMainClass}>
            <span className={patientNameClass}>{p.name}</span>
            <span className={patientSubClass}>
                {p.sex} · {p.birth?.split('T')[0] || "정보 없음"} ·
                최근 진료: {p.latestStudy?.split('T')[0] || "기록 없음"}
            </span>
          </span>
                      <span className={`${patientBadgeBase} ${p.key === selectedPatientId ? patientBadgeActive : patientBadgeDefault}`}>
            {p.studyCount}
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
                {selectedPatient && <span className={wsSubLabelClass}>{selectedPatient.name} 선택됨</span>}
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
          </section>
        </section>
        {/*{isAddPatientModalOpen && <AddPatientModal onClose={() => setIsAddPatientModalOpen(false)} onRefresh={fetchMyPatients} />}*/}
      </div>
  );
}