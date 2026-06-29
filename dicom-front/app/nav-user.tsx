"use client";

import { useRouter } from "next/navigation";
import { currentUser } from "@/mock-data";

export default function NavUser() {
  const router = useRouter();
  
  const handleLogout = () => {
    // 임시 로그아웃 로직 (현재는 메인 페이지로 이동)
    router.push("/");
  };

  return (
    <div className="flex items-center gap-3.5 max-[560px]:gap-2.5">
      <span className="flex shrink-0 items-center justify-center rounded-full font-bold w-9 h-9 text-[15px] text-paper bg-slate max-[560px]:hidden">{currentUser.name.charAt(0)}</span>
      <span className="font-semibold text-base text-ink max-[560px]:text-sm">{currentUser.name}님</span>
      <button type="button" className="btn btn-medium" onClick={handleLogout}>
        로그아웃
      </button>
    </div>
  );
}
