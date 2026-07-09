"use client";

import { type ReactNode, useEffect, useSyncExternalStore } from "react";
import { useRouter } from "next/navigation";
import { getStoredAccountType, type AccountType } from "@/app/api/ApiFetch";

const HOME_BY_ACCOUNT_TYPE = {
  MEDICAL: "/workspace",
  RESEARCHER: "/research",
} satisfies Record<AccountType, string>;

const SERVER_AUTH_STATE = "__server_auth_state__";

function subscribeAuthState(onStoreChange: () => void) {
  window.addEventListener("auth-state-changed", onStoreChange);
  window.addEventListener("storage", onStoreChange);

  return () => {
    window.removeEventListener("auth-state-changed", onStoreChange);
    window.removeEventListener("storage", onStoreChange);
  };
}

function readStoredAuthState() {
  return JSON.stringify({
    username: localStorage.getItem("username"),
    accountType: getStoredAccountType(),
  });
}

function readServerAuthState() {
  return SERVER_AUTH_STATE;
}

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
