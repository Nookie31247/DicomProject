"use client";
import { useState } from "react";
// apiDicom 모듈 전체를 가져옵니다.
import * as apiDicom from "../api/dicomApi";

export default function AddPatientModal({ onClose, onRefresh }: { onClose: () => void, onRefresh: () => void }) {
    const [keyword, setKeyword] = useState("");
    const [results, setResults] = useState<any[]>([]);

    // 1. 환자 검색
    const handleSearch = async () => {
        try {
            // import한 apiDicom의 searchPatients 함수 사용
            const response = await apiDicom.searchPatients(keyword);
            setResults(response.data);

            console.log(response);

        } catch (error) {
            console.error("환자 검색 실패:", error);
            alert("환자 검색 중 오류가 발생했습니다.");
        }
    };

    // 2. 환자 추가 실행
    const handleAddPatient = async (pId: string) => {
        try {
            // api 객체가 아니라 apiDicom 모듈의 함수를 호출
            await apiDicom.addPatient(pId);
            alert("환자가 등록되었습니다.");
            onRefresh(); // 부모 컴포넌트의 목록 새로고침 함수 호출
            onClose();   // 모달 닫기
        } catch (error) {
            console.error("환자 등록 실패:", error);
            alert("환자 등록에 실패했습니다.");
        }
    };

    return (
        <div className="fixed inset-0 bg-black/50 flex items-center justify-center z-50">
            <div className="bg-white p-6 rounded-2xl w-[500px]">
                <h2 className="text-xl font-bold mb-4">환자 추가</h2>
                <div className="flex gap-2 mb-4">
                    <input
                        className="flex-1 border p-2 rounded-xl"
                        value={keyword}
                        onChange={(e) => setKeyword(e.target.value)}
                        onKeyDown={(e) => e.key === "Enter" && handleSearch()}
                        placeholder="이름 또는 ID 입력"
                    />
                    <button
                        onClick={handleSearch}
                        className="bg-mint-deep text-white px-4 py-2 rounded-xl font-bold"
                    >
                        검색
                    </button>
                </div>

                <div className="max-h-[300px] overflow-y-auto border-t">
                    {results.length > 0 ? results.map((p) => (
                        <div key={p.pId} className="flex justify-between items-center p-3 border-b hover:bg-gray-50">
                            <div>
                                {/* 백엔드에서 반환하는 필드명(pName, pId 등)을 정확히 사용 */}
                                <p className="font-bold">{p.pname} <span className="text-sm font-normal text-slate-500">({p.pid})</span></p>
                                <p className="text-xs text-slate-400">{p.pbirth} / {p.psex}</p>
                            </div>
                            <button
                                onClick={() => handleAddPatient(p.pid)}
                                className="text-mint-deep font-bold border border-mint-deep px-3 py-1 rounded-lg hover:bg-mint-deep hover:text-white"
                            >
                                추가
                            </button>
                        </div>
                    )) : (
                        <p className="p-4 text-center text-slate-400">검색 결과가 없습니다.</p>
                    )}
                </div>
                <button
                    onClick={onClose}
                    className="mt-4 w-full text-slate-500 font-medium hover:text-slate-800"
                >
                    닫기
                </button>
            </div>
        </div>
    );
}