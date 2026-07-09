"use client";

import { useEffect } from "react";

/**
 * 토스트 알림 컴포넌트입니다.
 * 메시지 상자를 표시하고 설정된 시간이 지나면 자동으로 닫습니다.
 * 위치 및 스태킹은 부모 컨테이너에서 관리해야 합니다.
 *
 * @param props - 컴포넌트 속성
 * @param props.message - 표시할 메시지
 * @param props.onClose - 토스트가 닫힐 때의 콜백 함수
 * @returns 토스트 컴포넌트
 */
export default function Toast({ message, onClose }: { message: string, onClose: () => void }) {
    useEffect(() => {
        const timer = setTimeout(onClose, 3000);
        return () => clearTimeout(timer);
    }, [onClose]);

    return (
        <div className="toast-fade-in-up-down">
            <div className="bg-ink text-canvas px-6 py-4 rounded-xl shadow-lg font-bold">
                {message}
            </div>
        </div>
    );
}
