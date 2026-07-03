import axios from 'axios';

const api = axios.create({
    baseURL: 'http://localhost:8080/api/worklist',
    withCredentials: true,  // 쿠키를 함께 전송하기 위해 필수 설정
});

export const getMyPatients = () => api.get('/patients');
export const addPatient = (pId: string) => api.post(`/patients/add?pId=${pId}`);
export const getStudiesWithStatus = (PId: string) => api.get(`/studies?pId=${PId}`);
export const assignStudies = (PId: string, studyKeys: number[]) => api.post(`/studies/assign?pId=${PId}`, studyKeys);
export const searchPatients = (keyword: string) => api.get(`/patients/search?keyword=${keyword}`);


//
// // 쿠키에서 특정 이름의 값을 가져오는 함수
// const getCookie = (name: string) => {
//     const value = `; ${document.cookie}`;
//     const parts = value.split(`; ${name}=`);
//     if (parts.length === 2) return parts.pop()?.split(';').shift();
// };
//
// // 인터셉터: 모든 요청 헤더에 Authorization 토큰 자동 추가
// api.interceptors.request.use((config) => {
//     // 로컬스토리지나 상태 관리에서 토큰을 가져옵니다.
//     const token = getCookie('token');
//
//     console.log("[API Request] 토큰 확인:", token ? `토큰 발견: ${token.substring(0, 10)}...` : "토큰 없음 (쿠키에서 확인 불가)");
//
//     if (token) {
//         config.headers.Authorization = `Bearer ${token}`;
//     } else {
//         console.warn("[API Request] Authorization 헤더에 토큰이 포함되지 않았습니다.");
//     }
//
//     return config;
// }, (error) => {
//     return Promise.reject(error);
// });
//
// // 1. 의사의 환자 목록 조회
// export const getMyPatients = () =>
//     api.get('/patients');
//
// // 2. 환자 등록 (pId만 넘기면 됨, userKey는 토큰에서 추출됨)
// export const addPatient = (PId: string) =>
//     api.post(`/patients?pId=${PId}`);
//
// // 3. 특정 환자의 검사 목록 및 할당 상태 조회
// export const getStudiesWithStatus = (PId: string) =>
//     api.get(`/studies?pId=${PId}`);
//
// // 4. 특정 검사들 할당 (studyKeys를 body에 담아 전달)
// export const assignStudies = (PId: string, studyKeys: number[]) =>
//     api.post(`/studies/assign?pId=${PId}`, studyKeys);
//
// export const searchPatients = (keyword: string) =>
//     api.get(`/patients/search?keyword=${keyword}`);