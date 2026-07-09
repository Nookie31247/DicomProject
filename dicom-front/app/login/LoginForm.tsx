"use client";

import { useEffect, useRef, useState } from "react";
import { Eye, EyeOff } from "lucide-react";
import { useRouter } from "next/navigation";
import { login } from "@/app/api/authApi";

export default function LoginForm() {
    const router = useRouter();
    const [id, setId] = useState<string>("");
    const [password, setPassword] = useState<string>("");
    const [showPassword, setShowPassword] = useState(false);
    const [errorMsg, setErrorMsg] = useState<string>("");

    // 비밀번호 입력창을 가리키는 ref
    const passwordInputRef = useRef<HTMLInputElement>(null);

    // 이미 로그인된 사용자가 /login에 직접 접근한 경우, 로그인 폼을 보여주는 대신 각자의 홈(연구원은 /research, 그 외는 /workspace)으로 바로 보낸다
    useEffect(() => {
        const username = localStorage.getItem("username");
        if (username) {
            const userType = localStorage.getItem("userType");
            router.replace(userType === "RESEARCHER" ? "/research" : "/workspace");
        }
    }, [router]);

    const doLogin = async () => {
        try {
            setErrorMsg("");
            const loginRes = await login(id, password);
            localStorage.setItem("username", loginRes.username);
            localStorage.setItem("userType", loginRes.userType); // NavUser 배지, research 페이지 분기 등에서 사용
            router.push(loginRes.userType === "RESEARCHER" ? "/research" : "/workspace");
        } catch (err: unknown) {
            setErrorMsg("아이디 혹은 비밀번호가 일치하지 않습니다.");
        }
    };

    // 2. 아이디 칸 핸들러 (엔터 시 비밀번호 칸으로 포커스 이동)
    const handleIdKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter") {
            e.preventDefault();
            passwordInputRef.current?.focus();
        }
    };

    // 3. 비밀번호 칸 핸들러 (엔터 시 로그인 실행)
    const handlePwKeyDown = (e: React.KeyboardEvent) => {
        if (e.key === "Enter") {
            e.preventDefault();
            doLogin();
        }
    };

    return (
        <form className="flex flex-col gap-5">
            <label className="field">
                <span className="field-label">아이디</span>
                <input
                    type="text"
                    placeholder="아이디를 입력하세요"
                    onChange={(e) => setId(e.target.value)}
                    onKeyDown={handleIdKeyDown} // 이벤트 추가
                    autoComplete="username"
                />
            </label>

            <label className="field">
                <span className="field-label">비밀번호</span>
                <div className="relative flex items-center">
                    <input
                        ref={passwordInputRef} // ref 연결
                        type={showPassword ? "text" : "password"}
                        placeholder="비밀번호를 입력하세요"
                        className="w-full pr-10"
                        onChange={(e) => setPassword(e.target.value)}
                        onKeyDown={handlePwKeyDown} // 이벤트 추가
                        autoComplete="current-password"
                    />
                    <button
                        type="button"
                        className="absolute right-3 text-ink-soft hover:text-ink"
                        onClick={() => setShowPassword(!showPassword)}
                    >
                        {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
                    </button>
                </div>
            </label>

            {/* 로그인 실패 시 메시지 출력 */}
            {errorMsg && (
                <span className="text-xs font-bold text-red-500 mt-[-10px]">
                    {errorMsg}
                </span>
            )}
            <button className="btn btn-big w-full mt-2" type="button" onClick={doLogin}>
                로그인
            </button>
        </form>
    );
}