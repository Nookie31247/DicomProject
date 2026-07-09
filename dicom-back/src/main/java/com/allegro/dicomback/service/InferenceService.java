package com.allegro.dicomback.service;


import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.allegro.dicomback.AI.Preprocessor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

@Service
public class InferenceService {
    private final Preprocessor preprocessor;

    // YOLOv8/11 표준 입력 크기는 640x640
    private static final int YOLO_INPUT_SIZE = 640;
    // 모델마다 threshold가 달라서 AiModelRegistry.ModelRule이 값을 들고 다니게 바꿈.
    // AiModelRegistry를 안 거치는 레거시 경로 전용 기본값
    // 레거시 경로(/infer, /visualize)만 이 기본값을 그대로 씀.
    // 디버그용 레거시 엔드포인트에서만 적용되는 별개의 기본값
    private static final float DEFAULT_CONFIDENCE_THRESHOLD = 0.25f;
    // NMS: 두 박스가 이 비율 이상 겹치면 같은 대상으로 보고 낮은 확률 쪽을 제거
    private static final float NMS_IOU_THRESHOLD = 0.45f;

    // YOLO가 뱉어낼 결과물 그릇 (박스 좌표와 확률) - 640x640 모델 좌표 기준
    InferenceService(Preprocessor p) { this.preprocessor = p; }

    // YOLO가 뱉어낼 결과물 그릇 (박스 좌표와 확률) - 640x640 모델 좌표 기준
    public record BoundingBox(float x_min, float y_min, float x_max, float y_max, float confidence) {}

    //  로컬 파일 경로로 추론, 640x640 좌표 그대로 반환 (/api/medical/ai/infer, /api/medical/ai/visualize 용)
    // inferAndRescale()을 태워서 여기서 바로 원본 좌표로 반환
    public List<BoundingBox> infer(Path dicomPath, Path modelPath) throws Exception {
        var t = preprocessor.preprocess(dicomPath, YOLO_INPUT_SIZE);
        return inferAndRescale(t, modelPath, DEFAULT_CONFIDENCE_THRESHOLD);
    }

    // 뷰어 연동용: Orthanc에서 받은 byte[]로 추론하고, 결과를 640 좌표가 아니라 원본 DICOM 이미지 픽셀 좌표로 변환해서 반환한다. 프론트는 이 좌표를 그대를 cornerstone.pixelToCanvas()에 넘긴다.
    // 압축 DICOM을 서버가 다시 디코딩해야 하므로 dcm4che-imageio-opencv가 필요함
    public List<BoundingBox> inferOnOriginalImage(byte[] dicomBytes, Path modelPath) throws Exception {
        var t = preprocessor.preprocess(dicomBytes, YOLO_INPUT_SIZE);
        return inferAndRescale(t, modelPath, DEFAULT_CONFIDENCE_THRESHOLD);
    }

    // 뷰어 연동용현재 사용: cornerstone가 이미 압축을 풀어놓은 픽셀 배열을 그대로 받아서 추론
    // 서버가 DICOM을 다시 디코딩할 필요가 없음
    // windowCenter/windowWidth는 null이면 preprocessRaw가 백분위수 기준으로 계산한다
    public List<BoundingBox> inferOnRawPixels(int rows, int cols, Double windowCenter, Double windowWidth,
                                              double slope, double intercept, boolean signed,
                                              byte[] pixelBytes, Path modelPath, float confidenceThreshold) throws Exception {
        var t = preprocessor.preprocessRaw(rows, cols, windowCenter, windowWidth, slope, intercept, signed,
                pixelBytes, YOLO_INPUT_SIZE);
        return inferAndRescale(t, modelPath, confidenceThreshold);
    }

    // 640x640 모델 좌표 결과를 원본 이미지 픽셀 좌표로 되돌린다 (letterbox 패딩/스케일 역산)
    private List<BoundingBox> inferAndRescale(Preprocessor.Tensor t, Path modelPath, float confidenceThreshold) throws Exception {
        List<BoundingBox> boxesIn640Space = runModelAndDecode(t, modelPath, confidenceThreshold);

        List<BoundingBox> rescaled = new ArrayList<>();
        for (BoundingBox b : boxesIn640Space) {
            rescaled.add(new BoundingBox(
                    (b.x_min() - t.padX()) / t.scale(),
                    (b.y_min() - t.padY()) / t.scale(),
                    (b.x_max() - t.padX()) / t.scale(),
                    (b.y_max() - t.padY()) / t.scale(),
                    b.confidence()
            ));
        }
        return rescaled;
    }

