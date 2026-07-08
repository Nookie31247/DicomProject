"use client";

import { useEffect } from "react";

// 개별 토스트 한 개(문구 박스 + 자동 닫힘 타이머)만 담당한다.
// 화면상 위치/여러 개 쌓기는 이걸 사용하는 쪽(ToastContext의 컨테이너)이 담당한다.
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
