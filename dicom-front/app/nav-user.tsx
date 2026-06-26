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
    <div className="nav-user">
      <span className="user-avatar">{currentUser.name.charAt(0)}</span>
      <span className="user-name">{currentUser.name}님</span>
      <button type="button" className="logout-btn" onClick={handleLogout}>
        로그아웃
      </button>
    </div>
  );
}
