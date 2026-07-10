import {medicalApiFetch, researchApiFetch} from "./ApiFetch";

// [신규] 업로드 실패 파일 1개의 상세 정보 (백엔드 DicomResponseDto.FailedFileDto와 대응).
// message는 서버가 사용자에게 그대로 보여줘도 되도록 만든 한국어 문구다(예: "이미 다른 환자에게 등록된 검사입니다.").
interface UploadFailedFileDto {
  "file-name": string;
  message: string;
}

// [신규] 다중 파일 업로드 결과 요약 (백엔드 UploadResultDto와 대응).
// 기존엔 이 응답을 아예 안 들여다보고 무조건 "완료!" 토스트만 띄웠는데, 이제 failed-files를 보고
// 실패 사유를 토스트로 안내하기 위해 타입을 명시한다.
export interface UploadResultDto {
  "succeeded-files": string[];
  "failed-files": UploadFailedFileDto[];
}

/**
 * DICOM 관련 작업을 위한 API 클라이언트입니다.
 */
const dicomApi = {
  getPatients(start: string | null, end: string | null, search: string | null) {
    const params = new URLSearchParams();
    if (search !== null && search !== "") {
      params.append("search", search);
    }
    if (start !== null && start !== "" && end !== null && end !== "") {
      params.append("start", start);
      params.append("end", end);
    }

    const queryString = params.toString();

    return medicalApiFetch(
      queryString
        ? `/api/medical/dicom/patients?${queryString}`
        : "/api/medical/dicom/patients",
    );
  },

  getStudies(patientId: number, start: string | null, end: string | null, search: string | null) {
    const params = new URLSearchParams();
    if (search !== null && search !== "") {
      params.append("search", search);
    }
    if (start !== null && start !== "" && end !== null && end !== "") {
      params.append("start", start);
      params.append("end", end);
    }
    params.append("patient-key", patientId.toString());

    return medicalApiFetch(`/api/medical/dicom/studies?${params.toString()}`);
  },

  getSeries(studyKey: number) {
    return medicalApiFetch(`/api/medical/dicom/series?study-key=${studyKey}`);
  },

  getResearchStudies(start?: string | null, end?: string | null, search?: string | null) {
    const params = new URLSearchParams();
    if (search) {
      params.append("search", search);
    }
    if (start && end) {
      params.append("start", start);
      params.append("end", end);
    }

    const queryString = params.toString();
    return researchApiFetch(queryString ? `/api/research/dicom/studies?${queryString}` : "/api/research/dicom/studies");
  },

  getSeriesForResearch(studyKey: number) {
    return researchApiFetch(`/api/research/dicom/studies/${studyKey}/series`);
  },

  setPatientHide(list: HiddenPatientList[]) {
    return medicalApiFetch("/api/medical/dicom/patients/hide", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(list),
    });
  },

  setStudyHide(list: HiddenStudyList[]) {
    return medicalApiFetch("/api/medical/dicom/studies/hide", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(list),
    });
  },

  setSeriesHide(list: HiddenSeriesList[]) {
    return medicalApiFetch("/api/medical/dicom/series/hide", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(list),
    });
  },

  setStudyResearch(list: StudyResearchList[]) {
    return medicalApiFetch("/api/medical/dicom/studies/research-allow", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(list),
    });
  },

  addPatient(patient: { name: string; sex: string; birth: string }) {
    return medicalApiFetch("/api/medical/dicom/patients", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        "patient-name": patient.name,
        "patient-sex": patient.sex,
        "patient-birth": patient.birth,
      }),
    });
  },

  uploadDicomFiles: (
    formData: FormData,
    options?: { signal?: AbortSignal; onProgress?: (ratio: number) => void },
  ): Promise<UploadResultDto> => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", "/api/medical/dicom/upload");

      xhr.upload.onprogress = (e) => {
        if (!options?.onProgress) {
          return;
        }
        options.onProgress(e.lengthComputable ? e.loaded / e.total : -1);
      };

      xhr.onload = () => {
        // 이 업로드 엔드포인트는 실제로는 항상 UploadResultDto를 담아 200으로 응답하지만(컨트롤러
        // 코드 참고), 이 헬퍼는 다른 곳에서도 쓰일 수 있는 범용 함수라 204(본문 없음) 방어 코드가
        // 남아있었다. 반환 타입을 Promise<UploadResultDto>로 강화하면서 null을 그대로 resolve하면
        // 타입이 안 맞으므로, "성공/실패 파일이 없는" 빈 결과로 대체한다.
        if (xhr.status === 204) {
          resolve({ "succeeded-files": [], "failed-files": [] });
          return;
        }

        const contentType = xhr.getResponseHeader("Content-Type");
        const text = xhr.responseText;
        const data = contentType?.includes("application/json")
          ? (text ? JSON.parse(text) : null)
          : text;

        if (xhr.status < 200 || xhr.status >= 300) {
          const message = (data && typeof data === "object" && "message" in data)
            ? data.message
            : `요청 실패 (status: ${xhr.status})`;
          const error = new Error(message);
          Object.assign(error, { status: xhr.status });
          reject(error);
          return;
        }

        resolve(data);
      };

      xhr.onerror = () => {
        reject(new Error("네트워크 오류로 업로드에 실패했습니다."));
      };

      xhr.onabort = () => {
        reject(new DOMException("업로드가 취소되었습니다.", "AbortError"));
      };

      if (options?.signal) {
        if (options.signal.aborted) {
          xhr.abort();
        } else {
          options.signal.addEventListener("abort", () => xhr.abort());
        }
      }

      xhr.send(formData);
    });
  },

  downloadBatch: async (studyKeys: number[], seriesKeys: number[]): Promise<Blob> => {
    const response = await fetch("/api/research/dicom/download/batch", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ "study-keys": studyKeys, "series-keys": seriesKeys }),
    });

    if (!response.ok) {
      let message = `다운로드 실패 (status: ${response.status})`;
      try {
        const data = await response.json();
        if (data?.message) {
          message = data.message;
        }
      } catch {
        // JSON 응답이 아니면 기본 메시지를 사용한다.
      }
      throw new Error(message);
    }

    return response.blob();
  },
};

/**
 * 숨길 환자를 나타내는 인터페이스입니다.
 */
export interface HiddenPatientList {
  "patient-key": number;
  "hidden": boolean;
}

/**
 * 숨길 연구를 나타내는 인터페이스입니다.
 */
export interface HiddenStudyList {
  "study-key": number;
  "hidden": boolean;
}

/**
 * 숨길 시리즈를 나타내는 인터페이스입니다.
 */
export interface HiddenSeriesList {
  "series-key": number;
  "hidden": boolean;
}

/**
 * 연구 목적으로 허용된 연구를 나타내는 인터페이스입니다.
 */
export interface StudyResearchList {
  "study-key": number;
  "allow-research": boolean;
}

export default dicomApi;
