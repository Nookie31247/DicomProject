package com.allegro.backanonymization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * DICOM 요청을 위한 DTO입니다.
 */
public class DicomRequestDto {
    /**
     * 배치 다운로드 요청을 위한 DTO입니다.
     */
    public record BatchDownloadDto(
            @JsonProperty("study-keys") List<Long> studyKeys,
            @JsonProperty("series-keys") List<Long> seriesKeys
    ) {}
}
