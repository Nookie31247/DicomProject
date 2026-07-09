import { medicalApiFetch, researchApiFetch } from "./ApiFetch";

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
  ): Promise<unknown> => {
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

export interface HiddenPatientList {
  "patient-key": number;
  "hidden": boolean;
}

export interface HiddenStudyList {
  "study-key": number;
  "hidden": boolean;
}

export interface HiddenSeriesList {
  "series-key": number;
  "hidden": boolean;
}

export interface StudyResearchList {
  "study-key": number;
  "allow-research": boolean;
}

export default dicomApi;
