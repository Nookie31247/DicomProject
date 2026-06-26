// ============================================================
// 목업 데이터 (테스트용)
// API 명세서에 맞춘 데이터 모델입니다.
// ============================================================

export type ApiPatient = {
  "patient-name": string;
  "patient-id": string;
  "patient-sex": "M" | "F";
  "patient-birth": string;
  "latest-study-datetime": string;
  "study-count": number;
  hidden: boolean;
};

export type ApiStudy = {
  "study-key": string;
  "patient-id": string; // 목업 필터링용
  modality: string;
  description: string;
  datetime: string;
  "series-num": number;
  "images-num": number;
  "allow-research": boolean;
  hidden: boolean;
};

export type ApiSeries = {
  "series-key": string;
  "study-key": string; // 목업 필터링용
  "series-index": number;
  datetime: string;
  "series-num": number;
  bodypart: string;
  hidden: boolean;
};

// 로그인한 사용자
export const currentUser = {
  name: "강현우",
  userId: "hyunwoo.kang",
  role: "doctor" as "doctor" | "researcher",
  licenseNumber: "2024-12345",
};

export const patients: ApiPatient[] = [
  {
    "patient-name": "김민준",
    "patient-id": "P-20240001",
    "patient-sex": "M",
    "patient-birth": "1985-03-12",
    "latest-study-datetime": "2024-05-14T10:30:00Z",
    "study-count": 3,
    hidden: false,
  },
  {
    "patient-name": "이서연",
    "patient-id": "P-20240002",
    "patient-sex": "F",
    "patient-birth": "1990-07-25",
    "latest-study-datetime": "2024-06-01T14:20:00Z",
    "study-count": 3,
    hidden: false,
  },
  {
    "patient-name": "박지후",
    "patient-id": "P-20240003",
    "patient-sex": "M",
    "patient-birth": "1978-11-30",
    "latest-study-datetime": "2024-05-28T09:15:00Z",
    "study-count": 0,
    hidden: false,
  },
];

export const studies: ApiStudy[] = [
  // 김민준 (P-20240001)
  {
    "study-key": "ST-1001",
    "patient-id": "P-20240001",
    modality: "CT",
    description: "Chest CT w/o contrast",
    datetime: "2024-05-14T10:30:00Z",
    "series-num": 2,
    "images-num": 220,
    "allow-research": true,
    hidden: false,
  },
  {
    "study-key": "ST-1002",
    "patient-id": "P-20240001",
    modality: "MR",
    description: "Brain MRI T2 FLAIR",
    datetime: "2024-03-02T11:00:00Z",
    "series-num": 4,
    "images-num": 180,
    "allow-research": false,
    hidden: false,
  },
  {
    "study-key": "ST-1003",
    "patient-id": "P-20240001",
    modality: "CR",
    description: "Chest PA",
    datetime: "2024-01-20T09:45:00Z",
    "series-num": 1,
    "images-num": 2,
    "allow-research": true,
    hidden: false,
  },
  
  // 이서연 (P-20240002)
  {
    "study-key": "ST-2001",
    "patient-id": "P-20240002",
    modality: "MR",
    description: "Knee MRI Sagittal",
    datetime: "2024-06-01T14:20:00Z",
    "series-num": 3,
    "images-num": 144,
    "allow-research": true,
    hidden: false,
  },
  {
    "study-key": "ST-2002",
    "patient-id": "P-20240002",
    modality: "US",
    description: "Abdominal Ultrasound",
    datetime: "2024-04-18T10:10:00Z",
    "series-num": 1,
    "images-num": 36,
    "allow-research": false,
    hidden: false,
  },
  {
    "study-key": "ST-2003",
    "patient-id": "P-20240002",
    modality: "CR",
    description: "Knee AP/LAT",
    datetime: "2024-02-09T16:30:00Z",
    "series-num": 1,
    "images-num": 2,
    "allow-research": true,
    hidden: false,
  },
];

export const series: ApiSeries[] = [
  // 김민준 - ST-1001 (Chest CT)
  {
    "series-key": "SE-1001-1",
    "study-key": "ST-1001",
    "series-index": 1,
    datetime: "2024-05-14T10:30:00Z",
    "series-num": 1,
    bodypart: "CHEST",
    hidden: false,
  },
  {
    "series-key": "SE-1001-2",
    "study-key": "ST-1001",
    "series-index": 2,
    datetime: "2024-05-14T10:35:00Z",
    "series-num": 2,
    bodypart: "CHEST",
    hidden: false,
  },
  // ST-2001 (Knee MRI)
  {
    "series-key": "SE-2001-1",
    "study-key": "ST-2001",
    "series-index": 1,
    datetime: "2024-06-01T14:20:00Z",
    "series-num": 1,
    bodypart: "KNEE",
    hidden: false,
  },
  {
    "series-key": "SE-2001-2",
    "study-key": "ST-2001",
    "series-index": 2,
    datetime: "2024-06-01T14:25:00Z",
    "series-num": 2,
    bodypart: "KNEE",
    hidden: false,
  },
  {
    "series-key": "SE-2001-3",
    "study-key": "ST-2001",
    "series-index": 3,
    datetime: "2024-06-01T14:30:00Z",
    "series-num": 3,
    bodypart: "KNEE",
    hidden: false,
  },
];
