// ============================================================
// 목업 데이터 (테스트용)
// 백엔드 연동 전, 화면 동작 확인을 위한 더미 데이터입니다.
// 실제 환자/DICOM 검색 로직은 백엔드에서 수행됩니다.
// ============================================================

// DICOM 파일 한 건(시리즈 단위)에 들어가는 정보.
// 필드명은 실제 DICOM 표준 태그를 참고했습니다.
export type DicomItem = {
  id: string;
  modality: string; // (0008,0060) Modality — CT / MR / CR / US ...
  description: string; // (0008,103E) Series Description
  bodyPart: string; // (0018,0015) Body Part Examined
  studyDate: string; // (0008,0020) Study Date
  seriesNumber: number; // (0020,0011) Series Number
  images: number; // 시리즈 내 영상(인스턴스) 수
  studyInstanceUID: string; // (0020,000D) Study Instance UID
};

// 환자 한 명. (왼쪽 "환자 목록"에 표시되는 항목)
export type Patient = {
  id: string;
  name: string; // (0010,0010) Patient Name
  patientId: string; // (0010,0020) Patient ID
  sex: "M" | "F"; // (0010,0040) Patient Sex
  birthDate: string; // (0010,0030) Patient Birth Date
  items: DicomItem[];
};

// 로그인한 사용자(목업). 인증 연동 전 화면 확인용 더미 데이터입니다.
export const currentUser = {
  name: "강현우",
  role: "doctor" as "doctor" | "researcher",
};

export const patients: Patient[] = [
  {
    id: "p1",
    name: "김민준",
    patientId: "P-20240001",
    sex: "M",
    birthDate: "1985-03-12",
    items: [
      {
        id: "p1-s1",
        modality: "CT",
        description: "Chest CT w/o contrast",
        bodyPart: "CHEST",
        studyDate: "2024-05-14",
        seriesNumber: 2,
        images: 220,
        studyInstanceUID: "1.2.840.113619.2.55.3.1001",
      },
      {
        id: "p1-s2",
        modality: "MR",
        description: "Brain MRI T2 FLAIR",
        bodyPart: "BRAIN",
        studyDate: "2024-03-02",
        seriesNumber: 4,
        images: 180,
        studyInstanceUID: "1.2.840.113619.2.55.3.1002",
      },
      {
        id: "p1-s3",
        modality: "CR",
        description: "Chest PA",
        bodyPart: "CHEST",
        studyDate: "2024-01-20",
        seriesNumber: 1,
        images: 2,
        studyInstanceUID: "1.2.840.113619.2.55.3.1003",
      },
    ],
  },
  {
    id: "p2",
    name: "이서연",
    patientId: "P-20240002",
    sex: "F",
    birthDate: "1990-07-25",
    items: [
      {
        id: "p2-s1",
        modality: "MR",
        description: "Knee MRI Sagittal",
        bodyPart: "KNEE",
        studyDate: "2024-06-01",
        seriesNumber: 3,
        images: 144,
        studyInstanceUID: "1.2.840.113619.2.55.3.2001",
      },
      {
        id: "p2-s2",
        modality: "US",
        description: "Abdominal Ultrasound",
        bodyPart: "ABDOMEN",
        studyDate: "2024-04-18",
        seriesNumber: 1,
        images: 36,
        studyInstanceUID: "1.2.840.113619.2.55.3.2002",
      },
      {
        id: "p2-s3",
        modality: "CR",
        description: "Knee AP/LAT",
        bodyPart: "KNEE",
        studyDate: "2024-02-09",
        seriesNumber: 1,
        images: 2,
        studyInstanceUID: "1.2.840.113619.2.55.3.2003",
      },
    ],
  },
  {
    id: "p3",
    name: "박지후",
    patientId: "P-20240003",
    sex: "M",
    birthDate: "1978-11-30",
    items: [
      {
        id: "p3-s1",
        modality: "CT",
        description: "Abdomen CT w/ contrast",
        bodyPart: "ABDOMEN",
        studyDate: "2024-05-28",
        seriesNumber: 5,
        images: 310,
        studyInstanceUID: "1.2.840.113619.2.55.3.3001",
      },
      {
        id: "p3-s2",
        modality: "PT",
        description: "PET Whole Body",
        bodyPart: "WHOLEBODY",
        studyDate: "2024-05-28",
        seriesNumber: 2,
        images: 268,
        studyInstanceUID: "1.2.840.113619.2.55.3.3002",
      },
      {
        id: "p3-s3",
        modality: "CR",
        description: "Abdomen Supine",
        bodyPart: "ABDOMEN",
        studyDate: "2024-03-15",
        seriesNumber: 1,
        images: 1,
        studyInstanceUID: "1.2.840.113619.2.55.3.3003",
      },
    ],
  },
  {
    id: "p4",
    name: "최예은",
    patientId: "P-20240004",
    sex: "F",
    birthDate: "1996-02-14",
    items: [
      {
        id: "p4-s1",
        modality: "MR",
        description: "Lumbar Spine MRI",
        bodyPart: "LSPINE",
        studyDate: "2024-06-10",
        seriesNumber: 6,
        images: 96,
        studyInstanceUID: "1.2.840.113619.2.55.3.4001",
      },
      {
        id: "p4-s2",
        modality: "CT",
        description: "Head CT Plain",
        bodyPart: "HEAD",
        studyDate: "2024-04-03",
        seriesNumber: 2,
        images: 156,
        studyInstanceUID: "1.2.840.113619.2.55.3.4002",
      },
      {
        id: "p4-s3",
        modality: "CR",
        description: "L-Spine AP/LAT",
        bodyPart: "LSPINE",
        studyDate: "2024-01-08",
        seriesNumber: 1,
        images: 2,
        studyInstanceUID: "1.2.840.113619.2.55.3.4003",
      },
    ],
  },
  {
    id: "p5",
    name: "정도윤",
    patientId: "P-20240005",
    sex: "M",
    birthDate: "1982-09-05",
    items: [
      {
        id: "p5-s1",
        modality: "CT",
        description: "Coronary CT Angiography",
        bodyPart: "HEART",
        studyDate: "2024-06-15",
        seriesNumber: 3,
        images: 412,
        studyInstanceUID: "1.2.840.113619.2.55.3.5001",
      },
      {
        id: "p5-s2",
        modality: "US",
        description: "Echocardiography",
        bodyPart: "HEART",
        studyDate: "2024-05-02",
        seriesNumber: 1,
        images: 48,
        studyInstanceUID: "1.2.840.113619.2.55.3.5002",
      },
      {
        id: "p5-s3",
        modality: "CR",
        description: "Chest PA",
        bodyPart: "CHEST",
        studyDate: "2024-02-21",
        seriesNumber: 1,
        images: 2,
        studyInstanceUID: "1.2.840.113619.2.55.3.5003",
      },
    ],
  },
];
