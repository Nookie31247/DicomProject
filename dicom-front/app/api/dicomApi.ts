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

  uploadDicomFiles: async (formData: FormData) => {
    const res = await fetch("/api/dicom/upload", {
      method: "POST",
      body: formData,
    });
    if (!res.ok) throw new Error("파일 업로드 서버 오류");
    return res.json();
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
