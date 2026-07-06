package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DicomRequestDto {
    //환자 목록 숨기기/보이기 설정
    public record PatientHideDto(
            @JsonProperty("patient-id") Long patientKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

    //스터디 목록 숨기기/보이기 설정
    public record StudyHideDto(

            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

    //시리즈 목록 숨기기/보이기 설정
    public record SeriesHideDto(
            @JsonProperty("series-key") Long seriesKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

    //이미지 목록 숨기기/보이기 설정
    public record ImageHideDto(
            @JsonProperty("image-key") Long imageKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

//    //스터디 다운로드 DTO 필요없음
//    public record StudyDownloadDto(
//            @JsonProperty("study-key") String studyKey
//    ) {}
//
//    //시리즈 다운로드
//    public record SeriesDownloadDto(
//            @JsonProperty("series-key") String seriesKey
//    ) {}
//
//    //이미지 다운로드
//    public record ImageDownloadDto(
//            @JsonProperty("image-key") String imageKey
//    ) {}
}
