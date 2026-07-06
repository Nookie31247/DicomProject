"use client";

// ── 스타일 변수 ──
const wsPanelClass = "flex min-h-0 flex-col overflow-hidden bg-paper border border-line rounded-[20px]";
const wsPanelHeadClass = "flex shrink-0 items-start justify-between gap-3 pt-4.5 px-4.5 pb-4 border-b border-line";
const wsHeadLeftClass = "flex min-w-0 flex-1 flex-col gap-1.25";
const wsPanelTitleClass = "m-0 font-bold text-xl text-ink tracking-[-0.01em]";
const wsSubLabelClass = "text-left font-medium text-sm text-ink-soft leading-[1.4]";
const colDescClass = "col-desc overflow-hidden whitespace-nowrap font-semibold text-ellipsis";
const colDateClass = "col-date text-ink-soft";
const colSeriesClass = "col-series text-right tabular-nums text-ink-soft";
const colImagesClass = "col-images text-right tabular-nums text-ink-soft";
const modalityBadgeClass = "inline-flex items-center justify-center font-bold min-w-10.5 px-2 py-1 rounded-lg text-xs tracking-[0.02em] text-paper";
const modalityColors: Record<string, string> = { ct: "bg-[#2563eb]", mr: "bg-[#7c3aed]", cr: "bg-[#0e7490]", us: "bg-[#c2410c]", pt: "bg-[#be185d]" };
const studyGridColumns = "30px 84px 1.6fr 1fr 1fr 70px 70px 100px";

export default function StudyList({selectedPatient}) {
  return(
    <section className={`${wsPanelClass} flex flex-col`}>
      <div className={wsPanelHeadClass}>
        <div className={`${wsHeadLeftClass} w-full`}>
          <div className="flex items-center justify-between w-full">
            <h2 className={wsPanelTitleClass}>검사 목록</h2>
            <button type="button" className="btn btn-medium">파일 추가</button>
          </div>
          {selectedPatient && <span className={wsSubLabelClass}>{selectedPatient.pname} 선택됨</span>}
        </div>
      </div>
      <div className="flex min-h-0 flex-1 flex-col overflow-hidden">
        <div className="grid items-center gap-2.5 shrink-0 font-bold py-3 px-5 text-xs text-ink-soft bg-canvas border-b border-line" style={{ gridTemplateColumns: studyGridColumns }}>
          <span></span><span>모달리티</span><span className={colDescClass}>검사 설명</span><span className="col-body">검사 부위</span><span className={colDateClass}>검사 일자</span><span className={colSeriesClass}>시리즈</span><span className={colImagesClass}>영상 수</span><span>연구 활용</span>
        </div>
        <ul className="min-h-0 flex-1 list-none overflow-y-auto m-0 p-1.5">
          {displayedStudies.map((it) => (
              <li key={it["study-key"]}>
                <div className="study-row grid items-center gap-2.5 w-full cursor-pointer p-3.5 border-b" style={{ gridTemplateColumns: studyGridColumns }}>
                  <input type="checkbox" checked={checkedStudyIds.has(it["study-key"])} onChange={() => toggleStudyCheck(it["study-key"])} />
                  <span className={modalityBadgeClass + " " + modalityColors[it.modality.toLowerCase()]}>{it.modality}</span>
                  <span className={colDescClass}>{it.description}</span>
                  <span className={colDateClass}>{it.datetime.split("T")[0]}</span>
                  <span className={colSeriesClass}>{it["series-num"]}</span>
                  <span className={colImagesClass}>{it["images-num"]}</span>
                  <span className={it["allow-research"] ? "text-green-600" : "text-red-600"}>{it["allow-research"] ? "예" : "아니오"}</span>
                </div>
              </li>
          ))}
        </ul>
      </div>
    </section>
  )
}