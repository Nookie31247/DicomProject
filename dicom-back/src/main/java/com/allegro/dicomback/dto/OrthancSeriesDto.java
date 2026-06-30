package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import java.util.List;

@Data
public class OrthancSeriesDto {
    @JsonProperty("ID")
    private String ID;

    @JsonProperty("Instances")
    private List<String> Instances; // 해당 시리즈에 포함된 이미지(Instance)들의 UUID 목록

    @JsonProperty("MainDicomTags")
    private MainDicomTags mainDicomTags;

    @Data
    public static class MainDicomTags {
        @JsonProperty("SeriesInstanceUID")
        private String seriesInstanceUID;

        @JsonProperty("SeriesNumber")
        private Integer seriesNumber;

        @JsonProperty("Modality")
        private String modality;

        @JsonProperty("BodyPartExamined")
        private String bodyPartExamined;

        @JsonProperty("SeriesDescription")
        private String seriesDescription;
    }
}