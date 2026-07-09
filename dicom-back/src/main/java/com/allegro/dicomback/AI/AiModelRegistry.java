package com.allegro.dicomback.AI;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * 애플리케이션에서 사용되는 AI 모델을 위한 레지스트리입니다.
 */
@Component
public class AiModelRegistry {

    // 실제 등록된 모델 목록. 모델이 늘어나면 이 목록에 한 줄씩 추가하면 됨.
    // bodyPartKeywords가 null이면 "이 modality면 bodyPart 상관없이 이 모델" (경쟁하는 모델이 없을 때만 유효)
    private final List<ModelRule> rules = List.of(
            new ModelRule("tumor", "뇌종양(CT/MR)", Set.of("CT", "MR"), null,
                    Path.of("models/best.onnx"), 0.1f),
            new ModelRule("pneumonia", "폐렴(CR/DX)", Set.of("CR", "DX"), Set.of("CHEST", "LUNG"),
                    Path.of("models/CR_pneumonia_yolov8n.onnx"), 0.26f)
    );

    /**
     * 특정 모델에 대한 규칙을 나타내는 레코드입니다.
     * 하나의 모델이 어떤 modality/bodyPart를 대상으로 하는지 정의
     * 여기 있는 confidenceThreshold는 어가 쓰는 /detect-raw 경로에서 쓰이는 값
     * resolve()가 모델을 확정하면 그 모델 전용 threshold를 같이 돌려줘서
     * inferOnRawPixels() 호출할 때 그 값을 그대로 넘김
     *
     * @param key 모델 키
     * @param displayName 모델 표시 이름
     * @param modalities 지원되는 모달리티 세트
     * @param bodyPartKeywords 신체 부위 키워드 세트
     * @param modelPath 모델 파일 경로
     * @param confidenceThreshold 신뢰도 임계값
     */
    public record ModelRule(String key, String displayName, Set<String> modalities,
                            Set<String> bodyPartKeywords, Path modelPath, float confidenceThreshold) {

        /**
         * 규칙이 주어진 모달리티와 일치하는지 확인합니다.
         *
         * @param modality 확인할 모달리티
         * @return 모달리티가 일치하면 true, 그렇지 않으면 false
         */
        boolean matchesModality(String modality) {
            return modality != null && modalities.contains(modality.toUpperCase());
        }

        /**
         * 규칙이 주어진 신체 부위와 일치하는지 확인합니다.
         * bodyPartKeywords가 null이면 이 규칙은 bodyPart를 안 가림 (항상 true)
         * 실제로는 해당 modality에 경쟁 규칙이 없을 때만 이 분기까지 옴
         *
         * @param bodyPart 확인할 신체 부위
         * @return 신체 부위가 일치하면 true, 그렇지 않으면 false
         */
        boolean matchesBodyPart(String bodyPart) {
            if (bodyPartKeywords == null) return true;
            if (bodyPart == null || bodyPart.isBlank()) return false;
            String upper = bodyPart.toUpperCase();
            return bodyPartKeywords.stream().anyMatch(upper::contains);
        }
    }

    /**
     * 모델 확정 결과를 나타내는 인터페이스입니다.
     * 해석 결과 3종: 확정됨 / 지원 안 함(modality 자체가 없거나 등록 안 됨) / 애매해서 사용자 선택 필요
     */
    public sealed interface Result permits Resolved, Unsupported, Ambiguous {}
    public record Resolved(ModelRule rule) implements Result {}
    public record Unsupported(String reason) implements Result {}
    public record Ambiguous(List<ModelRule> candidates) implements Result {}

    /**
     * 모달리티와 신체 부위를 기반으로 적절한 모델 규칙을 확정합니다.
     *
     * @param modality 이미지의 모달리티
     * @param bodyPart 이미지의 신체 부위
     * @return 확정 결과 (Resolved, Unsupported 또는 Ambiguous)
     */
    public Result resolve(String modality, String bodyPart) {
        // Modality 자체가 없으면 아예 추측하지 않는다
        if (modality == null || modality.isBlank()) {
            return new Unsupported("Modality 태그가 없어 AI 모델을 선택할 수 없습니다.");
        }


        // Modality로  필터링
        List<ModelRule> candidates = rules.stream()
                .filter(r -> r.matchesModality(modality))
                .toList();

        if (candidates.isEmpty()) {
            return new Unsupported("지원하지 않는 모달리티입니다: " + modality);
        }
        if (candidates.size() == 1) {
            return new Resolved(candidates.get(0)); // 경쟁 모델 없으니 bodyPart 안 봐도 확정
        }

        // 후보가 여러 개일 때만 bodyPart로 좁힌다
        List<ModelRule> narrowed = candidates.stream()
                .filter(r -> r.matchesBodyPart(bodyPart))
                .toList();

        if (narrowed.size() == 1) {
            return new Resolved(narrowed.get(0));
        }

        // bodyPart가 없거나, 여러 후보에 다 걸리거나, 아무데도 안 걸리면 → 자동 결정 포기
        return new Ambiguous(candidates);
    }

    /**
     * 키로 모델 규칙을 찾습니다.
     * 사용자가 Ambiguous 응답을 받은 뒤 직접 모델을 선택했을 때, key로 강제 지정하기 위한 조회
     *
     * @param key 모델 키
     * @return 규칙을 찾은 경우 해당 규칙을 포함하는 Optional, 그렇지 않으면 빈 Optional
     */
    public Optional<ModelRule> findByKey(String key) {
        return rules.stream().filter(r -> r.key().equals(key)).findFirst();
    }
}