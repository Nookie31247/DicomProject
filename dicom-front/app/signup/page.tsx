"use client";

import { useState } from "react";
import Link from "next/link";
import ScanVisual from "@/app/components/scan-visual/ScanVisual";
import { userApi } from "@/services/auth";

export default function SignupPage() {
  const [userRole, setUserRole] = useState<"의료진" | "연구원">("의료진");

  // 상태 관리
  const [userId, setUserId] = useState("");
  const [idMessage, setIdMessage] = useState({ text: "", color: "" });
  const [isIdChecked, setIsIdChecked] = useState(false);

  const [formData, setFormData] = useState({
    password: "",
    passwordConfirm: "",
    name: "",
    medicalLicenseNumber: ""
  });

  // 유효성 검사 메시지
  const getPwMessage = () => {
    if (!formData.passwordConfirm) return { text: "", color: "" };
    return formData.password === formData.passwordConfirm
        ? { text: "비밀번호가 일치합니다.", color: "text-mint-deep" }
        : { text: "비밀번호가 일치하지 않습니다.", color: "text-red-500" };
  };

  const handleCheckUserId = async () => {
    if (!userId) return;
    try {
      const isDuplicate = await userApi.checkId(userId);
      if (isDuplicate) {
        setIdMessage({ text: "이미 사용 중인 아이디입니다.", color: "text-red-500" });
        setIsIdChecked(false);
      } else {
        setIdMessage({ text: "사용 가능한 아이디입니다.", color: "text-mint-deep" });
        setIsIdChecked(true);
      }
    } catch {
      setIdMessage({ text: "검사 중 오류가 발생했습니다.", color: "text-red-500" });
    }
  };

  const handleSignup = async () => {
    if (!isIdChecked) return alert("아이디 중복 확인을 해주세요.");
    if (formData.password !== formData.passwordConfirm) return alert("비밀번호를 확인해주세요.");
    if (!formData.name) return alert("이름을 입력해주세요.");

    try {
      await userApi.signup({
        userId,
        password: formData.password,
        name: formData.name,
        userRole: userRole === "의료진" ? "의료진" : "연구원",
        medicalLicenseNumber: formData.medicalLicenseNumber
      });
      alert("회원가입이 완료되었습니다!");
    } catch {
      alert("회원가입에 실패했습니다.");
    }
  };

  const pwMsg = getPwMessage();

  return (
      <div className="page">
        <section className="grid flex-1 items-start grid-cols-[1.05fr_0.95fr] gap-14 pt-[72px] px-[clamp(24px,5vw,62px)] pb-24 max-[900px]:grid-cols-1">
          <div className="flex flex-col items-center gap-7 sticky top-24"><ScanVisual /></div>

          <div className="mx-auto w-full bg-paper rounded-3xl shadow-xl max-w-[560px] px-[52px] py-[52px]">
            <h1 className="font-bold text-[34px] mb-7">회원가입</h1>
            <form className="flex flex-col gap-[18px]">
              {/* 아이디 */}
              <label className="field">
                <span className="field-label">아이디</span>
                <div className="flex gap-2 w-full">
                  <input type="text" value={userId} placeholder="아이디를 입력하세요" className="flex-1"
                         onChange={(e) => { setUserId(e.target.value); setIsIdChecked(false); setIdMessage({text:"", color:""}); }} />
                  <button type="button" onClick={handleCheckUserId} className="px-4 rounded-xl font-bold bg-ink text-white whitespace-nowrap">중복확인</button>
                </div>
                {idMessage.text && <p className={`text-xs font-bold mt-1 ${idMessage.color}`}>{idMessage.text}</p>}
              </label>

              {/* 비밀번호 */}
              <label className="field"><span className="field-label">비밀번호</span>
                <input type="password" placeholder="비밀번호를 입력하세요" onChange={(e) => setFormData({...formData, password: e.target.value})} /></label>

              <label className="field"><span className="field-label">비밀번호 확인</span>
                <input type="password" placeholder="비밀번호를 다시 입력하세요" onChange={(e) => setFormData({...formData, passwordConfirm: e.target.value})} />
                {pwMsg.text && <p className={`text-xs font-bold mt-1 ${pwMsg.color}`}>{pwMsg.text}</p>}
              </label>

              <label className="field"><span className="field-label">이름</span>
                <input type="text" placeholder="이름을 입력하세요" onChange={(e) => setFormData({...formData, name: e.target.value})} /></label>

              {/* 회원 유형 */}
              <fieldset className="field">
                <legend className="field-label">회원유형</legend>
                <div className="flex gap-3">
                  <button type="button" onClick={() => setUserRole("의료진")}
                          className={`flex-1 py-3 rounded-xl border-2 font-semibold ${userRole === "의료진" ? "border-mint-deep bg-[rgba(76,255,157,0.3)]" : "border-line"}`}>의사</button>
                  <button type="button" onClick={() => setUserRole("연구원")}
                          className={`flex-1 py-3 rounded-xl border-2 font-semibold ${userRole === "연구원" ? "border-mint-deep bg-[rgba(76,255,157,0.3)]" : "border-line"}`}>연구원</button>
                </div>
              </fieldset>

              {userRole === "의료진" && (
                  <label className="field">
                    <span className="field-label">의사면허번호</span>
                    <input type="text" placeholder="면허번호를 입력하세요" onChange={(e) => setFormData({...formData, medicalLicenseNumber: e.target.value})} />
                  </label>
              )}

              <button className="btn btn-big w-full mt-2" type="button" onClick={handleSignup}>회원가입</button>
            </form>
          </div>
        </section>
      </div>
  );
}