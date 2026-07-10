"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import initCornerstone from "@/app/lib/cornerstoneInit";
import { dicomTagDictionary } from "./dicom-dictionary";
import {getTextDecoder, getElementDisplayValue} from "./dicom-charset"
import { medicalApiFetch } from "../../api/ApiFetch";

interface DicomViewerProps {
  dicomUrls: string[];
  children?: React.ReactNode;
}

// 백엔드 InferenceController.BoxDto와 동일
type AiBox = {
  x: number;
  y: number;
  width: number;
  height: number;
  confidence: number;
};

// modality/bodyPart만으로 모델을 하나로 못 좁혔을 때 서버가 후보 목록을 대신 내려주면 사용자가 직접 고르게 한다. (사실상 진짜 웬만해서 이게 안 나온다.)
type AiModelChoice = {
  key: string;
  displayName: string;
};

// boxes/candidates/unsupportedReason 중 하나만 채워짐
type DetectRawResponse = {
  boxes: AiBox[] | null;
  candidates: AiModelChoice[] | null;
  unsupportedReason: string | null;
};

// DICOM Overlay Plane(그룹 6000~601E, 짝수 그룹) 1개를 화면에 그리기 위해 미리 가공해둔 형태.
type OverlayPlane = {
  rows: number;
  cols: number;
  originRow0: number; // Overlay Origin을 0-based로 변환한 값
  originCol0: number; // 위와 동일, 시작 열
  canvas: HTMLCanvasElement;
};

// dicom-parser의 DataSet(ds)에서 Overlay Plane(그룹 6000, 6002, ... 601E - 짝수만 존재,최대 16개)을 전부 찾아서 오프스크린 캔버스로 미리 렌더링해둔다.
//
// DICOM Overlay Plane 구조(태그 그룹 앞자리는 60xx로 동일, 뒷자리 4자리로 의미가 정해짐):
//   (60xx,0010) Rows, (60xx,0011) Columns - 오버레이 비트맵의 크기
//   (60xx,0050) Overlay Origin - 이 오버레이가 이미지의 어느 위치(행,열)에서 시작하는지 (1-based)
//   (60xx,0100) Bits Allocated, (60xx,0102) Bit Position - 대부분 1비트/픽셀(값 1, 0)이라 그 외 형식은 건너뛴다
//   (60xx,3000) Overlay Data - 실제 비트맵. 픽셀 1개가 1비트이고, 바이트 안에서 최하위비트(LSB)가 그 바이트가 담당하는 8픽셀 중 첫 번째(가장 앞) 픽셀에 해당한다(DICOM PS3.5).
function extractOverlayPlanes(ds: any): OverlayPlane[] {
  const planes: OverlayPlane[] = [];

  for (let group = 0x6000; group <= 0x601e; group += 2) {
    const hex = group.toString(16).padStart(4, "0");
    const dataTag = `x${hex}3000`;

    // (60xx,3000)이 없으면 이 번호의 오버레이는 애초에 파일에 없는 것 - 다음 그룹으로 넘어간다.
    const dataElement = ds.elements?.[dataTag];
    if (!dataElement) continue;

    const rows = ds.uint16(`x${hex}0010`);
    const cols = ds.uint16(`x${hex}0011`);
    if (!rows || !cols) continue; // 크기 정보가 없으면 그릴 수 없으니 건너뜀

    // 대부분의 오버레이는 1비트/픽셀(Bits Allocated=1, Bit Position=0)이다.
    // 그 외 형식(드묾)은 이번 구현 범위 밖이라 방어적으로 건너뛴다.
    const bitsAllocated = ds.uint16(`x${hex}0100`) ?? 1;
    const bitPosition = ds.uint16(`x${hex}0102`) ?? 0;
    if (bitsAllocated !== 1 || bitPosition !== 0) continue;

    // Overlay Origin(60xx,0050)은 (row, col) 두 값을 가진 1-based 좌표다. 값이 없으면 기본값 (1,1).
    const originRow1Based = ds.int16(`x${hex}0050`, 0) ?? 1;
    const originCol1Based = ds.int16(`x${hex}0050`, 1) ?? 1;

    // 오버레이 원본 바이트(비트 패킹된 상태)를 파일 전체 바이트 배열에서 오프셋/길이로 잘라낸다.
    // dicom-parser는 이렇게 큰 바이너리 값(OW/OB/UN 등)을 위한 전용 getter가 없어서 이 방식이 표준적이다.
    const overlayBytes: Uint8Array = ds.byteArray.slice(
      dataElement.dataOffset,
      dataElement.dataOffset + dataElement.length
    );

    // 오프스크린 캔버스에 원본 해상도(rows x cols) 그대로 렌더링해둔다.
    // 비트가 1이면 잘 보이는 녹색으로 불투명하게, 0이면 완전 투명하게 칠해서 이미지 위에 겹쳐도
    // 아래 CT/X-ray 픽셀이 그대로 비쳐 보이도록 한다.
    const offCanvas = document.createElement("canvas");
    offCanvas.width = cols;
    offCanvas.height = rows;
    const offCtx = offCanvas.getContext("2d");
    if (!offCtx) continue;

    const imageData = offCtx.createImageData(cols, rows);
    const totalPixels = rows * cols;
    for (let i = 0; i < totalPixels; i++) {
      const byteIndex = i >> 3; // i / 8 (이 픽셀이 몇 번째 바이트에 들어있는지)
      const bitIndex = i & 7;   // i % 8 (그 바이트 안에서 몇 번째 비트인지, 0=최하위비트)
      const bit = (overlayBytes[byteIndex] >> bitIndex) & 1;
      const pixelOffset = i * 4; // RGBA 4바이트씩
      if (bit) {
        imageData.data[pixelOffset] = 0;       // R
        imageData.data[pixelOffset + 1] = 255; // G
        imageData.data[pixelOffset + 2] = 0;   // B
        imageData.data[pixelOffset + 3] = 255; // A - 완전 불투명
      } else {
        imageData.data[pixelOffset + 3] = 0;   // A - 완전 투명(꺼진 픽셀은 안 그림)
      }
    }
    offCtx.putImageData(imageData, 0, 0);

    planes.push({
      rows,
      cols,
      originRow0: originRow1Based - 1,
      originCol0: originCol1Based - 1,
      canvas: offCanvas,
    });
  }

  return planes;
}

