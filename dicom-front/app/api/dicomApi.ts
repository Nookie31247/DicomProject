import {ApiFetch} from "./ApiFetch";

const dicomApi = {
  getPatients(start: string | null, end: string | null, search:string | null) {
    const params = new URLSearchParams();
    if (search !== null && search !== "") {
      params.append('search', search);
    }
    if (start !== null && start !== "" && end !== null && end !== "") {
      params.append('start', start);
      params.append('end', end);
    }

    const queryString = params.toString();

    return ApiFetch(
        queryString
            ? `/api/dicom/patients?${queryString}`
            : `/api/dicom/patients`
    );
  },
  getStudies(patientId: number, start: string | null, end: string | null, search:string | null) {
    const params = new URLSearchParams();
    if (search !== null && search !== "") {
      params.append('search', search);
    }
    if (start !== null && start !== "" && end !== null && end !== "") {
      params.append('start', start);
      params.append('end', end);
    }
    params.append("patient-key", patientId.toString());
    const queryString = params.toString();
    return ApiFetch(`/api/dicom/studies?${queryString}`);
  },

  getSeries(studyKey: number) {
    return ApiFetch(`/api/dicom/series?study-key=${studyKey}`);
  },

  getResearchStudies() {
    return ApiFetch(`/api/dicom/studies/research`);
  },

  getSeriesForResearch(studyKey: number) {
    return ApiFetch(`/api/dicom/studies/${studyKey}/series`);
  },

  setPatientHide(list: HiddenPatientList[]) {
    return ApiFetch("/api/dicom/patients/hide", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  setStudyHide(list: HiddenStudyList[]) {
    return ApiFetch("/api/dicom/studies/hide", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  setSeriesHide(list: HiddenSeriesList[]) {
    return ApiFetch("/api/dicom/series/hide", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  setStudyResearch(list: StudyResearchList[]) {
    return ApiFetch("/api/dicom/studies/research-allow", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  addPatient(patient: { name: string; sex: string; birth: string }) {
    return ApiFetch("/api/dicom/patients", {
      method: 'POST',
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({
        "patient-name": patient.name,
        "patient-sex": patient.sex,
        "patient-birth": patient.birth
      }),
    });
  },

  // /api/dicom/upload는 app/api/dicom/upload/route.ts가 직접 처리한다(Next rewrite의
  // multipart 유실 버그 우회용 커스텀 프록시). apiFetch는 그대로 상대경로로 호출하면 된다.
  //
  // 업로드 진행률(%)은 fetch로는 알 수 없어서(업로드 바이트 진행 이벤트가 없음) 이 함수만
  // XMLHttpRequest로 직접 구현한다. 응답 파싱/에러 형태는 apiFetch와 최대한 동일하게 맞췄다.
  // signal은 로그아웃 시 업로드를 취소하기 위한 AbortSignal이고, onProgress는 0~1 사이의
  // 진행률을 전달한다(lengthComputable이 false인 드문 경우엔 -1을 전달해 "진행률 알 수 없음"을 표시).
  uploadDicomFiles: (
      formData: FormData,
      options?: { signal?: AbortSignal; onProgress?: (ratio: number) => void },
  ): Promise<unknown> => {
    return new Promise((resolve, reject) => {
      const xhr = new XMLHttpRequest();
      xhr.open("POST", "/api/dicom/upload");

      xhr.upload.onprogress = (e) => {
        if (!options?.onProgress) {
          return;
        }
        options.onProgress(e.lengthComputable ? e.loaded / e.total : -1);
      };

      xhr.onload = () => {
        if (xhr.status === 204) {
          resolve(null);
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

  // 연구 자료 다운로드 페이지: 체크된 study/series 여러 개를 zip 하나로 묶어 받는다.
  // body가 있는 POST에 응답도 바이너리(zip)라서 텍스트 응답을 전제하는 apiFetch를 못 쓰고 fetch를 직접 써서 blob으로 받음
  downloadBatch: async (studyKeys: number[], seriesKeys: number[]): Promise<Blob> => {
    const response = await fetch("/api/dicom/download/batch", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      credentials: "include",
      body: JSON.stringify({ "study-keys": studyKeys, "series-keys": seriesKeys }),
    });
    if (!response.ok) {
      // 백엔드가 내려주는 실제 에러 메시지(ErrorResponse.message)가 있으면 그걸 그대로 보여준다.
      // 예: 연구원 계정이 다운로드를 시도하면 "연구원 계정은 익명화 다운로드 기능이..." 메시지가 옴.
      // JSON 파싱이 안 되는 경우(네트워크 레벨 에러 등)엔 기존처럼 상태 코드만 표시.
      let message = `다운로드 실패 (status: ${response.status})`;
      try {
        const data = await response.json();
        if (data?.message) message = data.message;
      } catch {
        // 응답이 JSON이 아니면 기본 메시지 사용
      }
      throw new Error(message);
    }
    return response.blob();
  },
}

export interface HiddenPatientList {
  "patient-key" : number;
  "hidden" : boolean;
}

export interface HiddenStudyList {
  "study-key" : number;
  "hidden" : boolean;
}

export interface HiddenSeriesList {
  "series-key" : number;
  "hidden" : boolean;
}

export interface StudyResearchList {
  "study-key" : number;
  "allow-research" : boolean;
}

export default dicomApi;
