package com.allegro.backanonymization.entity.ai;

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
    private Long ResultKey;

    //StudyTab 외래키
    @Column(name = "StudyKey")
    private String StudyKey;

    //AI 모델명
    @Column(name = "ModelName", length = 50)
    private String ModelName;

    //추론 수행 시각
    @Column(name = "CreatedAt")
    private LocalDateTime CreatedAt;

    //추론 작업 끝나는 시간
    @Column(name = "FinishedAt")
    private LocalDateTime FinishedAt;

    //추론 작업 현재 상태
    @Column(name = "Status", length = 150)
    private String Status;






}