export default function DicomViewer({ dicomUrls, children }: DicomViewerProps) {
  const elementRef = useRef<HTMLDivElement>(null);
  const cornerstoneRef = useRef<any>(null); // pixelToCanvas 계산에 동기적으로 재사용하기 위한 참조
  const overlayCanvasRef = useRef<HTMLCanvasElement>(null); // [신규] Overlay Plane을 그릴 실제 화면 캔버스

  const [isLoaded, setIsLoaded] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [metadata, setMetadata] = useState<Array<{tag: string; name: string; value: string}>>([]);
  const [overlayPlanes, setOverlayPlanes] = useState<OverlayPlane[]>([]); // [신규] 현재 이미지의 Overlay Plane들

  // AI 판독 관련 상태
  const [aiBoxes, setAiBoxes] = useState<AiBox[]>([]);
  const [aiLoading, setAiLoading] = useState(false);
  const [aiError, setAiError] = useState<string | null>(null);
  const [aiCandidates, setAiCandidates] = useState<AiModelChoice[] | null>(null); // 모델 자동 선택이 애매할 때 사용자에게 보여줄 후보 목록
  const [aiInfo, setAiInfo] = useState<string | null>(null); // 판독은 정상 완료됐지만 박스가 0개일 때(에러 아님) 안내 메시지
  const [renderTick, setRenderTick] = useState(0); // 줌/팬 등으로 캔버스가 다시 그려질 때마다 오버레이 좌표 재계산 트리거

  const isInitialized = useRef(false);
  const prevUrlsRef = useRef<string[] | null>(null); // 시리즈(=dicomUrls) 전환 감지용

  // 줌/팬/윈도우레벨 조작 시 cornerstone이 캔버스를 다시 그릴 때마다 발생하는 이벤트 핸들러.
  // useCallback으로 고정해야 addEventListener/removeEventListener에 같은 함수 참조를 넘길 수 있다.
  const handleCornerstoneRerender = useCallback(() => {
    setRenderTick((t) => t + 1);
  }, []);

  // 1. 코너스톤 초기화 및 뷰포트 활성화 (마운트 시 1회)
  useEffect(() => {
    let cornerstone: any;

    const initViewer = async () => {
      try {
        await initCornerstone();
        cornerstone = (await import("cornerstone-core")).default;
        const cornerstoneTools = (await import("cornerstone-tools")).default;

        const element = elementRef.current;
        if (!element || isInitialized.current) return;

        cornerstone.enable(element);
        cornerstoneRef.current = cornerstone; // pixelToCanvas를 렌더링 중에 동기적으로 쓰기 위해 저장

        // 줌/팬/윈도우레벨 등으로 캔버스가 다시 그려질 때마다 AI 박스 오버레이 좌표도 다시 계산
        element.addEventListener("cornerstoneimagerendered", handleCornerstoneRerender);

        // 도구 추가 (밝기/대비, 이동, 줌)
        const WwwcTool = cornerstoneTools.WwwcTool;
        const PanTool = cornerstoneTools.PanTool;
        const ZoomTool = cornerstoneTools.ZoomTool;

        cornerstoneTools.addTool(WwwcTool);
        cornerstoneTools.addTool(PanTool);
        cornerstoneTools.addTool(ZoomTool);

        // 도구 활성화 매핑
        cornerstoneTools.setToolActive("Wwwc", { mouseButtonMask: 1 }); // 좌클릭
        cornerstoneTools.setToolActive("Zoom", { mouseButtonMask: 2 }); // 우클릭
        cornerstoneTools.setToolActive("Pan", { mouseButtonMask: 4 });  // 휠클릭

        isInitialized.current = true;
        setIsReady(true);
      } catch (err: any) {
        console.error("Cornerstone Init Error:", err);
        setError("뷰어 초기화에 실패했습니다.");
      }
    };

    initViewer();

    return () => {
      if (elementRef.current) {
        elementRef.current.removeEventListener("cornerstoneimagerendered", handleCornerstoneRerender);
      }
      if (elementRef.current && cornerstone && isInitialized.current) {
        cornerstone.disable(elementRef.current);
        isInitialized.current = false;
      }
    };
  }, [handleCornerstoneRerender]);

  // 2. 이미지 로딩 및 프리페칭 (currentIndex 변경 시)
  useEffect(() => {
    if (!dicomUrls || dicomUrls.length === 0) return;
    if (!isInitialized.current) return;

    // dicomUrls 배열 참조가 바뀌었다 = 다른 시리즈를 선택했다는 뜻.
    // 이전 시리즈에서 90번째 이미지를 보고 있다가 이미지가 10장뿐인 시리즈로 넘어가면
    // currentIndex(90)가 새 배열 범위를 벗어나 dicomUrls[90]이 undefined가 되고 로드가 실패한다.
    // 그래서 시리즈가 바뀐 걸 감지하면 currentIndex를 0으로 되돌리고, 이번 실행은 로드 없이 종료한다.
    // (state 업데이트는 비동기라 여기서 바로 currentIndex를 신뢰할 수 없으므로 return 후 다음 렌더에서 로드)
    if (prevUrlsRef.current !== dicomUrls) {
      prevUrlsRef.current = dicomUrls;
      if (currentIndex !== 0) {
        setCurrentIndex(0);
        return;
      }
    }

    let isCancelled = false;

    const loadTargetImage = async () => {
      try {
        if (!isLoaded) setIsLoaded(false);
        setError(null);
        // 다른 이미지로 넘어가면 이전 이미지의 AI 판독 결과는 더 이상 유효하지 않으므로 초기화
        setAiBoxes([]);
        setAiError(null);
        setAiCandidates(null);
        setAiInfo(null);
        setOverlayPlanes([]);

        const cornerstone = (await import("cornerstone-core")).default;
        const element = elementRef.current;
        if (!element) return;

        const url = `wadouri:${dicomUrls[currentIndex]}`;
        const image = await cornerstone.loadImage(url);

        if (isCancelled) return;

        cornerstone.displayImage(element, image);
        const viewport = cornerstone.getViewport(element);
        if (viewport) {
          viewport.voi.windowWidth = image.windowWidth;
          viewport.voi.windowCenter = image.windowCenter;
          cornerstone.setViewport(element, viewport);
        }
        setIsLoaded(true);

        // 메타데이터 추출
        if (image.data && image.data.elements) {
          const ds = image.data;
          const decoder = getTextDecoder(ds);
          const allTags = Object.keys(ds.elements);
          const parsedMeta: Array<{ tag: string; name: string; value: string }> = [];

          for (const tag of allTags) {
            const formattedTag = `(${tag.substring(1, 5)},${tag.substring(5, 9)})`.toUpperCase();
            // let value = "";
            // try {
            //   value = ds.string(tag);
            // } catch (e) {
            //   // 무시 (파싱 불가 태그)
            // }
            const value = getElementDisplayValue(ds, tag, decoder);

            if (value && typeof value === "string" && value.trim() !== "") {
              const cleanValue = value.replace(/\0/g, "").trim();
              if (cleanValue) {
                const tagName = dicomTagDictionary[formattedTag] || "Unknown Tag";
                parsedMeta.push({ tag: formattedTag, name: tagName, value: cleanValue });
              }
            }
          }
          setMetadata(parsedMeta);

          // Overlay Plane(그룹 6000~601E) 추출. 장비가 픽셀 데이터와 별도로 저장해둔
          // 1비트 그래픽(주석/측정선 등)인데, cornerstone은 이걸 자동으로 그려주지 않으므로
          // 직접 파싱해서 오프스크린 캔버스에 미리 렌더링해둔다(실제 화면 배치는 아래 useEffect가 담당).
          setOverlayPlanes(extractOverlayPlanes(ds));
        }

        // 백그라운드 지연 로딩 (Prefetching) - 다음 10개
        for (let i = 1; i <= 10; i++) {
          const nextIndex = currentIndex + i;
          if (nextIndex < dicomUrls.length) {
            const nextUrl = `wadouri:${dicomUrls[nextIndex]}`;
            cornerstone.loadAndCacheImage(nextUrl).catch(() => {});
          }
        }

      } catch (err: any) {
        if (!isCancelled) {
          console.error("Image Load Error:", err);
          setError("이미지를 로드할 수 없습니다.");
        }
      }
    };

    loadTargetImage();

    return () => {
      isCancelled = true;
    };
  }, [dicomUrls, currentIndex, isReady]);

  // 메타데이터 실시간 검색 필터링
  const filteredMetadata = metadata.filter((item) => {
    const term = searchTerm.toLowerCase();
    return (
      item.tag.toLowerCase().includes(term) ||
      item.name.toLowerCase().includes(term) ||
      item.value.toLowerCase().includes(term)
    );
  });

  // 3. 키보드 및 휠 스크롤 핸들러
  const handleNext = useCallback(() => {
    setCurrentIndex((prev) => Math.min(prev + 1, dicomUrls.length - 1));
  }, [dicomUrls.length]);

  const handlePrev = useCallback(() => {
    setCurrentIndex((prev) => Math.max(prev - 1, 0));
  }, []);

  useEffect(() => {
    const handleKeyDown = (e: KeyboardEvent) => {
      // 입력창(input)에 포커스된 경우 단축키 동작을 무시하여 타이핑을 방해하지 않음
      if (document.activeElement?.tagName === "INPUT" || document.activeElement?.tagName === "TEXTAREA") {
        return;
      }

      if (e.key === "ArrowRight" || e.key === "ArrowDown") {
        e.preventDefault(); // 브라우저 기본 스크롤 동작 방지
        handleNext();
      } else if (e.key === "ArrowLeft" || e.key === "ArrowUp") {
        e.preventDefault();
        handlePrev();
      }
    };
    // passive: false 옵션을 주어 preventDefault가 정상 동작하도록 보장
    window.addEventListener("keydown", handleKeyDown, { passive: false });
    return () => window.removeEventListener("keydown", handleKeyDown);
  }, [handleNext, handlePrev]);

  const handleWheel = (e: React.WheelEvent) => {
    if (e.deltaY > 0) handleNext();
    else if (e.deltaY < 0) handlePrev();
  };

  const handleReset = () => {
    if (elementRef.current) {
      import("cornerstone-core").then((cornerstone) => {
        cornerstone.default.reset(elementRef.current);
      });
    }
  };

  // cornerstone의 타입드 배열(Int16Array/Uint16Array/Uint8Array) 버퍼를 base64 문자열로 변환.
  // JS 타입드 배열과 DICOM 둘 다 기본 little-endian이라 바이트 순서를 맞출 필요가 없다.
  const pixelDataToBase64 = (pixelData: ArrayLike<number> & { buffer: ArrayBufferLike; byteOffset: number; byteLength: number }) => {
    const byteView = new Uint8Array(pixelData.buffer, pixelData.byteOffset, pixelData.byteLength);
    let binary = "";
    for (let i = 0; i < byteView.length; i++) binary += String.fromCharCode(byteView[i]);
    return btoa(binary);
  };

  //현재 보고 있는 이미지의에서 seriesKey와 instanceId를 뽑아내고
  //백엔드가 AI 결과를 DB에 저장할 때 어떤 시리즈,이미지였는지필요함.
  //이미지 URL는 대충 예: /api/medical/dicom/series/12/instances/abcd.../file 이런 느낌
  const parseSeriesAndInstance = (url: string): { seriesKey: number | null; instanceId: string | null } => {
    const match = url.match(/\/api\/(?:medical\/)?dicom\/series\/(\d+)\/instances\/([^/]+)\/file/);
    if (!match) return { seriesKey: null, instanceId: null };
    return { seriesKey: Number(match[1]), instanceId: match[2] };
  };

  // 화면에 이미 디코딩되어 떠 있는 cornerstone 이미지의 픽셀 배열을 그대로 백엔드로 보낸다.
  // 서버가 Orthanc에서 DICOM을 다시 받아 압축을 푸는 과정이 통째로 필요 없어져서
  // dcm4che-imageio-opencv/OpenCV 네이티브 의존성 없이도 AI 판독이 동작한다.

  // modelKey: 서버가 modality/bodyPart만으로 모델을 하나로 못 좁혀서 candidates를 내려줬을 때 사용자가 그 중 하나를 직접 골라 재요청할 때만 채워서 보낸다. 처음 호출할 땐 항상 undefined.
  const handleAiDetect = async (modelKey?: string) => {
    const cornerstone = cornerstoneRef.current;
    const element = elementRef.current;
    if (!cornerstone || !element) return;

    const image = cornerstone.getEnabledElement(element)?.image;
    if (!image) {
      setAiError("현재 이미지 정보를 확인할 수 없습니다.");
      return;
    }

    setAiLoading(true);
    setAiError(null);
    setAiCandidates(null);
    setAiInfo(null);
    try {
      const pixelData = image.getPixelData(); // Int16Array | Uint16Array | Uint8Array
      const signed = pixelData instanceof Int16Array;

      // image.windowCenter/windowWidth는 태그가 있든 없든 항상 숫자를 반환한다(없으면 cornerstone이
      // 화면 표시용으로 자체 계산해서 채워 넣음) - 그래서 이 값만으로는 "진짜 태그가 있는지"를 구분할 수 없다.
      // 원본 dicomParser 데이터셋(image.data)에서 (0028,1050)/(0028,1051) 태그가 실제로 존재하는지
      // 직접 확인해서, 있으면 그 값을 그대로 보내고 없으면 null을 보낸다(서버가 null일 때만 자체 계산).
      const ds = image.data;
      const hasWindowTags = !!(ds?.elements?.["x00281050"] && ds?.elements?.["x00281051"]);
      const windowCenter = hasWindowTags ? ds.floatString("x00281050", 0) : null;
      const windowWidth = hasWindowTags ? ds.floatString("x00281051", 0) : null;

      // Modality(0008,0060)/BodyPartExamined(0018,0015)는 서버의 AiModelRegistry가 어떤 onnx 모델을 자동으로 고를지 판단하는 데 사용
      // 모달리티와 몸파트가 둘 다 없으면 undefined로 보내고 지원 안 함으로 응답하게 둔다
      const modality = ds?.string?.("x00080060") || null;
      const bodyPart = ds?.string?.("x00180015") || null;

      const { seriesKey, instanceId } = parseSeriesAndInstance(dicomUrls[currentIndex]);

      //현제 보고 있는 화면 dicom의 URL에서 seriesKey/instanceId를 뽑아낸다.
      //dicomUrls는 viewer/[id]/page.tsx가 "/api/medical/dicom/series/{seriesKey}/instances/{id}/file" 형태로
      //이미 만들어서 내려주고 있어서 그 문자열만 파싱하면 되서 별도 prop을 추가할 필요는 없음
      const res: DetectRawResponse = await medicalApiFetch("/api/medical/ai/detect-raw", {
        method: "POST",
        credentials: "include",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({
          rows: image.rows,
          cols: image.columns,
          windowCenter,
          windowWidth,
          slope: image.slope ?? 1,
          intercept: image.intercept ?? 0,
          signed,
          pixelDataBase64: pixelDataToBase64(pixelData),
          modality,
          bodyPart,
          modelKey: modelKey ?? null,
          seriesKey,
          instanceId,
        }),
      });

      if (res.unsupportedReason) {
        setAiError(res.unsupportedReason);
        setAiBoxes([]);
      } else if (res.candidates && res.candidates.length > 0) {
        // 자동으로 하나로 못 좁혀짐 - 사용자가 직접 고르게 선택지를 보여준다
        setAiCandidates(res.candidates);
      } else {
        const boxes = res.boxes ?? [];
        setAiBoxes(boxes);
        // 에러는 아니지만 판독 결과 박스가 0개인 경우 작동(아무것도 안 나오면 사용자가 헷갈림)
        if (boxes.length === 0) {
          setAiInfo("이상 소견이 발견되지 않았습니다.");
        }
      }
    } catch (e) {
      setAiError(e instanceof Error ? e.message : "AI 판독에 실패했습니다.");
      setAiBoxes([]);
    } finally {
      setAiLoading(false);
    }
  };

  // 원본 DICOM 이미지 픽셀 좌표 -> 지금 화면에 그려진 캔버스 좌표로 변환.
  // cornerstone.pixelToCanvas가 현재 줌/팬 상태를 반영해서 계산해주므로,
  // renderTick이 바뀔 때마다(줌/팬 발생 시) 이 값도 다시 계산되게 하면 박스가 이미지에 계속 붙어있게 된다.
  const getCanvasRect = (box: AiBox) => {
    const cornerstone = cornerstoneRef.current;
    const element = elementRef.current;
    if (!cornerstone || !element) return null;

    try {
      const topLeft = cornerstone.pixelToCanvas(element, { x: box.x, y: box.y });
      const bottomRight = cornerstone.pixelToCanvas(element, { x: box.x + box.width, y: box.y + box.height });
      return {
        x: topLeft.x,
        y: topLeft.y,
        width: bottomRight.x - topLeft.x,
        height: bottomRight.y - topLeft.y,
      };
    } catch {
      return null; // 아직 이미지가 표시되기 전이면 pixelToCanvas가 실패할 수 있음
    }
  };

  // Overlay Plane을 실제 화면 캔버스(overlayCanvasRef)에 그린다.
  // AI 박스와 같은 이유로 renderTick(줌/팬/윈도우레벨 변경)이 바뀔 때마다 다시 그려야
  // 오버레이가 이미지 위에 계속 정확한 위치로 붙어있는다. 이미지가 바뀌어 overlayPlanes가
  // 새로 채워질 때도 당연히 다시 그려야 하므로 두 값 모두 의존성 배열에 넣는다.
  useEffect(() => {
    const canvas = overlayCanvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;

    // 캔버스 해상도를 실제 화면에 표시된 크기(CSS 픽셀)에 맞춘다. 안 맞추면 흐릿하게 늘어나거나
    // 잘려 보인다. 매번 새로 만들 필요는 없고 값이 바뀌었을 때만 갱신한다.
    const displayWidth = canvas.clientWidth;
    const displayHeight = canvas.clientHeight;
    if (canvas.width !== displayWidth) canvas.width = displayWidth;
    if (canvas.height !== displayHeight) canvas.height = displayHeight;

    ctx.clearRect(0, 0, canvas.width, canvas.height);

    const element = elementRef.current;
    const cornerstone = cornerstoneRef.current;
    if (!element || !cornerstone || overlayPlanes.length === 0) return;

    for (const plane of overlayPlanes) {
      try {
        // 오버레이 비트맵의 좌상단/우하단이 "원본 이미지 픽셀 좌표계"에서 어디에 해당하는지 구한 뒤,
        // 그 두 점을 지금 화면(줌/팬 반영된 캔버스 좌표계)으로 변환해서 그 사각형 안에 그려 넣는다.
        const topLeft = cornerstone.pixelToCanvas(element, { x: plane.originCol0, y: plane.originRow0 });
        const bottomRight = cornerstone.pixelToCanvas(element, {
          x: plane.originCol0 + plane.cols,
          y: plane.originRow0 + plane.rows,
        });
        ctx.drawImage(
          plane.canvas,
          topLeft.x,
          topLeft.y,
          bottomRight.x - topLeft.x,
          bottomRight.y - topLeft.y
        );
      } catch {
        // 아직 이미지가 표시되기 전이면 pixelToCanvas가 실패할 수 있음 - 그냥 건너뜀
      }
    }
  }, [overlayPlanes, renderTick]);

  return (
    <div className="flex w-full h-full gap-5">
      {/* 뷰어 캔버스 영역 */}
      <div
        className="relative flex-1 bg-black rounded-xl overflow-hidden shadow-inner flex items-center justify-center"
        onWheel={handleWheel}
      >
        {!isLoaded && !error && (
          <div className="absolute inset-0 flex flex-col items-center justify-center text-white z-10 bg-black/50 backdrop-blur-sm">
            <div className="w-10 h-10 border-4 border-mint-deep border-t-transparent rounded-full animate-spin mb-4"></div>
            <p className="font-semibold text-sm text-mint-deep tracking-wider">LOADING DICOM...</p>
          </div>
        )}

        {error && (
          <div className="absolute inset-0 flex flex-col items-center justify-center z-10 bg-black/80 p-6 text-center">
            <div className="text-red-400 text-4xl mb-3">⚠️</div>
            <p className="font-semibold text-red-300">{error}</p>
          </div>
        )}

        <div
          ref={elementRef}
          className="w-full h-full"
          onContextMenu={(e) => e.preventDefault()}
          onMouseDown={(e) => {
            // 마우스 휠 클릭(가운데 버튼, button === 1) 시 브라우저 자동 스크롤 모드 진입 방지
            if (e.button === 1) e.preventDefault();
          }}
        ></div>

        {/* [신규] DICOM Overlay Plane 렌더링용 캔버스. SVG 대신 <canvas>를 쓰는 이유:
            오버레이는 512x512 같은 픽셀 단위 비트맵이라, 픽셀마다 SVG <rect>를 그리면
            (최대 26만 개 이상) 브라우저가 감당을 못 한다. 실제 그리기는 위쪽 useEffect가 담당. */}
        <canvas
          ref={overlayCanvasRef}
          className="absolute inset-0 w-full h-full pointer-events-none z-10"
        />

        {/* AI 판독 박스 오버레이 - 이미지 위에 겹쳐서 그리되, 마우스 조작은 그대로 캔버스로 통과시킴 */}
        {isLoaded && aiBoxes.length > 0 && (
          <svg className="absolute inset-0 w-full h-full pointer-events-none z-10">
            {aiBoxes.map((box, idx) => {
              const rect = getCanvasRect(box);
              if (!rect) return null;
              return (
                <g key={idx}>
                  <rect
                    x={rect.x}
                    y={rect.y}
                    width={rect.width}
                    height={rect.height}
                    fill="none"
                    stroke="#ff3b3b"
                    strokeWidth={2}
                  />
                  <text
                    x={rect.x}
                    y={rect.y > 14 ? rect.y - 4 : rect.y + 14}
                    fill="#ff3b3b"
                    fontSize={12}
                    fontWeight="bold"
                  >
                    {(box.confidence * 100).toFixed(1)}%
                  </text>
                </g>
              );
            })}
          </svg>
        )}

        {/* 툴 사용법 및 컨트롤 버튼 오버레이 */}
        {isInitialized.current && (
          <div className="absolute top-4 left-4 z-10 flex flex-col gap-3.5">
            <div className="flex gap-2 text-[11px] font-medium text-white/70 pointer-events-none select-none">
              <span className="bg-black/60 px-2 py-1 rounded">좌클릭: 밝기/대비</span>
              <span className="bg-black/60 px-2 py-1 rounded">우클릭: 줌</span>
              <span className="bg-black/60 px-2 py-1 rounded">휠클릭: 이동</span>
              <span className="bg-black/60 px-2 py-1 rounded">휠/방향키: 이전/다음</span>
            </div>
            <div className="flex flex-col items-start gap-2 w-21 *:w-full">
              {children}
              <button
                type="button"
                className="btn btn-small shadow-md"
                onClick={() => handleAiDetect()}
                disabled={aiLoading || !isLoaded}
              >
                {aiLoading ? "판독 중..." : "AI 판독"}
              </button>
              <button
                type="button"
                className="btn btn-small bg-[#3b82f6] text-white hover:bg-[#60a5fa] border-none shadow-md"
                onClick={handleReset}
              >
                초기화
              </button>
            </div>
            {/* modality/bodyPart만으로 모델을 하나로 못 좁혔을 때 사용자가 직접 모델을 선택 */}
            {aiCandidates && aiCandidates.length > 0 && (
              <div className="flex flex-col gap-1 bg-black/70 rounded p-2 max-w-40">
                <span className="text-white text-[11px] font-semibold">판독 모델 선택:</span>
                {aiCandidates.map((c) => (
                  <button
                    key={c.key}
                    type="button"
                    className="btn btn-small shadow-md text-left"
                    onClick={() => handleAiDetect(c.key)}
                    disabled={aiLoading}
                  >
                    {c.displayName}
                  </button>
                ))}
              </div>
            )}
            {aiError && (
              <span className="bg-red-500/80 text-white text-[11px] px-2 py-1 rounded max-w-40">
                {aiError}
              </span>
            )}
            {/* 에러는 아니지만 판독 결과가 0건일 때 조용히 넘어가면 판독 자체가 안 된 건지 헷갈리므로 안내 */}
            {aiInfo && (
              <span className="bg-black/70 text-white text-[11px] px-2 py-1 rounded max-w-40">
                {aiInfo}
              </span>
            )}
          </div>
        )}

        {/* 이미지 스크롤 카운터 */}
        {dicomUrls.length > 0 && (
          <div className="absolute bottom-4 right-4 z-10 bg-black/60 text-white px-3 py-1.5 rounded-lg text-sm font-semibold pointer-events-none">
            {currentIndex + 1} / {dicomUrls.length}
          </div>
        )}
      </div>

      {/* 우측 메타데이터 패널 */}
      <div className="w-64 shrink-0 bg-paper border border-line rounded-xl flex flex-col overflow-hidden max-[900px]:hidden">
        <div className="p-4 border-b border-line shrink-0 bg-canvas flex flex-col gap-3">
          <h3 className="font-bold text-[15px] text-ink m-0 tracking-wide">DICOM METADATA</h3>
          <input
            type="text"
            placeholder="태그, 설명, 값 검색..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full bg-paper border border-line rounded-lg px-2.5 py-1.5 text-xs text-ink placeholder:text-slate-400 focus:outline-none focus:border-mint-deep transition-colors"
          />
        </div>

        <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-3 scrollbar-thin scrollbar-thumb-slate-300">
          {filteredMetadata.length > 0 ? (
            filteredMetadata.map((item, idx) => (
              <div key={idx} className="flex flex-col gap-0.5 border-b border-line/50 pb-2 last:border-0 last:pb-0">
                <div className="flex items-center gap-1.5 flex-wrap">
                  <span className="text-[#14b876] font-mono text-[11px] font-bold">{item.tag}</span>
                  <span className="text-ink-soft text-[11px] font-semibold">{item.name}</span>
                </div>
                <span className="text-ink text-[12.5px] font-medium break-all leading-[1.3] mt-0.5">{item.value}</span>
              </div>
            ))
          ) : (
            <span className="text-ink-soft text-sm">검색 결과가 없습니다.</span>
          )}
        </div>
      </div>
    </div>
  );
}
