"use client";

import { useState } from "react";
import {Download, ShieldCheck, FileText, AlertCircle, Calendar} from 'lucide-react';

export default function ResearchDataPage() {
    // 상태: 예시 데이터
    const [selectedStudies, setSelectedStudies] = useState<any[]>([]); // 선택된 연구 자료들
    const [dateRange, setDateRange] = useState({ start: "", end: "" });
    const [anonymizationOptions, setAnonymizationOptions] = useState({
        removeName: true,
        removeId: true,
        removeBirth: true
    });

    // 날짜 필터링 로직 예시
    const filteredStudies = selectedStudies.filter(study => {
        if (!dateRange.start || !dateRange.end) return true;
        return study.datetime >= dateRange.start && study.datetime <= dateRange.end;
    });

    const gridCols = "40px 80px 2fr 1fr 1fr 100px";

    return (
        <div className="page p-8 max-w-[1200px] mx-auto">
            {/* 상단 헤더 영역에 검색 필터 통합 */}
            <div className="flex justify-between items-end mb-8">
                <div>
                    <h1 className="text-2xl font-bold text-ink">연구 자료 다운로드</h1>
                    <p className="text-ink-soft mt-1 mr-5">익명화된 연구 자료를 선택해 다운로드할 수 있습니다.</p>
                </div>

                {/* 날짜 검색 영역 */}
                <div className="flex items-center gap-2 bg-paper border border-line p-2 rounded-xl">
                    <Calendar size={18} className="text-ink-soft ml-2" />
                    <input type="date" className="bg-transparent text-sm p-1 outline-none"
                           onChange={(e) => setDateRange(s => ({...s, start: e.target.value}))} />
                    <span className="text-ink-soft">~</span>
                    <input type="date" className="bg-transparent text-sm p-1 outline-none"
                           onChange={(e) => setDateRange(s => ({...s, end: e.target.value}))} />
                </div>
            </div>

            <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
                {/* 왼쪽: 선택된 리스트 (기존 Workspace 스타일 활용) */}
                <div className="lg:col-span-2 bg-paper border border-line rounded-[20px] p-6">
                    <h2 className="text-lg font-bold mb-4 flex items-center gap-2">
                        <FileText size={20} /> 선택된 자료 목록
                    </h2>

                    <div className="grid font-bold text-xs text-ink-soft bg-canvas p-3 rounded-lg mb-2" style={{ gridTemplateColumns: gridCols }}>
                        <span></span><span>유형</span><span>검사 설명</span><span>검사 일자</span><span>영상 수</span><span>상태</span>
                    </div>

                    <ul className="space-y-2 max-h-[500px] overflow-y-auto">
                        {/* 목록 아이템 매핑 영역 */}
                        {selectedStudies.length === 0 && (
                            <li className="p-8 text-center text-ink-soft text-sm">선택된 자료가 없습니다.</li>
                        )}
                    </ul>
                </div>

                {/* 오른쪽: 익명화 설정 및 액션 패널 */}
                <div className="bg-paper border border-line rounded-[20px] p-6 h-fit">
                    <h2 className="text-lg font-bold mb-6 flex items-center gap-2">
                        <ShieldCheck size={20} /> 익명화 설정
                    </h2>

                    <div className="grid grid-cols-2 gap-3 text-xs mb-4">
                        {[
                            { label: "환자 정보", desc: "익명으로 데이터 치환" },
                            { label: "검사 정보", desc: "익명화 및 데이터의 일반화" },
                            { label: "UID 속 장비번호·날짜", desc: "시퀀스 내부까지 순회" },
                            { label: "파일 정보", desc: "정보 마스킹 처리 및 규칙 변경" }
                        ].map((rule) => (
                            <div key={rule.label} className="bg-paper p-3 rounded-lg border border-line">
                                <div className="font-bold text-ink mb-0.5">{rule.label}</div>
                                <div className="text-ink-soft">{rule.desc}</div>
                            </div>
                        ))}
                    </div>

                    <div className="p-4 bg-canvas rounded-xl mb-6 text-xs text-ink-soft leading-relaxed flex gap-2">
                        <AlertCircle size={16} className="shrink-0 text-mint-deep" />
                        익명화된 자료는 DICOM 헤더 내 개인식별정보가 제거된 후 다운로드됩니다.
                    </div>

                    <button className="w-full btn btn-big bg-mint text-slate font-bold flex items-center justify-center gap-2">
                        <Download size={20} /> 다운로드 시작
                    </button>
                </div>
            </div>
        </div>
    );
}