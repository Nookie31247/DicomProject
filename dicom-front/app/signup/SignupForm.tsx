"use client";

import { useState, useRef } from "react";
import { checkId, signup } from "@/app/api/authApi";
import Toast from "@/app/components/message-box/Toast";
import { useRouter } from "next/navigation";
import { Eye, EyeOff } from 'lucide-react';

export default function SignupForm() {
  const router = useRouter();

  // 메시지 띄울 때 쓰는 토스트
  const [toastMsg, setToastMsg] = useState<string | null>(null);

  // 포커스 제어를 위한 Ref들
  const passwordInputRef = useRef<HTMLInputElement>(null);
  const confirmPasswordInputRef = useRef<HTMLInputElement>(null);
  const nameInputRef = useRef<HTMLInputElement>(null);

  const [showPassword, setShowPassword] = useState(false);
  const [showPasswordCheck, setShowPasswordCheck] = useState(false);

  const pillBase = "flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl border-2 px-4 py-[13px] text-base transition-[border-color,background,color] duration-150";
  const pillActive = "border-mint-deep bg-[rgba(76,255,157,0.3)] text-ink font-bold";
  const pillIdle = "border-line bg-canvas text-ink-soft font-semibold";
  const hiddenRadio = "pointer-events-none absolute m-0 h-px w-px border-0 p-0 opacity-0 [clip:rect(0_0_0_0)] [clip-path:inset(50%)]";

  const [form, setForm] = useState({ id: "", password: "", confirmPassword: "", username: "" });
  const [memberType, setMemberType] = useState<"의료진" | "연구원">("의료진");

  const [checkStatus, setCheckStatus] = useState<{ msg: string, isChecked: boolean, isDuplicated: boolean }>({
    msg: "", isChecked: false, isDuplicated: false
  });

  const validateId = (id: string) => {
    if (id.length === 0) return "";
    if (id.length < 4 || id.length > 25) return "아이디는 4자 이상 25자 이하로 입력해주세요.";
    if (!/^[a-zA-Z0-9]+$/.test(id)) return "아이디는 영문과 숫자만 가능합니다.";
    if (!/[a-zA-Z]/.test(id)) return "영문이 최소 1자 포함되어야 합니다.";
    return "사용 가능한 아이디입니다.";
  };

  const validatePassword = (pw: string) => {
    if (pw.length === 0) return { msg: "", level: 0 };
    if (/[^a-zA-Z0-9!@#$%^&*]/.test(pw)) return { msg: "허용되지 않은 문자가 포함되었습니다.", level: 0 };
    if (pw.length < 8 || pw.length > 16) return { msg: "8~16자 이내로 입력해주세요.", level: 0 };
    const typesCount = [/\d/.test(pw), /[a-zA-Z]/.test(pw), /[!@#$%^&*]/.test(pw)].filter(Boolean).length;
    if (typesCount >= 3) return { msg: "안전한 비밀번호입니다.", level: 2 };
    if (typesCount >= 2 && pw.length >= 10) return { msg: "보통 수준의 비밀번호입니다.", level: 1 };
    return { msg: "영문/숫자/특수문자 조합이 필요합니다.", level: 0 };
  };

  const validateName = (name: string) => {
    if (name.length === 0) return "";
    if (name.length < 2 || name.length > 20) return "2~20자 사이로 입력해주세요.";
    if (!/^[가-힣a-zA-Z0-9]+$/.test(name)) return "올바른 양식이 아닙니다.";
    return "올바른 형식입니다.";
  };

  const handleIdChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    setForm({ ...form, id: e.target.value });
    setCheckStatus({ msg: "", isChecked: false, isDuplicated: false });
  };

  const handleCheckId = async () => {
    const res = await checkId(form.id);
    if (!res.isUnique) {
      setCheckStatus({ msg: "사용 가능한 아이디입니다.", isChecked: true, isDuplicated: false });
    } else {
      setCheckStatus({ msg: "이미 사용 중인 아이디입니다.", isChecked: true, isDuplicated: true });
    }
  };

  // 공통 엔터키 핸들러
  const handleKeyDown = (e: React.KeyboardEvent, nextRef?: React.RefObject<HTMLInputElement | null>, action?: () => void) => {
    if (e.key === "Enter") {
      e.preventDefault();
      if (nextRef && nextRef.current) {
        nextRef.current.focus();
      } else if (action) {
        action();
      }
    }
  };

  const getDisplayIdInfo = () => {
    const idMsg = validateId(form.id);
    if (form.id.length > 0 && idMsg !== "사용 가능한 아이디입니다.") {
      return { msg: idMsg, color: "text-red-500" };
    }
    if (!checkStatus.isChecked) {
      return { msg: "중복 확인 버튼을 눌러 아이디를 확인해주세요.", color: "text-orange-500" };
    }
    return {
      msg: checkStatus.msg,
      color: checkStatus.isDuplicated ? "text-red-500" : "text-emerald-500"
    };
  };

  const idMsg = validateId(form.id);
  const { msg: displayMsg, color: displayColor } = getDisplayIdInfo();
  const { msg: pwMsg, level: pwLevel } = validatePassword(form.password);
  const nameMsg = validateName(form.username);

  const idValid = checkStatus.isChecked && !checkStatus.isDuplicated;
  const pwValid = pwLevel >= 1;
  const confirmValid = form.confirmPassword.length > 0 && form.password === form.confirmPassword;
  const nameValid = nameMsg === "올바른 형식입니다.";

  const allValid = idValid && pwValid && confirmValid && nameValid;

  const doSignup = async () => {
    if (!allValid) {
      setToastMsg("양식을 모두 올바르게 입력해주세요.");
      return;
    }

    await signup(form.id, form.password, form.username, memberType);
    router.push("/login");
  };

  return (
      <>
        <form className="flex flex-col gap-4.5 w-full max-w-lg mx-auto">
          <label className="field">
            <span className="field-label">아이디</span>
            <div className="flex w-full gap-2">
              <input
                  type="text"
                  className="flex-1"
                  placeholder="영문 포함 4자 이상"
                  onChange={handleIdChange}
                  value={form.id}
                  onKeyDown={(e) => handleKeyDown(e, passwordInputRef)}
              />
              <button
                  type="button"
                  className="btn btn-small whitespace-nowrap"
                  disabled={idMsg !== "사용 가능한 아이디입니다." || checkStatus.isChecked}
                  onClick={handleCheckId}
              >
                중복 확인
              </button>
            </div>
            {form.id && (
                <span className={`text-xs font-bold ${displayColor}`}>
            {displayMsg}
          </span>
            )}
          </label>

          {/* 비밀번호 */}
          <label className="field">
            <span className="field-label">비밀번호</span>
            <div className="relative flex items-center">
              <input
                  ref={passwordInputRef}
                  type={showPassword ? "text" : "password"}
                  placeholder="8~16자 조합"
                  className="w-full pr-10"
                  onChange={(e) => setForm({ ...form, password: e.target.value })}
                  onKeyDown={(e) => handleKeyDown(e, confirmPasswordInputRef)}
              />
              <button
                  type="button"
                  className="absolute right-3 text-ink-soft hover:text-ink transition-colors"
                  onClick={() => setShowPassword(!showPassword)}
              >
                {showPassword ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            {form.password && (
                <span className={`text-xs font-bold ${pwLevel === 2 ? "text-emerald-500" : pwLevel === 1 ? "text-orange-500" : "text-red-500"}`}>
            {pwMsg}
          </span>
            )}
          </label>

          {/* 비밀번호 확인 */}
          <label className="field">
            <span className="field-label">비밀번호 확인</span>
            <div className="relative flex items-center">
              <input
                  ref={confirmPasswordInputRef}
                  type={showPasswordCheck ? "text" : "password"}
                  placeholder="비밀번호 재입력"
                  className="w-full pr-10"
                  onChange={(e) => setForm({ ...form, confirmPassword: e.target.value })}
                  onKeyDown={(e) => handleKeyDown(e, nameInputRef)}
              />
              <button
                  type="button"
                  className="absolute right-3 text-ink-soft hover:text-ink transition-colors"
                  onClick={() => setShowPasswordCheck(!showPasswordCheck)}
              >
                {showPasswordCheck ? <EyeOff size={20} /> : <Eye size={20} />}
              </button>
            </div>
            {form.confirmPassword && (
                <span className={`text-xs font-bold ${confirmValid ? "text-emerald-500" : "text-red-500"}`}>
            {confirmValid ? "비밀번호가 일치합니다." : "비밀번호가 일치하지 않습니다."}
          </span>
            )}
          </label>

          <label className="field">
            <span className="field-label">이름</span>
            <input
                ref={nameInputRef}
                type="text"
                placeholder="이름을 입력하세요"
                onChange={(e) => setForm({ ...form, username: e.target.value })}
                onKeyDown={(e) => handleKeyDown(e, undefined, doSignup)}
            />
            {form.username && <span className={`text-xs font-bold ${nameValid ? "text-emerald-500" : "text-red-500"}`}>{nameMsg}</span>}
          </label>

          <fieldset className="field gap-2.5">
            <legend className="field-label">회원유형</legend>
            <div className="flex gap-3 max-[560px]:flex-col">
              <label className={`${pillBase} ${memberType === "의료진" ? pillActive : pillIdle}`}>
                <input type="radio" checked={memberType === "의료진"} onChange={() => setMemberType("의료진")} className={hiddenRadio} />의사
              </label>
              <label className={`${pillBase} ${memberType === "연구원" ? pillActive : pillIdle}`}>
                <input type="radio" checked={memberType === "연구원"} onChange={() => setMemberType("연구원")} className={hiddenRadio} />연구원
              </label>
            </div>
          </fieldset>

          <button className={`btn btn-big w-full mt-2 transition-opacity ${!allValid ? "opacity-50 cursor-not-allowed" : ""}`}
                  type="button"
                  onClick={doSignup} disabled={!allValid}>
            회원가입
          </button>
        </form>
        {/* 토스트 메시지 출력 */}
        {toastMsg && (
            <Toast message={toastMsg} onClose={() => setToastMsg(null)} />
        )}
      </>
  );
}