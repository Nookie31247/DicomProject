"use client";

import { useRouter } from "next/navigation";

// 그냥 <Link href="/workspace">로 이동하면 workspace가 URL 쿼리로 들고 있던 선택
// 환자/날짜 필터가 초기화된 채로 이동하게 된다. router.back()은 브라우저 뒤로가기와
// 동일하게 동작해서 원래 있던 페이지(대개 workspace)의 상태를 그대로 복원해준다.
export default function BackButton() {
  const router = useRouter();

  return (
      <button
          type="button"
          onClick={() => router.back()}
          className="mb-4.5 flex h-9.5 w-9.5 items-center justify-center rounded-[10px] border border-line bg-paper text-xl leading-none text-ink-soft no-underline transition-[background,color,border-color] duration-150 hover:border-mint-deep hover:bg-canvas hover:text-ink"
          aria-label="이전 화면으로 돌아가기"
          title="뒤로가기"
      >
        ←
      </button>
  );
}
