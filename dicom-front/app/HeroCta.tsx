"use client";

import Link from "next/link";
import { useEffect, useState } from "react";

export default function HeroCta() {
    const [isLogin, setIsLogin] = useState(false);
    const [userType, setUserType] = useState("");

    useEffect(() => {
        setIsLogin(!!localStorage.getItem("username"));
        setUserType(localStorage.getItem("userType") ?? "");
    }, []);

    const btnClass = "btn btn-big text-xl px-9 py-4.5 max-[560px]:flex-1 max-[560px]:px-5 max-[560px]:py-4 max-[560px]:text-lg";

    if (isLogin) {
        return (
            <div className="flex gap-4.5 max-[560px]:flex-wrap">
                <Link href={userType === "RESEARCHER" ? "/research" : "/workspace"} className={btnClass}>
                    홈으로 이동
                </Link>
            </div>
        );
    }

    return (
        <div className="flex gap-4.5 max-[560px]:flex-wrap">
            <Link href="/login" className={btnClass}>로그인</Link>
            <Link href="/signup" className={`${btnClass} text-paper bg-slate hover:-translate-y-0.5 hover:bg-[#0f1722]`}>시작하기</Link>
        </div>
    );
}