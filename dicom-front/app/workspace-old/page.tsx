"use client";

import { useState, type CSSProperties } from "react";
import Link from "next/link";
import { useRouter } from "next/navigation";
import { patients, currentUser, type DicomItem } from "../workspace/mock-data";

export default function WorkspacePage() {
  // 선택된 환자 / 선택된 DICOM 항목 상태.
  // 기본값으로 첫 환자를 선택해 테스트 시 화면이 비어보이지 않게 합니다.
  const [selectedPatientId, setSelectedPatientId] = useState(patients[0].id);
  const [selectedItemId, setSelectedItemId] = useState<string | null>(null);

  // 패널 접힘 상태 (DICOM 이미지를 크게 보기 위함)
  const [patientsCollapsed, setPatientsCollapsed] = useState(false);
  const [studiesCollapsed, setStudiesCollapsed] = useState(false);

  const selectedPatient =
    patients.find((p) => p.id === selectedPatientId) ?? null;

  const selectedItem: DicomItem | null =
    selectedPatient?.items.find((it) => it.id === selectedItemId) ?? null;

  // 환자를 바꾸면 뷰어 선택은 초기화.
  function handleSelectPatient(id: string) {
    setSelectedPatientId(id);
    setSelectedItemId(null);
  }

  const router = useRouter();

  // 인증 미구현: 실제 로그아웃 처리는 추후 연동. 지금은 홈으로 이동만.
  function handleLogout() {
    router.push("/");
  }

  const sexLabel = (s: "M" | "F") => (s === "M" ? "남" : "여");

  // 접힘 상태에 따라 그리드 컬럼 너비를 동적으로 계산.
  // CSS 변수로 넘겨 반응형 미디어쿼리가 인라인 스타일을 덮어쓸 수 있게 함.
  const gridColumns = [
    patientsCollapsed ? "52px" : "290px",
    studiesCollapsed ? "52px" : "minmax(380px, 1.1fr)",
    "1.35fr",
  ].join(" ");

  const workspaceStyle = { "--ws-grid": gridColumns } as CSSProperties;

  return (
    <div className="page">
      {/* ───────────── Nav ───────────── */}
      <header className="nav">
        <Link href="/public" className="logo">
          DICOM!
        </Link>
        <div className="nav-user">
          <span className="user-avatar">{currentUser.name.charAt(0)}</span>
          <span className="user-name">{currentUser.name}님</span>
          <button
            type="button"
            className="logout-btn"
            onClick={handleLogout}
          >
            로그아웃
          </button>
        </div>
      </header>

      {/* ───────────── Workspace ───────────── */}
      <section
        className={`workspace ${patientsCollapsed ? "patients-collapsed" : ""} ${
          studiesCollapsed ? "studies-collapsed" : ""
        }`}
        style={workspaceStyle}
      >
        {/* ── 1. 환자 목록 (DB에 저장된 환자) ── */}
        <aside
          className={`ws-panel patients-panel ${
            patientsCollapsed ? "collapsed" : ""
          }`}
        >
          {patientsCollapsed ? (
            <button
              type="button"
              className="ws-rail"
              onClick={() => setPatientsCollapsed(false)}
              aria-label="환자 목록 펼치기"
              title="환자 목록 펼치기"
            >
              <span className="ws-rail-icon">›</span>
              <span className="ws-rail-label">환자 목록</span>
            </button>
          ) : (
            <>
              <div className="ws-panel-head">
                <div className="ws-head-left">
                  <div className="ws-title-row">
                    <h2 className="ws-panel-title">환자 목록</h2>
                    <span className="ws-count">{patients.length}명</span>
                  </div>
                </div>
                <button
                  type="button"
                  className="ws-collapse-btn"
                  onClick={() => setPatientsCollapsed(true)}
                  aria-label="환자 목록 접기"
                  title="접기"
                >
                  ‹
                </button>
              </div>
              <ul className="patient-list">
                {patients.map((p) => (
                  <li key={p.id}>
                    <button
                      type="button"
                      className={`patient-row ${
                        p.id === selectedPatientId ? "active" : ""
                      }`}
                      onClick={() => handleSelectPatient(p.id)}
                    >
                      <span className="patient-avatar">{p.name.charAt(0)}</span>
                      <span className="patient-main">
                        <span className="patient-name">{p.name}</span>
                        <span className="patient-sub">
                          {sexLabel(p.sex)} · {p.birthDate}
                        </span>
                      </span>
                      <span className="patient-badge">{p.items.length}</span>
                    </button>
                  </li>
                ))}
              </ul>
            </>
          )}
        </aside>

        {/* ── 2. 검사 목록 (선택한 환자의 DICOM 파일) ── */}
        <section
          className={`ws-panel studies-panel ${
            studiesCollapsed ? "collapsed" : ""
          }`}
        >
          {studiesCollapsed ? (
            <button
              type="button"
              className="ws-rail"
              onClick={() => setStudiesCollapsed(false)}
              aria-label="검사 목록 펼치기"
              title="검사 목록 펼치기"
            >
              <span className="ws-rail-icon">›</span>
              <span className="ws-rail-label">검사 목록</span>
            </button>
          ) : (
            <>
              <div className="ws-panel-head">
                <div className="ws-head-left">
                  <div className="ws-title-row">
                    <h2 className="ws-panel-title">검사 목록</h2>
                  </div>
                  {selectedPatient ? (
                    <span className="ws-sub-label">
                      {selectedPatient.name} · {sexLabel(selectedPatient.sex)} ·{" "}
                      {selectedPatient.birthDate} · {selectedPatient.patientId}
                    </span>
                  ) : (
                    <span className="ws-sub-label">환자를 선택하세요</span>
                  )}
                </div>
                <button
                  type="button"
                  className="ws-collapse-btn"
                  onClick={() => setStudiesCollapsed(true)}
                  aria-label="검사 목록 접기"
                  title="접기"
                >
                  ‹
                </button>
              </div>

              {selectedPatient ? (
                <div className="study-table">
                  <div className="study-head">
                    <span className="col-modality">모달리티</span>
                    <span className="col-desc">검사 설명</span>
                    <span className="col-body">검사 부위</span>
                    <span className="col-date">검사 일자</span>
                    <span className="col-series">시리즈</span>
                    <span className="col-images">영상 수</span>
                  </div>
                  <ul className="study-list">
                    {selectedPatient.items.map((it) => (
                      <li key={it.id}>
                        <button
                          type="button"
                          className={`study-row ${
                            it.id === selectedItemId ? "active" : ""
                          }`}
                          onClick={() => setSelectedItemId(it.id)}
                        >
                          <span className="col-modality">
                            <span
                              className={`modality-badge mod-${it.modality.toLowerCase()}`}
                            >
                              {it.modality}
                            </span>
                          </span>
                          <span className="col-desc">{it.description}</span>
                          <span className="col-body">{it.bodyPart}</span>
                          <span className="col-date">{it.studyDate}</span>
                          <span className="col-series">#{it.seriesNumber}</span>
                          <span className="col-images">{it.images}</span>
                        </button>
                      </li>
                    ))}
                  </ul>
                </div>
              ) : (
                <div className="ws-empty">왼쪽에서 환자를 선택해 주세요.</div>
              )}
            </>
          )}
        </section>

        {/* ── 3. DICOM 뷰어 ── */}
        <section className="ws-panel viewer-panel">
          <div className="ws-panel-head">
            <div className="ws-head-left">
              <div className="ws-title-row">
                <h2 className="ws-panel-title">DICOM 뷰어</h2>
              </div>
              {selectedItem && (
                <span className="ws-sub-label">
                  {selectedItem.modality} · {selectedItem.description}
                </span>
              )}
            </div>
          </div>

          <div className="viewer-stage">
            <div className="scan-frame">
              <div className="scan-grid" />
              <div className="scan-line" />
              <div className="scan-corner tl" />
              <div className="scan-corner tr" />
              <div className="scan-corner bl" />
              <div className="scan-corner br" />
              <span className="scan-tag">
                {selectedItem
                  ? `${selectedItem.modality} · SERIES ${selectedItem.seriesNumber} · ${selectedItem.images} IMAGES`
                  : "NO IMAGE SELECTED"}
              </span>
            </div>

            {selectedItem ? (
              <dl className="meta-grid">
                <div className="meta-row">
                  <dt>검사 설명</dt>
                  <dd>{selectedItem.description}</dd>
                </div>
                <div className="meta-row">
                  <dt>모달리티</dt>
                  <dd>{selectedItem.modality}</dd>
                </div>
                <div className="meta-row">
                  <dt>검사 부위</dt>
                  <dd>{selectedItem.bodyPart}</dd>
                </div>
                <div className="meta-row">
                  <dt>검사 일자</dt>
                  <dd>{selectedItem.studyDate}</dd>
                </div>
                <div className="meta-row">
                  <dt>시리즈 번호</dt>
                  <dd>#{selectedItem.seriesNumber}</dd>
                </div>
                <div className="meta-row">
                  <dt>영상 수</dt>
                  <dd>{selectedItem.images} 장</dd>
                </div>
                <div className="meta-row meta-uid">
                  <dt>Study UID</dt>
                  <dd>{selectedItem.studyInstanceUID}</dd>
                </div>
              </dl>
            ) : (
              <p className="viewer-hint">
                가운데 검사 목록에서 DICOM 파일을 선택하면
                <br />이 영역에서 미리보기와 상세 정보를 볼 수 있습니다.
              </p>
            )}
          </div>
        </section>
      </section>
    </div>
  );
}
