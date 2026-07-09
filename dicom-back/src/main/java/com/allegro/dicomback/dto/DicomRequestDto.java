package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.List;

/**
 * DICOM 요청을 위한 데이터 전송 객체(DTO)입니다.
 */
public class DicomRequestDto {

    /**
     * 목록에서 환자의 가시성을 구성하기 위한 DTO입니다.
     *
     * @param patientKey 환자의 고유 식별자
     * @param hidden 환자를 숨길지 여부
     */
    public record PatientHideDto(
            @JsonProperty("patient-key") Long patientKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 목록에서 검사(study)의 가시성을 구성하기 위한 DTO입니다.
     *
     * @param studyKey 검사(study)의 고유 식별자
     * @param hidden 검사(study)를 숨길지 여부
     */
    public record StudyHideDto(
            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 검사(study)가 연구 목적으로 허용되는지 여부를 구성하기 위한 DTO입니다.
     *
     * @param studyKey 검사(study)의 고유 식별자
     * @param allowResearch 검사(study)가 연구 목적으로 허용되는지 여부
     */
    public record StudyResearchDto(
            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("allow-research") boolean allowResearch
    ) {}

    /**
     * 목록에서 시리즈의 가시성을 구성하기 위한 DTO입니다.
     *
     * @param seriesKey 시리즈의 고유 식별자
     * @param hidden 시리즈를 숨길지 여부
     */
    public record SeriesHideDto(
            @JsonProperty("series-key") Long seriesKey,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 환자 정보를 업데이트하거나 요청하기 위한 DTO입니다.
     *
     * @param name 환자의 이름
     * @param sex 환자의 성별
     * @param birth 환자의 생년월일
     */
    public record PatientRequestDto(
            @JsonProperty("patient-name")
            String name,

            @JsonProperty("patient-sex")
            String sex,

            @JsonProperty("patient-birth")
            @DateTimeFormat(pattern = "yyyy-MM-dd")
            LocalDate birth
    ) {}

    /**
     * 단일 ZIP 파일로 여러 검사(study) 및 시리즈의 일괄 다운로드를 요청하기 위한 DTO입니다.
     * 연구 데이터 다운로드 페이지에서 사용됩니다.
     *
     * @param studyKeys 다운로드할 검사(study)의 고유 식별자 목록
     * @param seriesKeys 다운로드할 시리즈의 고유 식별자 목록
     */
    public record BatchDownloadDto(
            @JsonProperty("study-keys") List<Long> studyKeys,
            @JsonProperty("series-keys") List<Long> seriesKeys
    ) {}
}
