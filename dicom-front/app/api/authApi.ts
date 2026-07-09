import {ApiFetch} from "./ApiFetch";


// 1. 로그인 (클라이언트 요청)
export async function login(userId: string, password: string) {
  return ApiFetch("/api/users/login", {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password })
  })
}

// 2. 회원가입 (클라이언트 요청)
export async function signup(userId: string, password: string, name: string, userType: string) {
  return ApiFetch("/api/users/signup", {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId, password, name, userType})
  })
}

// 3. 로그아웃 (클라이언트 요청)
export async function logout() {
  return ApiFetch("/api/users/logout", {
    method: 'POST',
    credentials: 'include'
  })
}

// 4. 아이디 중복 확인 (클라이언트 요청)
export async function checkId(userId: string) {
  return ApiFetch("/api/users/check-id", {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ userId })
  })
}

// 5. 비밀번호 수정 (클라이언트 요청)
export async function changePassword(currentPassword: string, newPassword: string) {
  return ApiFetch("/api/users/change-password", {
    method: 'PUT',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({currentPassword, newPassword })
  })
}

// 6. 회원탈퇴 (클라이언트 요청)
export async function deleteUser(password: string) {
  return ApiFetch("/api/users/delete", {
    method: 'DELETE',
    credentials: 'include',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({password})
  })
}

// 7. 회원정보 불러오기 (서버 요청, 쿠키값 수동으로 넣을 것)
export async function getUserInfo(token: string | undefined) {
  return ApiFetch("/api/users/info", {
    method: 'GET',
    headers: { 'Cookie': `token=${token}` },
    cache: 'no-store',
    credentials: 'include'
  })
}