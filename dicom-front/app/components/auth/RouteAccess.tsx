"use client";

import { type ReactNode, useEffect, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import { getStoredAccountType, type AccountType } from "@/app/api/ApiFetch";

const HOME_BY_ACCOUNT_TYPE = {
  MEDICAL: "/workspace",
  RESEARCHER: "/research",
} satisfies Record<AccountType, string>;

const SERVER_AUTH_STATE = "__server_auth_state__";

/**
 * 인증 상태 변경을 구독합니다.
 */
function subscribeAuthState(onStoreChange: () => void) {
  window.addEventListener("auth-state-changed", onStoreChange);
  window.addEventListener("storage", onStoreChange);

  return () => {
    window.removeEventListener("auth-state-changed", onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}

/**
 * 로컬 스토리지에서 인증 상태를 읽습니다.
 */
function readStoredAuthState() {
  return JSON.stringify({
    username: localStorage.getItem("username"),
    accountType: getStoredAccountType(),
  });
}

/**
 * 정적 서버 인증 상태를 반환합니다.
 */
function readServerAuthState() {
  return SERVER_AUTH_STATE;
}

/**
 * 저장된 인증 상태를 가져오는 훅입니다.
 */
function useStoredAuth() {
  const authState = useSyncExternalStore(
    subscribeAuthState,
    readStoredAuthState,
    readServerAuthState,
  );

  if (authState === SERVER_AUTH_STATE) {
    return {
      hydrated: false,
      username: null,
      accountType: "MEDICAL" as AccountType,
    };
  }

  const parsed = JSON.parse(authState) as {
    username: string | null;
    accountType: AccountType;
  };

  return {
    hydrated: true,
    username: parsed.username,
    accountType: parsed.accountType,
  };
}

/**
 * 계정 유형에 따라 접근을 제한하는 RoleGuard 컴포넌트입니다.
 * 권한이 없는 사용자를 적절한 홈페이지나 로그인 페이지로 리디렉션합니다.
 *
 * @param props - 컴포넌트 속성
 * @param props.allow - 허용되는 계정 유형
 * @param props.children - 권한이 있을 경우 렌더링할 자식 컴포넌트
 * @returns 권한이 있을 경우 감싸진 컴포넌트, 그렇지 않으면 null
 */
/**
 * 계정 유형에 따라 접근을 제한하는 가드 컴포넌트입니다.
 */
export function RoleGuard({
  allow,
  children,
}: {
  allow: AccountType;
  children: ReactNode;
}) {
  const router = useRouter();
  const auth = useStoredAuth();

  useEffect(() => {
    if (!auth.hydrated) {
      return;
    }

    if (!auth.username) {
      router.replace("/login");
      return;
    }

    if (auth.accountType !== allow) {
      router.replace(HOME_BY_ACCOUNT_TYPE[auth.accountType]);
    }
  }, [allow, auth.accountType, auth.hydrated, auth.username, router]);

  return auth.hydrated && auth.username && auth.accountType === allow ? <>{children}</> : null;
}

/**
 * 인증된 사용자를 적절한 홈페이지로 리디렉션하는 컴포넌트입니다.
 *
 * @returns null
 */
/**
 * 인증된 사용자를 각각의 홈페이지로 리디렉션합니다.
 */
export function HomeRedirect() {
  const router = useRouter();
  const auth = useStoredAuth();

  useEffect(() => {
    if (!auth.hydrated || !auth.username) {
      return;
    }

    router.replace(HOME_BY_ACCOUNT_TYPE[auth.accountType]);
  }, [auth.accountType, auth.hydrated, auth.username, router]);

  return null;
}
