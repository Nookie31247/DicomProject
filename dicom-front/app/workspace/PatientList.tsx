"use client"

import {useEffect, useState} from "react";
import dicomApi from "@/app/api/dicomApi";





export default function PatientList() {



  const handleSelect = (id: number) => {
    setSelectedId(id);
    setCheckedIds(new Set());
  };

  const toggleCheck = (id: number) => {
    setCheckedIds((prev) => {
      const next = new Set(prev);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  };

  const fetchPatients = async (search: string | null = searchKeyword) => {
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

  useEffect(() => {
    fetchPatients(null);
  }, []);


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
        {patients.map((p) => (
          <li key={p.key} className="flex items-center pl-2 gap-2">
            <input
                type="checkbox"
                checked={checkedIds.has(p.key)}
                onChange={() => toggleCheck(p.key)}
            />
            <button
                className={`${patientRowBase} ${p.key === selectedId ? patientRowActive : patientRowInactive}`}
                onClick={() => handleSelect(p.key)}
            >
              <span className={patientAvatarClass}>{p.name?.charAt(0)}</span>
              <span className={patientMainClass}>
                <span className={patientNameClass}>{p.name}</span>
                <span className={patientSubClass}>
                  {p.sex} · {p.birth?.split('T')[0] || "정보 없음"} · 최근 진료: {p.latestStudy?.split('T')[0] || "기록 없음"}
                </span>
              </span>
            <span className={`${patientBadgeBase} ${p.key === selectedId ? patientBadgeActive : patientBadgeDefault}`}>
              {p.studyCount}
            </span>
            </button>
          </li>
        ))}
      </ul>
    </aside>
  );
}