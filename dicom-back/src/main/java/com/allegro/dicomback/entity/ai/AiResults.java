package com.allegro.dicomback.entity.ai;

import com.allegro.dicomback.entity.Study;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "ai_results")
@Getter
@Setter
@NoArgsConstructor
public class AiResults {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //AI 결과 고유 번호
    @Column(name = "ResultKey")
    private Long resultKey;

    //StudyTab 외래키
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "StudyKey", nullable = false)
    private Study study;

    //AI 모델명
    @Column(name = "ModelName", length = 50)
    private String modelName;

    //추론 수행 시각
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    //추론 작업 끝나는 시간
    @Column(name = "FinishedAt")
    private LocalDateTime finishedAt;

    //추론 작업 현재 상태
    @Column(name = "Status", length = 150)
    private String status;






}
