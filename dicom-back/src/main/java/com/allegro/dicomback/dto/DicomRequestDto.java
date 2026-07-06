package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class DicomRequestDto {
    //환자 목록 숨기기/보이기 설정
    public record PatientHideDto(
            @JsonProperty("patient-key") Long patientKey,
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
}
