"use client";

// RootLayout(서버 컴포넌트)에서 감싸주는 전역 클라이언트 Provider 모음.
// 여기 있는 상태들은 페이지 이동으로 리마운트되지 않으므로, 토스트/업로드 진행 상태처럼
// "페이지를 옮겨도 유지돼야 하는 것"들을 여기서 관리한다.

import { ToastProvider } from "@/app/context/ToastContext";
import { UploadProvider } from "@/app/context/UploadContext";

export default function Providers({ children }: { children: React.ReactNode }) {
  return (
      <ToastProvider>
        <UploadProvider>
          {children}
        </UploadProvider>
      </ToastProvider>
  );
}
