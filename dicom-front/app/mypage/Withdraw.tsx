"use client"

import {useRouter} from "next/navigation";
import {type SubmitEvent, useState} from "react";
import {deleteUser, logout} from "@/app/api/authApi";

const pwInput = "w-full rounded-xl border-[1.5px] border-line bg-canvas px-4 py-[13px] text-base text-ink outline-none transition-[border-color,background] duration-150 placeholder:text-[#9aa3b2] focus:border-mint-deep focus:bg-paper";

export default function Withdraw() {
  const router = useRouter();

  // 회원 탈퇴 모달 상태
  const [isWithdrawModalOpen, setIsWithdrawModalOpen] = useState(false);
  const [withdrawPassword, setWithdrawPassword] = useState("");
  const [withdrawError, setWithdrawError] = useState("");

  function handleOpenWithdrawModal() {
    setWithdrawPassword("");
    setWithdrawError("");
    setIsWithdrawModalOpen(true);
  }

  async function handleWithdrawSubmit(e: SubmitEvent<HTMLFormElement>) {
    e.preventDefault();
    if (!withdrawPassword) {
      setWithdrawError("비밀번호를 입력해주세요.");
      return;
    }
    // 인증 로직 미구현: 실제 탈퇴는 추후 백엔드 연동.
    await deleteUser(withdrawPassword);
    setIsWithdrawModalOpen(false);
    alert("회원 탈퇴가 처리되었습니다.");
    await logout();
    localStorage.removeItem("username");
    router.push("/");
  }

  return (
    <>
      <div className="mt-9 border-t border-line pt-6.5">
        <button
          type="button"
          onClick={handleOpenWithdrawModal}
          className="cursor-pointer rounded-xl border-[1.5px] border-[#f1c7c9] bg-transparent px-5.5 py-[11px] text-base font-semibold text-[#d92d20] transition-[background,border-color] duration-150 hover:border-[#d92d20] hover:bg-[#fef3f2]"
        >
          회원탈퇴
        </button>
      </div>
      {/* ── 회원 탈퇴 모달 ── */}
      {isWithdrawModalOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/60 backdrop-blur-sm p-4">
          <div
            className="w-full max-w-md bg-paper rounded-2xl shadow-2xl overflow-hidden animate-in fade-in zoom-in-95 duration-200">
            <div className="p-6">
              <h3 className="text-xl font-bold text-ink mb-2">회원 탈퇴</h3>
              <p className="text-sm text-ink-soft mb-6 leading-relaxed">
                안전한 탈퇴를 위해 현재 비밀번호를 입력해주세요. <br/>
                탈퇴 시 모든 데이터가 즉시 삭제되며 복구할 수 없습니다.
              </p>

              <form onSubmit={handleWithdrawSubmit} className="flex flex-col gap-4">
                <input
                  type="password"
                  value={withdrawPassword}
                  onChange={(e) => setWithdrawPassword(e.target.value)}
                  placeholder="현재 비밀번호를 입력하세요"
                  className={pwInput}
                  autoFocus
                />
                {withdrawError &&
                    <p className="text-sm font-semibold text-[#d92d20] -mt-2">{withdrawError}</p>}

                <div className="mt-4 flex gap-3">
                  <button
                    type="button"
                    onClick={() => setIsWithdrawModalOpen(false)}
                    className="flex-1 cursor-pointer rounded-xl border-[1.5px] border-line bg-paper py-3 text-base font-semibold text-ink-soft transition-[background,border-color] duration-150 hover:border-ink-soft hover:text-ink"
                  >
                    취소
                  </button>
                  <button
                    type="submit"
                    className="flex-1 cursor-pointer rounded-xl border-none bg-[#d92d20] py-3 text-base font-semibold text-white transition-[background,transform] duration-150 hover:bg-[#b01e14] hover:-translate-y-px"
                  >
                    탈퇴하기
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      )}
    </>
  );
}