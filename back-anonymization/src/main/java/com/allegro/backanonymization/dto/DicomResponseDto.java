package com.allegro.backanonymization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;


/**
 * DICOM 응답을 위한 DTO입니다.
 */
public class DicomResponseDto {

    /**
     * 환자 정보를 위한 DTO입니다.
     */
    public record PatientDto(
            @JsonProperty("patient-key") Long pId,
            @JsonProperty("patient-name") String pName,
            @JsonProperty("patient-birth") LocalDate pBirth,
            @JsonProperty("patient-sex") String pSex,
            @JsonProperty("latest-study-datetime") LocalDateTime pDateTime,
            @JsonProperty("study-count") Integer studyCount,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 연구 정보를 위한 DTO입니다.
     */
    public record StudyDto(
            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("description") String description,
            @JsonProperty("datetime") LocalDateTime dateTime,
            @JsonProperty("series-num") Number seriesNum,
            @JsonProperty("images-num") Number imagesNum,
            @JsonProperty("allow-research") boolean allowedResearch,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 시리즈 정보를 위한 DTO입니다.
     */
    public record SeriesDto(
            @JsonProperty("series-key") Long seriesKey,
            @JsonProperty("series-index") Number seriesIndex,
            @JsonProperty("datetime") LocalDateTime dateTime,
            @JsonProperty("series-num") Number seriesNum,
            @JsonProperty("bodypart") String bodyPart,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 인스턴스 정보를 위한 DTO입니다.
     */
    public record InstanceInfoDto(
            @JsonProperty("instance-id") String instanceId,
            @JsonProperty("number-of-frames") int numberOfFrames
    ) {}
}
