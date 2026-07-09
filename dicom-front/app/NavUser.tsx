"use client";

import {useEffect, useState} from "react";
import {usePathname, useRouter} from "next/navigation";
import Link from "next/link";
import {logout} from "@/app/api/authApi";
import {useUpload} from "@/app/context/UploadContext";
import {clearStoredAuth, getStoredAccountType} from "@/app/api/ApiFetch";

/**
 * 사용자 정보 및 로그아웃 기능을 표시하는 내비게이션 사용자 컴포넌트입니다.
 * 또한 로그인 상태 동기화를 처리합니다.
 *
 * @returns 내비게이션 사용자 인터페이스
 */
export default function NavUser() {
  const router = useRouter();
  const pathname = usePathname();
  const { cancelUpload } = useUpload();
  const navLinkClass = "relative font-semibold no-underline text-lg text-ink after:absolute after:left-0 after:bottom-[-6px] after:h-[2px] after:w-0 after:bg-mint-deep after:transition-[width] after:duration-200 after:content-[''] hover:after:w-full";

  // 로그인 여부
  const [isLogin, setIsLogin] = useState(false);
  const [username, setUsername] = useState("");
  const [userType, setUserType] = useState(""); // "연구원" 배지 표시용

  useEffect(() => {
    const syncAuthState = () => {
      const name = localStorage.getItem("username");
      const type = localStorage.getItem("userType");
      if (name) {
        setUsername(name);
        setUserType(type ?? "");
        setIsLogin(true);
      } else {
        setUsername("");
        setUserType("");
        setIsLogin(false);
      }
    };

    syncAuthState();
    window.addEventListener("auth-state-changed", syncAuthState);
    return () => window.removeEventListener("auth-state-changed", syncAuthState);
  }, [pathname]);

  const handleLogout = async () => {
    // 서버가 /api/medical/dicom/** 요청의 로그인 여부를 검사하지 않기 때문에, 로그아웃해도
    // 서버가 알아서 업로드를 막아주지 않는다. 그래서 로그아웃 처리 전에 여기서
    // 직접 진행 중인 업로드를 취소한다.
    cancelUpload();
    const accountType = getStoredAccountType();
    try {
      await logout(accountType);
    } catch (error) {
      const status = typeof error === "object" && error !== null && "status" in error
        ? (error as { status?: unknown }).status
        : null;

      if (status !== 401 && status !== 403) {
        console.error("Logout failed", error);
      }
    } finally {
      clearStoredAuth();
      setIsLogin(false);
      setUsername("");
      setUserType("");
      router.push("/");
    }
  };

  return (
    <>
      {isLogin ? (
        <div className="flex items-center gap-3.5 max-[560px]:gap-2.5">
          <Link href="/mypage" className="flex items-center gap-2 no-underline hover:opacity-70 transition-opacity cursor-pointer">
            <span className="flex shrink-0 items-center justify-center rounded-full font-bold w-9 h-9 text-base text-paper bg-slate max-[560px]:hidden">
              {username.charAt(0)}
            </span>
            <span className="font-semibold text-base text-ink max-[560px]:text-sm">
              {username}님
              {userType === "RESEARCHER" && (
                <span className="ml-1.5 align-middle text-[11px] font-bold text-mint-deep bg-mint/20 px-1.5 py-0.5 rounded-full">
                  연구원
                </span>
              )}
            </span>
          </Link>
          <button type="button" className="btn btn-medium" onClick={handleLogout}>
            로그아웃
          </button>
        </div>
      ) : (
        <nav className="flex gap-10 max-[560px]:gap-5.5">
          <Link href="/#how-it-works" className={navLinkClass}>사용방법</Link>
          <Link href="/#faq" className={navLinkClass}>FAQ</Link>
        </nav>
      )}
    </>
  );
}
