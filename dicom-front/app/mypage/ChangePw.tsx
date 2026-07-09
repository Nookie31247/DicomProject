"use client"

import {useState, type SubmitEvent} from "react";
import {changePassword} from "@/app/api/authApi";

/**
 * 사용자의 비밀번호를 변경하는 컴포넌트입니다.
 * 비밀번호 유효성 검사 및 API 제출을 처리합니다.
 *
 * @returns 비밀번호 변경 인터페이스
 */
export default function ChangePw() {
  const pwInput = "w-full rounded-xl border-[1.5px] border-line bg-canvas px-4 py-[13px] text-base text-ink outline-none transition-[border-color,background] duration-150 placeholder:text-[#9aa3b2] focus:border-mint-deep focus:bg-paper";
  const pwFieldLabel = "text-base font-semibold text-ink";

  // 비밀번호 수정 영역 토글 + 입력 상태
  const [editingPw, setEditingPw] = useState(false);
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const [pwMessage, setPwMessage] = useState<{
    type: "error" | "ok";
    text: string;
  } | null>(null);

  // 비밀번호 필드 리셋
  function resetPwForm() {
    setPw({ current: "", next: "", confirm: "" });
    setPwMessage(null);
  }

  // 비밀번호 필드 열기
  function openEdit() {
    resetPwForm();
    setEditingPw(true);
  }

  // 비밀번호 필드 닫기
  function cancelEdit() {
    setEditingPw(false);
    resetPwForm();
  }

  // 비밀번호 변경 요청하기
  async function submitPw(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!pw.current || !pw.next || !pw.confirm) {
      setPwMessage({ type: "error", text: "모든 항목을 입력해 주세요." });
      return;
    }
    if (pw.next !== pw.confirm) {
      setPwMessage({ type: "error", text: "새 비밀번호가 일치하지 않습니다." });
      return;
    }

    try {
      await changePassword(pw.current, pw.next);
      setPwMessage({ type: "ok", text: "비밀번호가 변경되었습니다." });
      setEditingPw(false);
      setPw({ current: "", next: "", confirm: "" });
    } catch (err: unknown) {
      let message = "비밀번호 변경 중 오류가 발생했습니다.";

      if (err instanceof Error && 'status' in err) {
        const status = (err as { status: number }).status;

        console.log(status);

        if (status === 401) {
          message = "현재 비밀번호가 일치하지 않습니다.";
        } else if (status === 400) {
          message = "입력 형식이 올바르지 않습니다.";
        }
      }

      setPwMessage({ type: "error", text: message });
    }
  }

  return (
    <section>
      <h2 className="m-0 mb-4 text-xl font-bold tracking-[-0.01em] text-ink">
        비밀번호 변경
      </h2>

      {editingPw ? (
        <form className="flex max-w-105 flex-col gap-4" onSubmit={submitPw}>
          <label className="flex flex-col gap-2">
            <span className={pwFieldLabel}>현재 비밀번호</span>
            <input
              type="password"
              value={pw.current}
              onChange={(e) =>
                setPw((s) => ({ ...s, current: e.target.value }))
              }
              placeholder="현재 비밀번호를 입력하세요"
              autoComplete="current-password"
              className={pwInput}
            />
          </label>
          <label className="flex flex-col gap-2">
            <span className={pwFieldLabel}>새 비밀번호</span>
            <input
              type="password"
              value={pw.next}
              onChange={(e) =>
                setPw((s) => ({ ...s, next: e.target.value }))
              }
              placeholder="새 비밀번호를 입력하세요"
              autoComplete="new-password"
              className={pwInput}
            />
          </label>
          <label className="flex flex-col gap-2">
            <span className={pwFieldLabel}>새 비밀번호 확인</span>
            <input
              type="password"
              value={pw.confirm}
              onChange={(e) =>
                setPw((s) => ({ ...s, confirm: e.target.value }))
              }
              placeholder="새 비밀번호를 다시 입력하세요"
              autoComplete="new-password"
              className={pwInput}
            />
          </label>

          <div className="mt-1 flex gap-3 max-[560px]:flex-col">
            <button
              type="submit"
              className="cursor-pointer rounded-xl border-none bg-mint px-7 py-3 text-base font-semibold text-slate transition-[background,transform] duration-150 hover:-translate-y-px hover:bg-[#3fe88c] max-[560px]:w-full"
            >
              확인
            </button>
            <button
              type="button"
              onClick={cancelEdit}
              className="cursor-pointer rounded-xl border-[1.5px] border-line bg-paper px-7 py-3 text-base font-semibold text-ink-soft transition-[background,transform,border-color] duration-150 hover:border-ink-soft hover:text-ink max-[560px]:w-full"
            >
              취소
            </button>
          </div>
        </form>
      ) : (
        <button
          type="button"
          onClick={openEdit}
          className="cursor-pointer rounded-xl border-none bg-slate px-6 py-[13px] text-base font-semibold text-paper transition-[background,transform] duration-150 hover:-translate-y-px hover:bg-[#0f1722]"
        >
          비밀번호 수정
        </button>
      )}

      {pwMessage && (
        <p
          className={`mt-3.5 text-sm font-semibold ${
            pwMessage.type === "error"
              ? "text-[#d92d20]"
              : "text-mint-deep"
          }`}
        >
          {pwMessage.text}
        </p>
      )}
    </section>
  )
}