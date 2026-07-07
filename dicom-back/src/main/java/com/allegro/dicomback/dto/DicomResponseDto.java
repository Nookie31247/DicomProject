package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

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

//    //익명화 DTo 필요없음
//    public record StudyAnonymizationDto(
//            @JsonProperty("study-key") Long studyKey
//    ) {}
}