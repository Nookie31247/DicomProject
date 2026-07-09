"use client";

// 재사용 가능한 확인/취소 모달.
// showToast처럼 어디서든 훅 하나로 부를 수 있게, Promise로 결과(확인=true/취소=false)를
// 돌려주는 방식으로 만들었다.
//
//   const confirm = useConfirm();
//   const ok = await confirm({ message: "정말 진행할까요?" });
//   if (ok) { ... }
//
// 연구 목적 활용 허용처럼 "되돌릴 수 없는 동작" 앞에 확인을 한 번 더 받고 싶을 때뿐 아니라,
// 다른 위험한 동작(삭제, 일괄 처리 등)에도 그대로 재사용할 수 있다.

import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";

/**
 * 확인 모달의 옵션입니다.
 */
interface ConfirmOptions {
  title?: string;
  message: string;
  confirmLabel?: string;
  cancelLabel?: string;
}

/**
 * resolve 함수를 포함하는 대기 중인 확인 상태입니다.
 */
interface PendingConfirm extends ConfirmOptions {
  resolve: (result: boolean) => void;
}

/**
 * 확인 대화상자의 함수 시그니처입니다.
 */
type ConfirmFn = (options: ConfirmOptions) => Promise<boolean>;

const ConfirmContext = createContext<ConfirmFn | null>(null);

/**
 * 확인 모달 컨텍스트의 제공자입니다.
 * 애플리케이션 전체에서 재사용 가능한 프라미스 기반 확인/취소 대화상자를 활성화합니다.
 *
 * @param props - 컴포넌트 속성
 * @param props.children - 감쌀 애플리케이션 컴포넌트
 * @returns 제공자 컴포넌트
 */

/**
 * 확인 대화상자 컨텍스트의 제공자입니다.
 */
export function ConfirmProvider({ children }: { children: React.ReactNode }) {
  const [pending, setPending] = useState<PendingConfirm | null>(null);
  const confirmButtonRef = useRef<HTMLButtonElement>(null);

  const confirm = useCallback<ConfirmFn>((options) => {
    return new Promise<boolean>((resolve) => {
      setPending({ ...options, resolve });
    });
  }, []);

  const close = useCallback((result: boolean) => {
    setPending((current) => {
      current?.resolve(result);
      return null;
    });
  }, []);

  // 모달이 떠 있는 동안 Enter=확인, Esc=취소로 처리하고, 열리자마자 확인 버튼에
  // 포커스를 줘서 키보드만으로도 바로 확인/취소가 가능하게 한다.
  useEffect(() => {
    if (!pending) {
      return;
    }

    confirmButtonRef.current?.focus();

    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === "Enter") {
        e.preventDefault();
        close(true);
      } else if (e.key === "Escape") {
        e.preventDefault();
        close(false);
      }
    };

    document.addEventListener("keydown", handleKeyDown);
    return () => document.removeEventListener("keydown", handleKeyDown);
  }, [pending, close]);

  return (
      <ConfirmContext.Provider value={confirm}>
        {children}
        {pending && (
            <div className="fixed inset-0 z-[60] flex items-center justify-center bg-black/50">
              <div className="w-full max-w-sm rounded-2xl border border-line bg-paper p-6 shadow-xl">
                {pending.title && (
                    <h3 className="m-0 mb-2 text-xl font-bold text-ink">{pending.title}</h3>
                )}
                <p className="m-0 mb-6 whitespace-pre-line text-sm leading-relaxed text-ink-soft">
                  {pending.message}
                </p>
                <div className="flex justify-end gap-2">
                  <button
                      type="button"
                      onClick={() => close(false)}
                      className="rounded-xl bg-red-500 px-4 py-2 font-semibold text-paper transition-colors hover:bg-line/40 cursor-pointer"
                  >
                    {pending.cancelLabel ?? "취소"}
                  </button>
                  <button
                      ref={confirmButtonRef}
                      type="button"
                      onClick={() => close(true)}
                      className="rounded-xl bg-mint-deep px-4 py-2 font-bold text-paper transition-opacity hover:opacity-90 cursor-pointer"
                  >
                    {pending.confirmLabel ?? "확인"}
                  </button>
                </div>
              </div>
            </div>
        )}
      </ConfirmContext.Provider>
  );
}

/**
 * 확인 대화상자를 사용하는 훅입니다.
 */
/**
 * 확인 컨텍스트를 사용하는 훅입니다.
 * 참(확인) 또는 거짓(취소)으로 확인되는 프라미스를 반환하는 함수를 반환합니다.
 *
 * @returns 확인 함수
 */
export function useConfirm() {
  const ctx = useContext(ConfirmContext);
  if (!ctx) {
    throw new Error("useConfirm은 ConfirmProvider 내부에서만 사용할 수 있습니다.");
  }
  return ctx;
}
