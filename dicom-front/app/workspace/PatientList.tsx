"use client"

import {useEffect, useState} from "react";
import AddPatientModal from "@/app/workspace/AddPatientModal";
import dicomApi from "@/app/api/dicomApi";

const wsPanelClass = "flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px]";
const wsPanelHeadClass = "flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line";
const wsHeadLeftClass = "flex min-w-0 flex-1 flex-col gap-1.25";
const wsPanelTitleClass = "m-0 font-bold text-xl text-ink tracking-[-0.01em]";
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

interface patients {

}

export default function PatientList() {

  const [isModalOpen, setIsModalOpen] = useState(false);
  const [myPatients, setMyPatients] = useState<any[]>([]);
  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [checkedIds, setCheckedIds] = useState<Set<string>>(new Set());
  const [showHiddenPatients, setShowHiddenPatients] = useState(false);
  const [searchKeyword, setSearchKeyword] = useState("");

  const handleSelect = (id: string) => {
    setSelectedId(id);
    setCheckedIds(new Set());
  };

  const toggleCheck = (id: string) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  useEffect(() => {
    const res = await dicomApi.getPatients(null, null, searchKeyword);
    for (let i = 0; i < res.length; i++) {

    }
    setMyPatients(patients);
  }, []);

  const patients =

  return(
    <aside className={`${wsPanelClass} flex flex-col`}>
      <div className={wsPanelHeadClass}>
        <div className={`${wsHeadLeftClass} w-full`}>
          <div className="flex items-center justify-between w-full">
            <h2 className={wsPanelTitleClass}>환자 목록</h2>
            <button type="button" className="btn btn-small" onClick={() => setIsModalOpen(true)}>환자 추가</button>
          </div>
          <input
            className="w-full mt-2 p-2 border rounded-xl text-sm"
            placeholder="이름으로 검색하기"
            value={searchKeyword} onChange={(e) => setSearchKeyword(e.target.value)}
          />
        </div>
      </div>
      <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-2.5">
        {displayedPatients.map((p) => (
          <li key={p.pId} className="flex items-center pl-2 gap-2">
            <input
                type="checkbox"
                checked={checkedIds.has(p.pId)}
                onChange={() => toggleCheck(p.pId)}
            />
            <button
                className={`${patientRowBase} ${p.pId === selectedId ? patientRowActive : patientRowInactive}`}
                onClick={() => handleSelect(p.pId)}
            >
              <span className={patientAvatarClass}>{p.pName?.charAt(0)}</span>
              <span className={patientMainClass}>
                <span className={patientNameClass}>{p.pName}</span>
                <span className={patientSubClass}>
                  {p.pSex} · {p.pBirth?.split('T')[0] || "정보 없음"} · 최근 진료: {p.pTime?.split('T')[0] || "기록 없음"}
                </span>
              </span>
            <span className={`${patientBadgeBase} ${p.pId === selectedId ? patientBadgeActive : patientBadgeDefault}`}>
              {p.pStudyCount}
            </span>
            </button>
          </li>
        ))}
      </ul>
      {isModalOpen && <AddPatientModal onClose={() => setIsModalOpen(false)} onRefresh={fetchMyPatients} />}
    </aside>
  );
}