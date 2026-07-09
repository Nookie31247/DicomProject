"use client";

import { useState } from "react";
import dicomApi from "../api/dicomApi";

interface Props {
    onClose: () => void;
    onRefresh: () => void;
}

export default function AddPatientModal({ onClose, onRefresh }: Props) {
    const [formData, setFormData] = useState({
        name: "",
        sex: "M",
        birth: "",
    });

    const handleSubmit = async (e: React.FormEvent) => {
        e.preventDefault();
        try {
            // API 호출 (백엔드 엔드포인트는 /api/patients 등으로 가정)
            await dicomApi.addPatient(formData);
            onRefresh(); // 목록 새로고침
            onClose();   // 모달 닫기
        } catch (error) {
            console.error("환자 등록 실패", error);
            alert("환자 등록에 실패했습니다.");
        }
    };

    return (
        <div className="fixed inset-0 flex items-center justify-center bg-black/50 z-50">
            <form onSubmit={handleSubmit} className="bg-white p-6 rounded-2xl w-96 shadow-xl">
                <h2 className="text-xl font-bold mb-4">환자 추가</h2>
                <div className="flex flex-col gap-4">
                    <input className="border p-2 rounded" placeholder="이름" required onChange={(e) => setFormData({...formData, name: e.target.value})} />
                    <select className="border p-2 rounded" onChange={(e) => setFormData({...formData, sex: e.target.value})}>
                        <option value="M">남성</option>
                        <option value="F">여성</option>
                    </select>
                    <input type="date" className="border p-2 rounded" required onChange={(e) => setFormData({...formData, birth: e.target.value})} />
                </div>
                <div className="flex justify-end gap-2 mt-6">
                    <button type="button" onClick={onClose} className="px-4 py-2 bg-gray-200 rounded">취소</button>
                    <button type="submit" className="px-4 py-2 bg-mint-deep text-white rounded">등록</button>
                </div>
            </form>
        </div>
    );
}