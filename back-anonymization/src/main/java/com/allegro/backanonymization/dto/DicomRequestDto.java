package com.allegro.backanonymization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public class DicomRequestDto {
    public record BatchDownloadDto(
            @JsonProperty("study-keys") List<Long> studyKeys,
            @JsonProperty("series-keys") List<Long> seriesKeys
    ) {}
}
