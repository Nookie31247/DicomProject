"use client";

// 전역 토스트 상태 관리.
// RootLayout(app/layout.tsx)에 한 번만 Provider를 올려두면, 어느 페이지로 이동하든
// (workspace, signup, viewer 등) 컴포넌트가 리마운트/언마운트되는 것과 무관하게
// 같은 토스트 큐를 공유하게 된다. 그래서 "업로드 중 다른 페이지로 이동해도
// 완료/실패 토스트가 뜬다" 같은 요구사항을 만족할 수 있다.

import { createContext, useCallback, useContext, useMemo, useState } from "react";
import Toast from "@/app/components/message-box/Toast";

/**
 * 단일 토스트 메시지를 나타냅니다.
 */
interface ToastItem {
  id: number;
  message: string;
}

/**
 * 토스트 제공자의 컨텍스트 값입니다.
 */
interface ToastContextValue {
  showToast: (message: string) => void;
}

const ToastContext = createContext<ToastContextValue | null>(null);

let nextToastId = 1;

/**
 * 토스트 컨텍스트의 제공자입니다.
 */
export function ToastProvider({ children }: { children: React.ReactNode }) {
  const [toasts, setToasts] = useState<ToastItem[]>([]);

  const showToast = useCallback((message: string) => {
    const id = nextToastId++;
    setToasts((prev) => [...prev, { id, message }]);
  }, []);

  const closeToast = useCallback((id: number) => {
    setToasts((prev) => prev.filter((t) => t.id !== id));
  }, []);

  const value = useMemo(() => ({ showToast }), [showToast]);

  return (
      <ToastContext.Provider value={value}>
        {children}
        {/* 여러 개가 동시에 떠도 겹치지 않도록 아래에서 위로 쌓는다 */}
        <div className="fixed bottom-6 right-6 z-50 flex flex-col items-end gap-2.5">
          {toasts.map((t) => (
              <Toast key={t.id} message={t.message} onClose={() => closeToast(t.id)} />
          ))}
        </div>
      </ToastContext.Provider>
  );
}

/**
 * 토스트 컨텍스트를 사용하는 훅입니다.
 */
export function useToast() {
  const ctx = useContext(ToastContext);
  if (!ctx) {
    throw new Error("useToast는 ToastProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
