import Link from "next/link";
import ChangePw from "@/app/mypage/ChangePw";
import Withdraw from "@/app/mypage/Withdraw";
import {getUserInfo} from "@/app/api/authApi";
import {cookies} from "next/headers";

export default async function MyPage() {
  const cookieList = await cookies();
  const token = cookieList.get("token")?.value;
  const userdata = await getUserInfo(token);

  return (
    <div className="page">
      {/* ───────────── My page ───────────── */}
      <section className="flex flex-1 justify-center px-[clamp(20px,5vw,62px)] pt-11 pb-20 max-[560px]:px-4 max-[560px]:pb-14 max-[560px]:pt-7">
        <div className="w-full max-w-180 rounded-3xl border border-line bg-paper px-[clamp(26px,4vw,52px)] py-11 shadow-[0_24px_48px_-24px_rgba(15,31,61,0.18)] max-[560px]:px-5.5 max-[560px]:py-8">
          {/* 제목 위 뒤로가기 버튼 */}
          <Link
            href="/workspace"
            className="mb-4.5 flex h-9.5 w-9.5 items-center justify-center rounded-[10px] border border-line bg-paper text-xl leading-none text-ink-soft no-underline transition-[background,color,border-color] duration-150 hover:border-mint-deep hover:bg-canvas hover:text-ink"
            aria-label="워크스페이스로 돌아가기"
            title="뒤로가기"
          >
            ←
          </Link>
          <h1 className="m-0 mb-1.5 text-3xl font-bold tracking-[-0.01em] text-ink max-[560px]:text-[26px]">
            마이페이지
          </h1>
          <p className="m-0 mb-8 text-lg text-ink-soft">
            {userdata.username}님의 계정 정보입니다.
          </p>

          {/* ── 회원 정보 ── */}
          <dl className="m-0 mb-10 flex flex-col divide-y divide-line overflow-hidden rounded-2xl border border-line">
            <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
              <dt className="text-base font-semibold text-ink-soft">아이디</dt>
              <dd className="m-0 text-base font-semibold text-ink">
                {userdata.userId}
              </dd>
            </div>
            <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
              <dt className="text-base font-semibold text-ink-soft">가입일자</dt>
              <dd className="m-0 text-base font-semibold text-ink">
                {userdata.registerDay}
              </dd>
            </div>
            <div className="grid grid-cols-[160px_1fr] items-center gap-4 px-5.5 py-4 max-[560px]:grid-cols-1 max-[560px]:items-start max-[560px]:gap-1.25">
              <dt className="text-base font-semibold text-ink-soft">회원유형</dt>
              <dd className="m-0 text-base font-semibold text-ink">
                <span className="inline-flex items-center rounded-full bg-[rgba(76,255,157,0.18)] px-3.25 py-1 text-sm font-bold text-mint-deep">
                  {userdata.userRole}
                </span>
              </dd>
            </div>
          </dl>

          {/* ── 비밀번호 변경 ── */}
          <ChangePw/>

          {/* ── 회원 탈퇴 ── */}
          <Withdraw/>
        </div>
      </section>
    </div>
  );
}
