"use client";

// DICOM 파일 업로드를 페이지 이동과 무관하게 진행시키기 위한 전역 상태.
//
// - workspace 페이지에서 startUpload를 호출하면 그 즉시 이 Provider(=RootLayout에 떠 있음,
//   페이지 이동해도 리마운트되지 않음)가 fetch를 들고 있는다. workspace 컴포넌트가
//   언마운트돼도 fetch 자체와 완료 후 처리(토스트 표시)는 계속 진행된다.
// - 새로고침/탭 닫기는 브라우저가 강제로 요청을 끊어버리기 때문에 이 방식으로도 막을 수
//   없고(불가피), 로그아웃은 명시적으로 cancelUpload()를 호출해서 취소한다.
// - 한 번에 하나의 업로드만 허용한다(동시에 여러 개 올리는 시나리오는 지원하지 않음).

import { createContext, useCallback, useContext, useMemo, useRef, useState } from "react";
import dicomApi from "@/app/api/dicomApi";
import { useToast } from "@/app/context/ToastContext";

/**
 * DICOM 업로드 작업의 결과입니다.
 */
interface UploadResult {
  patientKey: number;
  success: boolean;
  at: number;
}

/**
 * 업로드 제공자의 컨텍스트 값입니다.
 */
interface UploadContextValue {
  isUploading: boolean;
  // 0~1 사이 진행률. lengthComputable이 안 되는 드문 경우엔 -1(진행률 알 수 없음).
  uploadProgress: number;
  // 업로드가 끝날 때마다 바뀌는 값. 어떤 환자의 업로드가 끝났는지 알아야
  // 현재 보고 있는 환자의 검사 목록을 새로고침할지 판단할 수 있다.
  uploadResult: UploadResult | null;
  startUpload: (patientKey: number, files: File[]) => void;
  cancelUpload: () => void;
}

const UploadContext = createContext<UploadContextValue | null>(null);

/**
 * DICOM 파일 업로드를 관리하는 제공자입니다.
 */
export function UploadProvider({ children }: { children: React.ReactNode }) {
  const { showToast } = useToast();
  const [isUploading, setIsUploading] = useState(false);
  const [uploadProgress, setUploadProgress] = useState(0);
  const [uploadResult, setUploadResult] = useState<UploadResult | null>(null);
  const abortControllerRef = useRef<AbortController | null>(null);

  const startUpload = useCallback((patientKey: number, files: File[]) => {
    if (files.length === 0) {
      return;
    }
    if (abortControllerRef.current) {
      showToast("이미 업로드가 진행 중입니다. 완료 후 다시 시도해주세요.");
      return;
    }

    const controller = new AbortController();
    abortControllerRef.current = controller;
    setIsUploading(true);
    setUploadProgress(0);

    const formData = new FormData();
    formData.append("patientKey", patientKey.toString());
    files.forEach((file) => formData.append("files", file));

    dicomApi.uploadDicomFiles(formData, {
      signal: controller.signal,
      onProgress: (ratio) => setUploadProgress(ratio),
    })
        .then(() => {
          showToast("파일 업로드 및 태그 추출 완료!");
          setUploadResult({ patientKey, success: true, at: Date.now() });
        })
        .catch((error: unknown) => {
          // 로그아웃 등으로 명시적으로 취소한 경우엔 실패 토스트를 띄우지 않는다.
          if (error instanceof DOMException && error.name === "AbortError") {
            return;
          }
          console.error("업로드 실패:", error);
          showToast("업로드 중 문제가 발생했습니다.");
          setUploadResult({ patientKey, success: false, at: Date.now() });
        })
        .finally(() => {
          abortControllerRef.current = null;
          setIsUploading(false);
          setUploadProgress(0);
        });
  }, [showToast]);

  const cancelUpload = useCallback(() => {
    abortControllerRef.current?.abort();
    abortControllerRef.current = null;
    setIsUploading(false);
    setUploadProgress(0);
  }, []);

  const value = useMemo(
      () => ({ isUploading, uploadProgress, uploadResult, startUpload, cancelUpload }),
      [isUploading, uploadProgress, uploadResult, startUpload, cancelUpload],
  );

  return <UploadContext.Provider value={value}>{children}</UploadContext.Provider>;
}

/**
 * 업로드 컨텍스트를 사용하는 훅입니다.
 */
export function useUpload() {
  const ctx = useContext(UploadContext);
  if (!ctx) {
    throw new Error("useUpload는 UploadProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
