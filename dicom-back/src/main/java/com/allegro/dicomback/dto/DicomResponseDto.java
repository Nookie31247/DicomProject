package com.allegro.dicomback.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DICOM 응답을 위한 데이터 전송 객체(DTO)입니다.
 */
public class DicomResponseDto {

    /**
     * 환자 목록 응답을 위한 DTO입니다.
     *
     * @param pId 환자의 고유 식별자
     * @param pName 환자의 이름
     * @param pBirth 환자의 생년월일
     * @param pSex 환자의 성별
     * @param pDateTime 환자의 최신 검사(study) 날짜 및 시간
     * @param studyCount 환자와 연결된 총 검사(study) 수
     * @param hidden 환자가 숨김으로 표시되었는지 여부
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
     * 검사(study) 목록 응답을 위한 DTO입니다.
     *
     * @param studyKey 검사(study)의 고유 식별자
     * @param description 검사(study)에 대한 설명
     * @param dateTime 검사(study) 날짜 및 시간
     * @param seriesNum 검사(study)의 시리즈 수
     * @param imagesNum 검사(study)의 총 이미지 수
     * @param allowedResearch 검사(study)가 연구 목적으로 허용되는지 여부
     * @param hidden 검사(study)가 숨김으로 표시되었는지 여부
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
     * 뷰어 페이지에서 사용되는 상세한 검사(study) 응답을 위한 DTO입니다.
     * StudyDto와 달리, 이 객체는 연결된 환자의 요약 정보를 포함합니다.
     *
     * @param studyKey 검사(study)의 고유 식별자
     * @param description 검사(study)에 대한 설명
     * @param dateTime 검사(study) 날짜 및 시간
     * @param seriesNum 검사(study)의 시리즈 수
     * @param imagesNum 검사(study)의 총 이미지 수
     * @param allowedResearch 검사(study)가 연구 목적으로 허용되는지 여부
     * @param hidden 검사(study)가 숨김으로 표시되었는지 여부
     * @param patient 요약된 환자 정보
     */
    public record StudyDetailDto(
            @JsonProperty("study-key") Long studyKey,
            @JsonProperty("description") String description,
            @JsonProperty("datetime") LocalDateTime dateTime,
            @JsonProperty("series-num") Number seriesNum,
            @JsonProperty("images-num") Number imagesNum,
            @JsonProperty("allow-research") boolean allowedResearch,
            @JsonProperty("hidden") boolean hidden,
            @JsonProperty("patient") PatientSummaryDto patient
    ) {}

    /**
     * 요약된 환자 정보(이름과 생년월일만)를 위한 DTO로,
     * 일반적으로 상세한 검사(study) 응답에 포함됩니다.
     *
     * @param name 환자의 이름
     * @param birth 환자의 생년월일
     */
    public record PatientSummaryDto(
            @JsonProperty("name") String name,
            @JsonProperty("birth") LocalDate birth
    ) {}

    /**
     * 시리즈 목록 응답을 위한 DTO입니다.
     *
     * @param seriesKey 시리즈의 고유 식별자
     * @param seriesIndex 시리즈의 인덱스
     * @param dateTime 시리즈 날짜 및 시간
     * @param seriesNum 시리즈 번호
     * @param bodyPart 시리즈에서 검사된 신체 부위
     * @param description 시리즈에 대한 설명
     * @param hidden 시리즈가 숨김으로 표시되었는지 여부
     */
    public record SeriesDto(
            @JsonProperty("series-key") Long seriesKey,
            @JsonProperty("series-index") Number seriesIndex,
            @JsonProperty("datetime") LocalDateTime dateTime,
            @JsonProperty("series-num") Number seriesNum,
            @JsonProperty("bodypart") String bodyPart,
            @JsonProperty("description") String description,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 이미지 목록 응답을 위한 DTO입니다.
     *
     * @param imageKey 이미지의 고유 식별자
     * @param imageIndex 이미지의 인덱스
     * @param datetime 이미지 날짜 및 시간
     * @param path 뷰어에 이미지를 로드하기 위한 파일 경로
     * @param hidden 이미지가 숨김으로 표시되었는지 여부
     */
    public record ImageDto(
            @JsonProperty("image-key") Long imageKey,
            @JsonProperty("image-index") Number imageIndex,
            @JsonProperty("datetime") LocalDateTime datetime,
            @JsonProperty("path") String path,
            @JsonProperty("hidden") boolean hidden
    ) {}

    /**
     * 다중 파일 업로드 결과를 요약하기 위한 DTO입니다.
     * 프론트엔드에 성공한 파일과 실패한 파일을 알리는 데 사용됩니다.
     *
     * @param succeededFiles 성공적으로 업로드된 파일 이름 목록
     * @param failedFiles 업로드에 실패한 파일 이름 목록
     */
    public record UploadResultDto(
            @JsonProperty("succeeded-files") List<String> succeededFiles,
            @JsonProperty("failed-files") List<String> failedFiles
    ) {}

    /**
     * 프레임 수를 포함한 인스턴스 정보를 제공하기 위한 DTO입니다.
     * 프론트엔드가 다중 프레임(cine) 인스턴스를 여러 이미지로 펼칠 수 있도록 합니다.
     *
     * @param instanceId 인스턴스의 식별자
     * @param numberOfFrames 프레임 수 (일반 이미지는 1, cine 루프는 2 이상)
     */
    public record InstanceInfoDto(
            @JsonProperty("instance-id") String instanceId,
            @JsonProperty("number-of-frames") int numberOfFrames
    ) {}
}