    // 모델 로드 + 추론 + confidence 필터링 + NMS까지 처리하는 공통 로직
    private List<BoundingBox> runModelAndDecode(Preprocessor.Tensor t, Path modelPath, float confidenceThreshold) throws Exception {
        List<BoundingBox> rawBoxes = new ArrayList<>();

        try (var env = OrtEnvironment.getEnvironment();
             var session = env.createSession(modelPath.toString(), new OrtSession.SessionOptions())) {

            String inputName = session.getInputNames().iterator().next();
            var tensor = OnnxTensor.createTensor(env,
                    java.nio.FloatBuffer.wrap(t.data()), t.shape());

            try (var results = session.run(Map.of(inputName, tensor))) {
                // YOLO의 출력 모양은 일반적으로 [1, 분류+좌표(예: 84), 그리드수(예: 8400)] 형태입니다.
                float[][][] out = (float[][][]) results.get(0).getValue();
                float[][] predictions = out[0];

                int numClasses = predictions.length - 4; // 보통 첫 4개는 x, y, w, h 입니다.
                int numAnchors = predictions[0].length;

                // 8400개의 예측 박스들을 모두 뒤져서 확률이 높은 진짜 종양 박스만 걸러냅니다.
                for (int i = 0; i < numAnchors; i++) {
                    float maxClassProb = 0;
                    for (int c = 0; c < numClasses; c++) {
                        float prob = predictions[4 + c][i];
                        if (prob > maxClassProb) {
                            maxClassProb = prob;
                        }
                    }

                    if (maxClassProb > confidenceThreshold) {
                        float x_center = predictions[0][i];
                        float y_center = predictions[1][i];
                        float width = predictions[2][i];
                        float height = predictions[3][i];

                        float x_min = x_center - (width / 2);
                        float y_min = y_center - (height / 2);
                        float x_max = x_center + (width / 2);
                        float y_max = y_center + (height / 2);

                        rawBoxes.add(new BoundingBox(x_min, y_min, x_max, y_max, maxClassProb));
                    }
                }
            }
        }
        return nonMaxSuppression(rawBoxes);
    }

    // 겹치는 박스들 중 confidence가 가장 높은 것만 남기고 나머지는 제거
    private List<BoundingBox> nonMaxSuppression(List<BoundingBox> boxes) {
        List<BoundingBox> sorted = new ArrayList<>(boxes);
        sorted.sort(Comparator.comparingDouble(BoundingBox::confidence).reversed());

        List<BoundingBox> kept = new ArrayList<>();
        for (BoundingBox candidate : sorted) {
            boolean overlapsWithKept = false;
            for (BoundingBox already : kept) {
                if (iou(candidate, already) > NMS_IOU_THRESHOLD) {
                    overlapsWithKept = true;
                    break;
                }
            }
            if (!overlapsWithKept) {
                kept.add(candidate);
            }
        }
        return kept;
    }

    // 두 박스가 겹치는 비율(Intersection over Union) 계산
    private float iou(BoundingBox a, BoundingBox b) {
        float interXMin = Math.max(a.x_min(), b.x_min());
        float interYMin = Math.max(a.y_min(), b.y_min());
        float interXMax = Math.min(a.x_max(), b.x_max());
        float interYMax = Math.min(a.y_max(), b.y_max());

        float interWidth = Math.max(0, interXMax - interXMin);
        float interHeight = Math.max(0, interYMax - interYMin);
        float interArea = interWidth * interHeight;

        float areaA = Math.max(0, a.x_max() - a.x_min()) * Math.max(0, a.y_max() - a.y_min());
        float areaB = Math.max(0, b.x_max() - b.x_min()) * Math.max(0, b.y_max() - b.y_min());
        float unionArea = areaA + areaB - interArea;

        return unionArea <= 0 ? 0 : interArea / unionArea;
    }
}
