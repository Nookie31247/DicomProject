"use client";

import { useEffect, useState } from "react";
import ChangePw from "@/app/mypage/ChangePw";
import Withdraw from "@/app/mypage/Withdraw";
import BackButton from "@/app/mypage/BackButton";
import { getUserInfo } from "@/app/api/authApi";
import { getStoredAccountType, type AccountType } from "@/app/api/ApiFetch";

type UserInfo = {
  userId: string;
  username: string;
  registerDay: string;
};

export default function MyPage() {
  const [userdata, setUserdata] = useState<UserInfo | null>(null);
  const [accountType, setAccountType] = useState<AccountType>("MEDICAL");
  const [errorMessage, setErrorMessage] = useState("");

  useEffect(() => {
    const storedType = getStoredAccountType();
    setAccountType(storedType);

    getUserInfo(storedType)
      .then((data: UserInfo) => setUserdata(data))
      .catch((error: unknown) => {
        setErrorMessage(error instanceof Error ? error.message : "회원 정보를 불러오지 못했습니다.");
      });
  }, []);

  const accountLabel = accountType === "RESEARCHER" ? "연구원" : "의료진";

  return (
    <div className="page">
      <section className="flex flex-1 justify-center px-[clamp(20px,5vw,62px)] pt-11 pb-20 max-[560px]:px-4 max-[560px]:pb-14 max-[560px]:pt-7">
        <div className="w-full max-w-180 rounded-3xl border border-line bg-paper px-[clamp(26px,4vw,52px)] py-11 shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-[560px]:px-5.5 max-[560px]:py-8">
          <BackButton />
          <h1 className="m-0 mb-1.5 text-3xl font-bold tracking-[-0.01em] text-ink max-[560px]:text-[26px]">
            마이페이지
          </h1>
          <p className="m-0 mb-8 text-lg text-ink-soft">
            {userdata ? `${userdata.username}님의 계정 정보입니다.` : "계정 정보를 불러오는 중입니다."}
          </p>

          {errorMessage ? (
            <p className="mb-8 rounded-2xl border border-[#f1c7c9] bg-[#fef3f2] px-5 py-4 text-sm font-semibold text-[#d92d20]">
              {errorMessage}
            </p>
          ) : (
            <dl className="m-0 mb-10 flex flex-col divide-y divide-line overflow-hidden rounded-2xl border border-line">
              <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
                <dt className="text-base font-semibold text-ink-soft">아이디</dt>
                <dd className="m-0 text-base font-semibold text-ink">
                  {userdata?.userId ?? "-"}
                </dd>
              </div>
              <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
                <dt className="text-base font-semibold text-ink-soft">가입일자</dt>
                <dd className="m-0 text-base font-semibold text-ink">
                  {userdata?.registerDay ?? "-"}
                </dd>
              </div>
              <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
                <dt className="text-base font-semibold text-ink-soft">회원유형</dt>
                <dd className="m-0 text-base font-semibold text-ink">
                  <span className="inline-flex items-center rounded-full bg-[rgba(76,255,157,0.18)] px-3.25 py-1 text-sm font-bold text-mint-deep">
                    {accountLabel}
                  </span>
                </dd>
              </div>
            </dl>
          )}

          <ChangePw />
          <Withdraw />
        </div>
      </section>
    </div>
  );
}
