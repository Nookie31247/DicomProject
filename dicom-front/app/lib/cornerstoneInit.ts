let isInitialized = false;

export default async function initCornerstone() {
  if (typeof window === "undefined" || isInitialized) return;

  try {
    const cornerstone = (await import("cornerstone-core")).default;
    const cornerstoneTools = (await import("cornerstone-tools")).default;
    const cornerstoneMath = (await import("cornerstone-math")).default;
    const cornerstoneWADOImageLoader = (await import("cornerstone-wado-image-loader")).default;
    const dicomParser = (await import("dicom-parser")).default;
    const Hammer = (await import("hammerjs")).default;

    // 1. External dependencies 설정
    cornerstoneWADOImageLoader.external.cornerstone = cornerstone;
    cornerstoneWADOImageLoader.external.dicomParser = dicomParser;
    cornerstoneTools.external.cornerstone = cornerstone;
    cornerstoneTools.external.Hammer = Hammer;
    cornerstoneTools.external.cornerstoneMath = cornerstoneMath;

    // 2. Cornerstone-tools 초기화
    cornerstoneTools.init({
      globalToolSyncEnabled: true,
      showSVGCursors: true,
    });

    // 3. Web Worker 설정 (WASM 디코딩을 위함)
    const config = {
      maxWebWorkers: navigator.hardwareConcurrency || 1,
      startWebWorkersOnDemand: true,
      taskConfiguration: {
        decodeTask: {
          initializeCodecsOnStartup: false,
          usePDFJS: false,
          strict: false,
        },
      },
    };

    cornerstoneWADOImageLoader.webWorkerManager.initialize(config);

    // ★ 팁: 백엔드(Spring)와 통신 시 인증 토큰이 필요하다면 아래와 같이 헤더를 추가할 수 있습니다.
    /*
    cornerstoneWADOImageLoader.configure({
      beforeSend: function(xhr) {
        const token = localStorage.getItem('token');
        if (token) xhr.setRequestHeader('Authorization', `Bearer ${token}`);
      }
    });
    */

    isInitialized = true;
  } catch (error) {
    console.error("Cornerstone initialization failed:", error);
  }
}
