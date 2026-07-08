import {apiFetch} from "@/app/api/apiFetch";

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

    return apiFetch(
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
    return apiFetch(`/api/dicom/studies?${queryString}`);
  },

  getSeries(studyKey: number) {
    return apiFetch(`/api/dicom/series?study-key=${studyKey}`);
  },

  getResearchStudies() {
    return apiFetch(`/api/dicom/studies/research`);
  },

  getSeriesForResearch(studyKey: number) {
    return apiFetch(`/api/dicom/studies/${studyKey}/series`);
  },

  setPatientHide(list: HiddenPatientList[]) {
    return apiFetch("/api/dicom/patients/hide", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  setStudyHide(list: HiddenStudyList[]) {
    return apiFetch("/api/dicom/studies/hide", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  setSeriesHide(list: HiddenSeriesList[]) {
    return apiFetch("/api/dicom/series/hide", {
      method: 'POST',
      headers: {"Content-Type": "application/json"},
      body: JSON.stringify(list),
    });
  },

  addPatient(patient: { name: string; sex: string; birth: string }) {
    return apiFetch("/api/dicom/patients", {
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
  uploadDicomFiles: (formData: FormData) => {
    return apiFetch("/api/dicom/upload", {
      method: "POST",
      body: formData,
    });
  }
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

export default dicomApi;
