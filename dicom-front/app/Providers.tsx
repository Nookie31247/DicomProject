"use client";


import {ToastProvider} from "@/app/context/ToastContext";
import {UploadProvider} from "@/app/context/UploadContext";
import {ConfirmProvider} from "@/app/context/ConfirmContext";

/**
 * 루트 레이아웃을 감싸는 전역 클라이언트 사이드 컨텍스트 제공자입니다.
 * 토스트 및 업로드 진행 상황과 같이 페이지 이동 간에 유지되어야 하는 상태를 관리합니다.
 *
 * @param props - 컴포넌트 속성
 * @param props.children - 제공자로 감쌀 애플리케이션 컴포넌트
 * @returns 감싸진 제공자 컴포넌트
 */
export default function Providers({ children }: { children: React.ReactNode }) {
  return (
      <ToastProvider>
        <UploadProvider>
          <ConfirmProvider>
            {children}
          </ConfirmProvider>
        </UploadProvider>
      </ToastProvider>
  );
}
