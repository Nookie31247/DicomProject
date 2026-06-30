
import {useState} from "react";
import {signup} from "@/app/api/authApi";

export default function SignupForm() {
  // 라디오 필 공통 스타일 + 선택 상태 스타일 (테두리 두께는 2px로 고정해 흔들림 방지)
  const pillBase = "flex flex-1 cursor-pointer items-center justify-center gap-2 rounded-xl border-2 px-4 py-[13px] text-base transition-[border-color,background,color] duration-150";
  const pillActive = "border-mint-deep bg-[rgba(76,255,157,0.3)] text-ink font-bold";
  const pillIdle = "border-line bg-canvas text-ink-soft font-semibold";
  // 네이티브 라디오는 완전히 숨김 (선택 표시는 필 테두리/배경으로 대체)
  const hiddenRadio = "pointer-events-none absolute m-0 h-px w-px border-0 p-0 opacity-0 [clip:rect(0_0_0_0)] [clip-path:inset(50%)]";

  const [id, setId] = useState("");
  const [password, setPassword] = useState("");
  const [confirmPassword, setConfirmPassword] = useState("");
  const [username, setUsername] = useState("");

  // 의료진, 연구원 선택 모드
  const [memberType, setMemberType] = useState<"의료진" | "연구원">("의료진");

  const doSignup = async () => {
    if(password == confirmPassword) {
      await signup(id, password, username, memberType);
    }
    else {
      alert("비밀번호 안맞음");
    }
  }

  return (
    <form className="flex flex-col gap-4.5">
      <label className="field">
        <span className="field-label">아이디</span>
        <input
          type="text"
          name="username"
          placeholder="아이디를 입력하세요"
          autoComplete="username"
          onChange={(e) => setId(e.target.value)}
        />
      </label>

      <label className="field">
        <span className="field-label">비밀번호</span>
        <input
          type="password"
          name="password"
          placeholder="비밀번호를 입력하세요"
          autoComplete="new-password"
          onChange={(e) => setPassword(e.target.value)}
        />
      </label>

      <label className="field">
        <span className="field-label">비밀번호 확인</span>
        <input
          type="password"
          name="passwordConfirm"
          placeholder="비밀번호를 다시 입력하세요"
          autoComplete="new-password"
          onChange={(e) => setConfirmPassword(e.target.value)}
        />
      </label>

      <label className="field">
        <span className="field-label">이름</span>
        <input
          type="text"
          name="name"
          placeholder="이름을 입력하세요"
          onChange={(e) => setUsername(e.target.value)}
        />
      </label>

      <fieldset className="field gap-2.5">
        <legend className="field-label">회원유형</legend>
        <div className="flex gap-3 max-[560px]:flex-col">
          <label
            className={`${pillBase} ${
              memberType === "의료진" ? pillActive : pillIdle
            }`}
          >
            <input
              type="radio"
              name="memberType"
              value="doctor"
              checked={memberType === "의료진"}
              onChange={() => setMemberType("의료진")}
              className={hiddenRadio}
            />
            의사
          </label>
          <label
            className={`${pillBase} ${
              memberType === "연구원" ? pillActive : pillIdle
            }`}
          >
            <input
              type="radio"
              name="memberType"
              value="researcher"
              checked={memberType === "연구원"}
              onChange={() => setMemberType("연구원")}
              className={hiddenRadio}
            />
            연구원
          </label>
        </div>
      </fieldset>
      <button className="btn btn-big w-full mt-2" type="button" onClick={doSignup}>
        회원가입
      </button>
    </form>
  );
}