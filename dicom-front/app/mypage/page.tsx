"use client";

import { useState, type FormEvent } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { currentUser } from "@/mock-data";

const roleLabel = (r: "doctor" | "researcher") =>
  r === "doctor" ? "의사" : "연구자";

export default function MyPage() {
  const router = useRouter();

  // 비밀번호 수정 영역 토글 + 입력 상태
  const [editingPw, setEditingPw] = useState(false);
  const [pw, setPw] = useState({ current: "", next: "", confirm: "" });
  const [pwMessage, setPwMessage] = useState<{
    type: "error" | "ok";
    text: string;
  } | null>(null);

  function resetPwForm() {
    setPw({ current: "", next: "", confirm: "" });
    setPwMessage(null);
  }

  function openEdit() {
    resetPwForm();
    setEditingPw(true);
  }

  function cancelEdit() {
    setEditingPw(false);
    resetPwForm();
  }

  function submitPw(e: FormEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!pw.current || !pw.next || !pw.confirm) {
      setPwMessage({ type: "error", text: "모든 항목을 입력해 주세요." });
      return;
    }
    if (pw.next !== pw.confirm) {
      setPwMessage({ type: "error", text: "새 비밀번호가 일치하지 않습니다." });
      return;
    }
    // 인증 미구현: 실제 변경은 추후 백엔드 연동.
    setEditingPw(false);
    setPw({ current: "", next: "", confirm: "" });
    setPwMessage({ type: "ok", text: "비밀번호가 변경되었습니다. (목업)" });
  }

  function handleWithdraw() {
    // 인증 미구현: 실제 탈퇴는 추후 백엔드 연동.
    const ok = window.confirm(
      "정말 탈퇴하시겠습니까? 이 작업은 되돌릴 수 없습니다."
    );
    if (ok) router.push("/");
  }

  // 비밀번호 입력 필드 공통 스타일
  const pwInput =
    "w-full rounded-xl border-[1.5px] border-line bg-canvas px-4 py-[13px] text-base text-ink outline-none transition-[border-color,background] duration-150 placeholder:text-[#9aa3b2] focus:border-mint-deep focus:bg-paper";
  const pwFieldLabel = "text-base font-semibold text-ink";

  return (
    <div className="page">
      {/* ───────────── My page ───────────── */}
      <section className="flex flex-1 justify-center px-[clamp(20px,5vw,62px)] pt-11 pb-20 max-[560px]:px-4 max-[560px]:pb-14 max-[560px]:pt-7">
        <div className="w-full max-w-[720px] rounded-3xl border border-line bg-paper px-[clamp(26px,4vw,52px)] py-11 shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-[560px]:px-5.5 max-[560px]:py-8">
          {/* 제목 위 뒤로가기 버튼 */}
          <Link
            href="/workspace"
            className="mb-4.5 flex h-9.5 w-9.5 items-center justify-center rounded-[10px] border border-line bg-paper text-xl leading-none text-ink-soft no-underline transition-[background,color,border-color] duration-150 hover:border-mint-deep hover:bg-canvas hover:text-ink"
            aria-label="워크스페이스로 돌아가기"
            title="뒤로가기"
          >
            ←
          </Link>
          <h1 className="m-0 mb-1.5 text-3xl font-bold tracking-[-0.01em] text-ink max-[560px]:text-[26px]">
            마이페이지
          </h1>
          <p className="m-0 mb-8 text-lg text-ink-soft">
            {currentUser.name}님의 계정 정보입니다.
          </p>

          {/* ── 회원 정보 ── */}
          <dl className="m-0 mb-10 flex flex-col divide-y divide-line overflow-hidden rounded-2xl border border-line">
            <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
              <dt className="text-base font-semibold text-ink-soft">아이디</dt>
              <dd className="m-0 text-base font-semibold text-ink">
                {currentUser.userId}
              </dd>
            </div>
            <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
              <dt className="text-base font-semibold text-ink-soft">회원유형</dt>
              <dd className="m-0 text-base font-semibold text-ink">
                <span className="inline-flex items-center rounded-full bg-[rgba(76,255,157,0.18)] px-[13px] py-1 text-sm font-bold text-mint-deep">
                  {roleLabel(currentUser.role)}
                </span>
              </dd>
            </div>
            {currentUser.role === "doctor" && (
              <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
                <dt className="text-base font-semibold text-ink-soft">
                  의사 면허번호
                </dt>
                <dd className="m-0 text-base font-semibold text-ink">
                  {currentUser.licenseNumber}
                </dd>
              </div>
            )}
          </dl>

          {/* ── 비밀번호 변경 ── */}
          <section>
            <h2 className="m-0 mb-4 text-xl font-bold tracking-[-0.01em] text-ink">
              비밀번호 변경
            </h2>

            {editingPw ? (
              <form
                className="flex max-w-[420px] flex-col gap-4"
                onSubmit={submitPw}
              >
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

          {/* ── 회원 탈퇴 ── */}
          <div className="mt-9 border-t border-line pt-6.5">
            <button
              type="button"
              onClick={handleWithdraw}
              className="cursor-pointer rounded-xl border-[1.5px] border-[#f1c7c9] bg-transparent px-5.5 py-[11px] text-base font-semibold text-[#d92d20] transition-[background,border-color] duration-150 hover:border-[#d92d20] hover:bg-[#fef3f2]"
            >
              회원탈퇴
            </button>
          </div>
        </div>
      </section>
    </div>
  );
}
