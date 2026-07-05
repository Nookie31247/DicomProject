package com.allegro.dicomback.dto;

import com.allegro.dicomback.entity.Study;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class StudyAllocationDto {
    private Long studyKey;
    private String description;
    private String studyDateTime;
    private Integer seriesNum;
    private Integer imagesNum;
    private boolean isAssigned; // ★ 핵심: 할당 여부 체크용

    // Study 엔티티로부터 DTO를 생성하는 정적 메서드
    public static StudyAllocationDto fromEntity(Study study, boolean isAssigned) {
        return StudyAllocationDto.builder()
                .studyKey(study.getKey())
                .description(study.getDescription())
                .studyDateTime(study.getCreatedAt().toString())
                .isAssigned(isAssigned)
                .build();
    }
}