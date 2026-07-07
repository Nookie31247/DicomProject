package com.allegro.backanonymization.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnonymizationRequestDto {
    @JsonProperty("uid")
    private String uid;

    @JsonProperty("patient-birth")
    private LocalDate patientBirth;

    @JsonProperty("patient-sex")
    private String patientSex;

    @JsonProperty("description")
    private String description;

    @JsonProperty("series")
    private List<SeriesData> series;

    public record SeriesData(
            @JsonProperty("uid") String uid,
            @JsonProperty("series-num") int seriesNum,
            @JsonProperty("body-part") String bodyPart,
            @JsonProperty("modality") String modality,
            @JsonProperty("total-images-count") int totalImagesCount
    ) {}
}