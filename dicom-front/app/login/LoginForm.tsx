"use client";

import {login} from "@/app/api/authApi";
import {useState} from "react";
import {useRouter} from "next/navigation";

export default function LoginForm() {
  const router = useRouter();
  const [id, setId] = useState<string>("");
  const [password, setPassword] = useState<string>("");

  const doLogin = async () => {
    const loginRes = await login(id, password);
    localStorage.setItem("username", loginRes.username);
    router.push("/workspace");
  }

  return (
    <form className="flex flex-col gap-5">
      <label className="field">
        <span className="field-label">아이디</span>
        <input
          type="text"
          name="username"
          placeholder="아이디를 입력하세요"
          onChange={(e) => setId(e.target.value)}
          autoComplete="username"
        />
      </label>

      <label className="field">
        <span className="field-label">비밀번호</span>
        <input
          type="password"
          name="password"
          placeholder="비밀번호를 입력하세요"
          onChange={(e) => setPassword(e.target.value)}
          autoComplete="current-password"
        />
      </label>

      <button className="btn btn-big w-full mt-2" type="button" onClick={doLogin}>
        로그인
      </button>
    </form>
  );
}
