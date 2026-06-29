import {apiFetch} from "@/app/api/apiFetch";


// 로그인
export async function login(userId: string, password: string) {
  return apiFetch("/api/users/login", {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password })
  })
}

// 회원가입
export async function signup(userId: string, password: string, name: string, userType: string) {
  return apiFetch("/api/users/signup", {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password, name, userType})
  })
}

// 로그아웃
export async function logout(userId: string, password: string, name: string, userType: string) {
  return apiFetch("/api/users/logout", {
    method: 'POST',
    headers: { "Authorization": `Bearer ${token}` },
  })
}