"use client";

import { useState, useEffect } from "react";
import { useRouter, usePathname } from "next/navigation";
import Link from "next/link";
import {logout} from "@/app/api/authApi";
import { useUpload } from "@/app/context/UploadContext";

export default function NavUser() {
  const router = useRouter();
  const pathname = usePathname();
  const { cancelUpload } = useUpload();
  const navLinkClass = "relative font-semibold no-underline text-lg text-ink after:absolute after:left-0 after:bottom-[-6px] after:h-[2px] after:w-0 after:bg-mint-deep after:transition-[width] after:duration-200 after:content-[''] hover:after:w-full";

  // 로그인 여부
  const [isLogin, setIsLogin] = useState(false);
  const [username, setUsername] = useState("");

  useEffect(() => {
    const name = localStorage.getItem("username");
    if (name) {
      setUsername(name);
      setIsLogin(true);
    }
    else {
      setUsername("");
      setIsLogin(false)
    }
  }, [pathname]);

  const handleLogout = async () => {
    // 서버가 /api/dicom/** 요청의 로그인 여부를 검사하지 않기 때문에, 로그아웃해도
    // 서버가 알아서 업로드를 막아주지 않는다. 그래서 로그아웃 처리 전에 여기서
    // 직접 진행 중인 업로드를 취소한다.
    cancelUpload();
    await logout();
    localStorage.removeItem("username");
    setIsLogin(false);
    setUsername("");
    router.push("/");
  };

  return (
    <>
      {isLogin ? (
        <div className="flex items-center gap-3.5 max-[560px]:gap-2.5">
          <Link href="/mypage" className="flex items-center gap-2 no-underline hover:opacity-70 transition-opacity cursor-pointer">
            <span className="flex shrink-0 items-center justify-center rounded-full font-bold w-9 h-9 text-base text-paper bg-slate max-[560px]:hidden">
              {username.charAt(0)}
            </span>
            <span className="font-semibold text-base text-ink max-[560px]:text-sm">{username}님</span>
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
