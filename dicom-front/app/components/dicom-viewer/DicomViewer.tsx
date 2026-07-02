"use client";

import { useEffect, useRef, useState, useCallback } from "react";
import initCornerstone from "@/app/lib/cornerstoneInit";
import { dicomTagDictionary } from "./dicom-dictionary";
import {getTextDecoder, getElementDisplayValue} from "./dicom-charset"

interface DicomViewerProps {
  dicomUrls: string[];
  children?: React.ReactNode;
}

export default function DicomViewer({ dicomUrls, children }: DicomViewerProps) {
  const elementRef = useRef<HTMLDivElement>(null);
  
  const [isLoaded, setIsLoaded] = useState(false);
  const [isReady, setIsReady] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [currentIndex, setCurrentIndex] = useState(0);
  const [searchTerm, setSearchTerm] = useState("");
  const [metadata, setMetadata] = useState<Array<{tag: string; name: string; value: string}>>([]);
  
  const isInitialized = useRef(false);

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
      if (elementRef.current && cornerstone && isInitialized.current) {
        cornerstone.disable(elementRef.current);
        isInitialized.current = false;
      }
    };
  }, []);

  // 2. 이미지 로딩 및 프리페칭 (currentIndex 변경 시)
  useEffect(() => {
    if (!dicomUrls || dicomUrls.length === 0) return;
    if (!isInitialized.current) return; 
    
    let isCancelled = false;

    const loadTargetImage = async () => {
      try {
        if (!isLoaded) setIsLoaded(false);
        setError(null);
        
        const cornerstone = (await import("cornerstone-core")).default;
        const element = elementRef.current;
        if (!element) return;

        const url = `wadouri:${dicomUrls[currentIndex]}`;
        const image = await cornerstone.loadImage(url);
        
        if (isCancelled) return;

        cornerstone.displayImage(element, image);
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
                className="btn btn-small bg-[#3b82f6] text-white hover:bg-[#60a5fa] border-none shadow-md"
                onClick={handleReset}
              >
                초기화
              </button>
            </div>
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
