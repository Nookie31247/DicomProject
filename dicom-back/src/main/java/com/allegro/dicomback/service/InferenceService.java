package com.allegro.dicomback.service;


import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtSession;
import com.allegro.dicomback.AI.Preprocessor;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class InferenceService {
    private final Preprocessor preprocessor;

    // YOLOv8/11 표준 입력 크기는 640x640
    private static final int YOLO_INPUT_SIZE = 640;
    // AI가 CONFIDENCE_THRESHOLD의 수치만큰 확신할 때만 박스로 인정
    private static final float CONFIDENCE_THRESHOLD = 0.1f;

    InferenceService(Preprocessor p) { this.preprocessor = p; }

    // YOLO가 뱉어낼 결과물 그릇 (박스 좌표와 확률)
    public record BoundingBox(float x_min, float y_min, float x_max, float y_max, float confidence) {}

    public List<BoundingBox> infer(Path dicomPath, Path modelPath) throws Exception {
        // 전처리 (YOLO 크기인 640x640으로 리사이즈 및 정규화)
        var t = preprocessor.preprocess(dicomPath, YOLO_INPUT_SIZE);

        List<BoundingBox> finalBoxes = new ArrayList<>();

        // 모델 로드 & 추론
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

                    // 확률이 50% (0.5) 이상인 녀석들만 박스로 만듭니다.
                    if (maxClassProb > CONFIDENCE_THRESHOLD) {
                        float x_center = predictions[0][i];
                        float y_center = predictions[1][i];
                        float width = predictions[2][i];
                        float height = predictions[3][i];

                        float x_min = x_center - (width / 2);
                        float y_min = y_center - (height / 2);
                        float x_max = x_center + (width / 2);
                        float y_max = y_center + (height / 2);

                        finalBoxes.add(new BoundingBox(x_min, y_min, x_max, y_max, maxClassProb));
                    }
                }
            }
        }

        // 추가 과제: 여기서 겹치는 박스들을 제거하는 NMS(Non-Maximum Suppression) 로직이 들어가야 완벽합니다!
        return finalBoxes;
    }
}