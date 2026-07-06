package com.allegro.dicomback.AI;

import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.Set;

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

    // 하나의 모델이 어떤 modality/bodyPart를 대상으로 하는지 정의
    // 여기 있는 confidenceThreshold는 어가 쓰는 /detect-raw 경로에서 쓰이는 값
    // resolve()가 모델을 확정하면 그 모델 전용 threshold를 같이 돌려줘서
    // inferOnRawPixels() 호출할 때 그 값을 그대로 넘김
    public record ModelRule(String key, String displayName, Set<String> modalities,
                            Set<String> bodyPartKeywords, Path modelPath, float confidenceThreshold) {

        boolean matchesModality(String modality) {
            return modality != null && modalities.contains(modality.toUpperCase());
        }

        // bodyPartKeywords가 null이면 이 규칙은 bodyPart를 안 가림 (항상 true)
        // 실제로는 해당 modality에 경쟁 규칙이 없을 때만 이 분기까지 옴
        boolean matchesBodyPart(String bodyPart) {
            if (bodyPartKeywords == null) return true;
            if (bodyPart == null || bodyPart.isBlank()) return false;
            String upper = bodyPart.toUpperCase();
            return bodyPartKeywords.stream().anyMatch(upper::contains);
        }
    }

    // 해석 결과 3종: 확정됨 / 지원 안 함(modality 자체가 없거나 등록 안 됨) / 애매해서 사용자 선택 필요
    public sealed interface Result permits Resolved, Unsupported, Ambiguous {}
    public record Resolved(ModelRule rule) implements Result {}
    public record Unsupported(String reason) implements Result {}
    public record Ambiguous(List<ModelRule> candidates) implements Result {}

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

    // 사용자가 Ambiguous 응답을 받은 뒤 직접 모델을 선택했을 때, key로 강제 지정하기 위한 조회
    public Optional<ModelRule> findByKey(String key) {
        return rules.stream().filter(r -> r.key().equals(key)).findFirst();
    }
}