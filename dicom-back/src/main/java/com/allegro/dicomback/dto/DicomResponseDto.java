package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class DicomResponseDto {

    // 환자 목록 응답 DTO
    public record PatientDto(
            @JsonProperty("patient-key")Long pId,
            @JsonProperty("patient-name")String pName,
            @JsonProperty("patient-birth")LocalDate pBirth,
            @JsonProperty("patient-sex")String pSex,
            @JsonProperty("latest-study-datetime") LocalDateTime pDateTime,
            @JsonProperty("study-count")Integer studyCount,
            @JsonProperty("hidden") boolean hidden
    ) {}

    // Study 목록 응답 DTO
    public record StudyDto(
            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("description")String description,
            @JsonProperty("datetime")LocalDateTime dateTime,
            @JsonProperty("series-num")Number seriesNum,
            @JsonProperty("images-num")Number imagesNum,
            @JsonProperty("allow-research")boolean allowedResearch,
            @JsonProperty("hidden")boolean hidden
    ) {}

    // Viewer 페이지의 Study 상세 응답 DTO (목록용 StudyDto와 달리, 연결된 환자 정보를 patient 객체로 함께 내려준다)
    public record StudyDetailDto(
            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("description")String description,
            @JsonProperty("datetime")LocalDateTime dateTime,
            @JsonProperty("series-num")Number seriesNum,
            @JsonProperty("images-num")Number imagesNum,
            @JsonProperty("allow-research")boolean allowedResearch,
            @JsonProperty("hidden")boolean hidden,
            @JsonProperty("patient") PatientSummaryDto patient
    ) {}

    // Study 상세에 함께 실리는 환자 요약 정보 (이름, 생년월일만)
    public record PatientSummaryDto(
            @JsonProperty("name") String name,
            @JsonProperty("birth") LocalDate birth
    ) {}

    // Series 목록 응답 DTO
    public record SeriesDto(
            @JsonProperty("series-key")Long seriesKey,
            @JsonProperty("series-index")Number seriesIndex,
            @JsonProperty("datetime")LocalDateTime dateTime,
            @JsonProperty("series-num")Number seriesNum,
            @JsonProperty("bodypart")String  bodyPart,
            @JsonProperty("hidden") boolean hidden
    ) {}

    // Image 목록 응답 DTO
    public record ImageDto(
            @JsonProperty("image-key")Long imageKey,
            @JsonProperty("image-index")Number imageIndex,
            @JsonProperty("datetime")LocalDateTime datetime,
            @JsonProperty("path")String path, // 뷰어에서 이미지를 로드하기 위한 파일 경로
            @JsonProperty("hidden")boolean hidden
    ) {}

    // 파일 업로드 결과 응답 DTO — 여러 파일을 한 번에 올릴 때
    // 어떤 파일이 성공했고 어떤 파일이 실패했는지 프론트에 요약해서 알려주기 위한 용도
    public record UploadResultDto(
            @JsonProperty("succeeded-files") List<String> succeededFiles, // 성공한 파일명 목록
            @JsonProperty("failed-files") List<String> failedFiles         // 실패한 파일명 목록
    ) {}

//    //익명화 DTo 필요없음
//    public record StudyAnonymizationDto(
//            @JsonProperty("study-key") Long studyKey
//    ) {}
}