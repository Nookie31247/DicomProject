package com.allegro.dicomback.entity.ai;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

//Ai추론 실행(1회)를 기록
@Entity
@Table(name = "ai_results")
@Getter
@Setter
@NoArgsConstructor
public class AiResults {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //AI 결과 고유 번호 (PK)
    @Column(name = "ResultKey")
    private Long resultKey;

    //원래는 study이지만 현재 ai판독 요청 detect-raw는 뷰어에써 지금 화면에 떠 있는 사진(Dicom)한장을 호출하기에 변경된것
    //어떤 검사였다 보다 어느 시리즈의 몇번째 이미지인지 나타냄는 역할
    //SeriesKey 외래키
    @Column(name = "SeriesKey")
    private Long seriesKey;

    // AiModelRegistry.ModelRule.key() 값 (예: "whddid(종양)", "pneumonia(폐렴)")
    @Column(name = "ModelKey", length = 50)
    private String modelKey;

    //AI 모델명
    @Column(name = "ModelName", length = 50)
    private String modelName;

    //추론 수행 시각
    @Column(name = "CreatedAt")
    private LocalDateTime createdAt;

    //추론 작업 끝나는 시간
    @Column(name = "FinishedAt")
    private LocalDateTime finishedAt; //현재는 동기 처리라 createdAt과 거의 유사함

    // SUCCESS(박스 1개 이상) / NO_DETECTION(박스 0개) / ERROR(예외 발생) 중 하나
    @Column(name = "Status", length = 30)
    private String status;






}
