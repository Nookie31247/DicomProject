package com.allegro.dicomback.service;

import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.entity.User;
import com.allegro.dicomback.entity.User;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.PatientRepository;
import com.allegro.dicomback.repository.SeriesRepository;
import com.allegro.dicomback.repository.StudyRepository;
import com.allegro.dicomback.repository.UserRepository;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.allegro.dicomback.dto.DicomResponseDto.*;
import com.allegro.dicomback.dto.DicomRequestDto.*;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    // 레포지토리 의존성 주입하기
    private final SeriesRepository seriesRepository;
    private final StudyRepository studyRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${orthanc.url:http://localhost:8042}")
    private String orthancUrl;

    // ======================================== 이영무 추가 ======================================================

    public List<PatientDto> getPatients(Long doctorKey, String start, String end, String search) {
        List<Patient> patientList;
        List<PatientDto> patientDtoList = new ArrayList<>();
        LocalDateTime startDay;
        LocalDateTime endDay;

        // 시작일, 종료일이 입력되지 않았을 때
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            // 모든 검색 결과를 다 전송하면 랙걸리니까, 기본값은 최근 3개월로 제한
            startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
            endDay = LocalDateTime.now();
        }
        // 시작일과 종료일이 모두 입력되었을 때
        else {
            try {
                startDay = LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN);
                endDay = LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX);
            } catch (DateTimeParseException e) {
                startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
                endDay = LocalDateTime.now();
                System.out.println("날짜 형식이 안맞아요");
            }
        }

        patientList = patientRepository.findByDoctorKeyWithOptionalRecentStudy(
                doctorKey,
                search,
                startDay,
                endDay
        );

//        // 검색어가 있을 때
//        if(StringUtils.hasText(search)) {
//            // 여기서는 시작일, 종료일, 검색어 3가지 모두를 가지고 검색한다.
//            patientList = patientRepository.findByDoctorKey_KeyAndNameContainingAndRecentStudyBetween(doctorKey, search, startDay, endDay);
//        }
//        // 검색어가 없을 때
//        else {
//            // 여기서는 시작일과 종료일만 가지고 검색한다.
//            patientList = patientRepository.findByDoctorKey_KeyAndRecentStudyBetween(doctorKey, startDay, endDay);
//        }

        for(Patient p : patientList) {
            patientDtoList.add(new PatientDto(
                    p.getKey(),
                    p.getName(),
                    p.getBirth(),
                    p.getSex(),
                    p.getRecentStudy(),
                    p.getStudyCount(),
                    p.getHiddenFlag()
            ));
        }

        return patientDtoList;
    }

    public List<StudyDto> getStudies(Long doctorKey, Long patientKey, String start, String end, String search) {
        List<Study> studyList;
        LocalDateTime startDay;
        LocalDateTime endDay;

        // 시작 날짜(start)와 종료 날짜(end)가 입력되었을 때에는 검색 범위를 입력된 값으로 하고
        // 입력되지 않았을 때에는 기본값인 최근으로부터 3개월치를 검색합니다. (임시로 10년까지)
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(120);
            endDay = LocalDateTime.now();
        }
        else {
            try {
                startDay = LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN);
                endDay = LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX);
            } catch (DateTimeParseException e) {
                // 날짜 형식이 안맞으면 강제로 기본값으로 변경
                startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(3);
                endDay = LocalDateTime.now();
                System.out.println("날짜 형식이 안맞아요");
            }
        }

        // 검색어가 있으면 검색어를 포함하여 검색하는 쿼리를 날리고
        if (StringUtils.hasText(search)) {
            studyList = studyRepository.findStudiesWithSearch(
                    doctorKey,
                    patientKey,
                    startDay,
                    endDay,
                    search
            );
        }
        // 검색어가 없으면 검색어를 포함하지 않는 쿼리를 날린다.
        else {
            studyList = studyRepository.findStudiesWithoutSearch(
                    doctorKey,
                    patientKey,
                    startDay,
                    endDay
            );
        }

        if (studyList.isEmpty()) {
            return List.of();
        }

        // 시리즈와 이미지의 개수를 구하기 위해 스터디 키를 가져온다.
        List<Long> studyKeys = studyList.stream()
                .map(Study::getKey)
                .toList();

        // SeriesAndImagesCount는 스터디 키, 시리즈 개수, 이미지 개수로 이루어진 record 값인데
        // 여기서 하나하나 for문 돌리면 연산이 많으니까 스터디 키를 Key로 하는 맵 타입을 만든다.
        Map<Long, SeriesRepository.SeriesAndImagesCount> countMap =
                seriesRepository.getSeriesAndImagesCount(studyKeys)
                        .stream()
                        .collect(Collectors.toMap(
                                SeriesRepository.SeriesAndImagesCount::getStudyKey,
                                Function.identity()
                        ));

        return studyList.stream()
                .map(study -> {
                    SeriesRepository.SeriesAndImagesCount count = countMap.get(study.getKey());

                    Long seriesNum = count == null ? 0L : count.getSeriesNum();
                    Long imagesNum = count == null ? 0L : count.getImagesNum();

                    return new StudyDto(
                            study.getKey(),
                            study.getDescription(),
                            study.getCreatedAt(),
                            seriesNum,
                            imagesNum,
                            study.getAllowResearch(),
                            study.getHiddenFlag()
                    );
                })
                .toList();
    }

    public List<SeriesDto> getSeries(Long doctorKey, Long studyKey) {
        List<Series> seriesList = seriesRepository.getSeries(doctorKey, studyKey);
        List<SeriesDto> seriesDtoList = new ArrayList<>();
        seriesList.forEach(s -> seriesDtoList.add(new SeriesDto(
                        s.getKey(),
                        s.getSeriesNum(),
                        s.getCreatedAt(),
                        s.getSeriesNum(),
                        s.getBodyPart(),
                        s.getHiddenFlag()
                ))
        );

        return seriesDtoList;

    }

    public void setAnonymization(Long doctorKey, Long studyKey) {
        // 익명화 DB 만들면 여기에 관련 로직 추가할 것
    }

    // 정보를 수정하는 메서드에는 @Transactional(readOnly = true)를 사용할 수 없다.
    @Transactional
    public void setHidePatients(Long doctorKey, List<PatientHideDto> requests) {
        List<Long> hiddenPatients = new ArrayList<>();
        List<Long> showPatients = new ArrayList<>();
        requests.forEach(r -> {
            if(r.hidden())
                hiddenPatients.add(r.patientKey());
            else
                showPatients.add(r.patientKey());
        });

        if(!hiddenPatients.isEmpty())
            patientRepository.changeHiddenFlag(doctorKey, hiddenPatients, true);
        if(!showPatients.isEmpty())
            patientRepository.changeHiddenFlag(doctorKey, showPatients, false);
    }

    @Transactional
    public void setHideStudies(Long doctorKey, List<StudyHideDto> requests) {
        List<Long> hiddenStudies = new ArrayList<>();
        List<Long> showStudies = new ArrayList<>();
        requests.forEach(r -> {
            if(r.hidden())
                hiddenStudies.add(r.studyKey());
            else
                showStudies.add(r.studyKey());
        });

        if(!hiddenStudies.isEmpty())
            studyRepository.changeHiddenFlag(doctorKey, hiddenStudies, true);
        if(!showStudies.isEmpty())
            studyRepository.changeHiddenFlag(doctorKey, showStudies, false);
    }

    @Transactional
    public void setHideSeries(Long doctorKey, List<SeriesHideDto> requests) {
        List<Long> hiddenSeries = new ArrayList<>();
        List<Long> showSeries = new ArrayList<>();
        requests.forEach(r -> {
            if(r.hidden())
                hiddenSeries.add(r.seriesKey());
            else
                showSeries.add(r.seriesKey());
        });

        if(!hiddenSeries.isEmpty())
            seriesRepository.changeHiddenFlag(doctorKey, hiddenSeries, true);
        if(!showSeries.isEmpty())
            seriesRepository.changeHiddenFlag(doctorKey, showSeries, false);
    }

    // 시리즈 전체 ZIP 다운로드 (Orthanc /archive)
    //GET http://localhost:8080/api/dicom/series/download?series-key=1
    public StreamingResponseBody downloadSeriesAsZip(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        // orthancSeriesId가 null이면 동기화가 안 된 상태이므로 미리 차단
        if (series.getOrthancId() == null) {
            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        String url = orthancUrl + "/series/" + series.getOrthancId() + "/archive";

        //스트리밍 프록시
        return outputStream -> {
            restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                return null;
            });
        };
    }

    // 스터디 전체 ZIP 다운로드 (Orthanc /archiv)
    //GET http://localhost:8080/api/dicom/studies/download?study-key=1
    public StreamingResponseBody downloadStudyAsZip(Long studyKey) {
        Study study = studyRepository.findById(studyKey)
                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));

        // orthancStudyId가 null이면 동기화가 안 된 상태이므로 미리 차단
        if (study.getOrthancId() == null) {
            log.warn("orthancStudyId가 없습니다. studyKey: {} (동기화 필요)", studyKey);
            throw new BaseException(ErrorCode.STUDY_NOT_SYNCED);
        }

        String url = orthancUrl + "/studies/" + study.getOrthancId() + "/archive";

        //스트리밍 프록시
        return outputStream -> {
            restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
                StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                return null;
            });
        };
    }

    // 연구 활용이 허용된 스터디 목록을 이 의사(doctorKey) 기준으로 조회해서
    // 화면에 필요한 시리즈 수를 채우는 메서드 (/research 페이지가 최초 로딩될 때 호출하는 API의 실제 로직)
    public List<StudyDto> getResearchStudies() {

        // 의사가 담당하는 환자들 중, 연구 허용 + 숨김 아님 조건에 맞는 Study 엔티티들을 DB에서 가져옴
        // (StudyRepository.findResearchStudies의 JPQL 쿼리가 실제 필터링을 담당)
        List<Study> studyList = studyRepository.findResearchStudies();

        // 연구 허용된 스터디가 하나도 없으면 뒤 로직 돌릴 필요 없이 바로 빈 리스트 반환
        if (studyList.isEmpty()) return List.of();

        // 방금 가져온 스터디들의 key(PK)만을 뽑아서 리스트로 만든다.,
        List<Long> studyKeys = studyList.stream().map(Study::getKey).toList();

        // studyKeys에 해당하는 모든 스터디의 "시리즈 개수 + 영상 개수" 집계 결과를 한 번의 쿼리로 가져와서
        // studyKey → 집계결과 로 바로 찾아볼 수 있는 Map으로 변환
        // 스터디마다 따로따로 쿼리 날리면 N+1 문제가 생기니, 한 번에 다 가져와서 메모리에서 매칭하는 방식
        Map<Long, SeriesRepository.SeriesAndImagesCount> countMap =
                seriesRepository.getSeriesAndImagesCount(studyKeys)
                        .stream()
                        .collect(Collectors.toMap(
                                SeriesRepository.SeriesAndImagesCount::getStudyKey, // Map의 key: studyKey
                                Function.identity()                                 // Map의 value: 집계 결과 객체 그대로
                        ));

        // 스터디 목록을 하나씩 돌면서 화면(StudyDto)에 필요한 형태로 변환
        return studyList.stream()
                .map(study -> {
                    // 이 스터디의 집계 결과를 Map에서 꺼내노다.
                    SeriesRepository.SeriesAndImagesCount count = countMap.get(study.getKey());

                    // 예외처리: count가 없는 시리즈가 없는 스터디면 0으로 처리, 있으면 실제 집계값 사용
                    Long seriesNum = count == null ? 0L : count.getSeriesNum();
                    Long imagesNum = count == null ? 0L : count.getImagesNum();

                    return new StudyDto(
                            study.getKey(),
                            study.getDescription(),
                            study.getCreatedAt(),
                            seriesNum,
                            imagesNum,
                            study.getAllowResearch(),
                            study.getHiddenFlag()
                    );
                })
                .toList();
    }
    @Transactional
    public void addPatient(Long doctorKey, PatientRequestDto request) {

        User doctor = userRepository.findById(doctorKey)
                .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Patient patient = Patient.builder()
                .doctorKey(doctor)
                .name(request.name())
                .sex(request.sex())
                .birth(request.birth())
                .studyCount(0)
                .hiddenFlag(false)
                .build();

        patientRepository.save(patient);
    }

    @Transactional
    public void processDicomFile(Long patientKey, MultipartFile file) throws IOException {
        // Orthanc 업로드용으로 바이트를 한 번만 읽어서 재사용한다.
        // (file.getInputStream()을 두 번 열어도 되긴 하지만, 바이트 배열로 고정해두는 게 더 안전하다)
        byte[] fileBytes = file.getBytes();

        Attributes attrs;
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(fileBytes))) {
            attrs = dis.readDataset(-1, -1);
        }

        String studyInstanceUid = attrs.getString(Tag.StudyInstanceUID);
        String studyDate = attrs.getString(Tag.StudyDate);
        String studyTime = attrs.getString(Tag.StudyTime);
        String studyDescription = attrs.getString(Tag.StudyDescription);

        String seriesInstanceUid = attrs.getString(Tag.SeriesInstanceUID);
        String seriesDate = attrs.getString(Tag.SeriesDate);
        String seriesTime = attrs.getString(Tag.SeriesTime);
        String modality = attrs.getString(Tag.Modality);
        String bodyPart = attrs.getString(Tag.BodyPartExamined);
        Integer seriesNum = parseIntTag(attrs.getString(Tag.SeriesNumber));

        //NumberOfFrames(0028,0008): 초음파/혈관조영처럼 인스턴스 1개 안에 여러 프레임이 들어있는 경우(얘네는 이미지 하나가 사실상 시리즈 하나 판정임).
        //태그가 없으면(대부분의 CT/MR/X-ray) 단일 프레임이므로 1로 취급.
        String numberOfFramesStr = attrs.getString(Tag.NumberOfFrames);
        int numberOfFrames = (numberOfFramesStr != null && !numberOfFramesStr.isBlank())
                ? Integer.parseInt(numberOfFramesStr.trim())
                : 1;

        log.info("--- DICOM 태그 추출 성공 ---");
        log.info("Study UID: {}, Series UID: {}", studyInstanceUid, seriesInstanceUid);
        log.info("Description: {}", studyDescription);

        // 1단계: PACS(Orthanc)에 원본 파일을 그대로 업로드한다.
        // Orthanc가 파일 안의 태그를 직접 읽어서 Patient/Study/Series/Instance 계층으로 알아서 정렬해주기 때문에
        // 별도로 구조를 맞춰서 보낼 필요가 없다. 이미 올라간 인스턴스를 다시 올려도 에러가 나지 않고
        // Status: "AlreadyStored"로 응답하므로, 같은 파일을 몇 번을 올려도 안전하다(idempotent).
        OrthancInstanceResponse orthancResponse = uploadToOrthanc(fileBytes);
        log.info("Orthanc 업로드 결과: status={}, studyId={}, seriesId={}",
                orthancResponse.status(), orthancResponse.parentStudy(), orthancResponse.parentSeries());

        // 2단계: Study Instance UID로 기존 Study를 찾는다.
        // - 없으면 새로 만들고, PACS가 방금 알려준 studyId를 orthanc_id에 바로 넣는다.
        // - 이미 있으면(같은 Study의 다른 인스턴스/시리즈를 올린 경우) 새로 만들지 않고,
        //   orthanc_id가 비어있을 때만 채워준다.
        Study study = studyRepository.findByUid(studyInstanceUid).orElse(null);
        if (study == null) {
            Patient patient = patientRepository.findById(patientKey)
                    .orElseThrow(() -> new BaseException(ErrorCode.PATIENT_NOT_FOUND));

            study = Study.builder()
                    .uid(studyInstanceUid)
                    .patientKey(patient)
                    .createdAt(parseDicomDateTime(studyDate, studyTime))
                    .description(studyDescription)
                    .allowResearch(false)
                    .hiddenFlag(false)
                    .orthancId(orthancResponse.parentStudy())
                    .build();
            study = studyRepository.save(study);
            log.info("새 Study 저장 완료. UID: {}", studyInstanceUid);

            // 환자의 검사 횟수(study_count) +1
            patient.setStudyCount((patient.getStudyCount() == null ? 0 : patient.getStudyCount()) + 1);

            // 환자의 최근 검사일(recent_study)을 이번에 새로 생긴 Study의 날짜로 갱신
            // (기존 Study는 날짜가 바뀌지 않으므로, 새 Study가 생길 때만 비교/갱신하면 충분하다)
            if (patient.getRecentStudy() == null || study.getCreatedAt().isAfter(patient.getRecentStudy())) {
                patient.setRecentStudy(study.getCreatedAt());
            }

            patientRepository.save(patient);
        } else if (study.getOrthancId() == null && orthancResponse.parentStudy() != null) {
            study.setOrthancId(orthancResponse.parentStudy());
            studyRepository.save(study);
            log.info("기존 Study의 orthanc_id를 채워넣었습니다. UID: {}", studyInstanceUid);
        } else {
            log.info("이미 등록된 Study입니다. UID: {}", studyInstanceUid);
        }

        // 3단계: Series Instance UID도 동일한 방식으로 처리한다.
        // 이미지 수(total_images_conut)는 Orthanc 응답의 status를 기준으로 판단한다.
        // - "AlreadyStored": 완전히 같은 인스턴스 파일을 재업로드한 것 -> 카운트 올리지 않음
        // - 그 외("Success" 등): 이번에 실제로 새로 저장된 인스턴스 -> 카운트 +1
        if (StringUtils.hasText(seriesInstanceUid)) {
            boolean isNewInstance = !"AlreadyStored".equalsIgnoreCase(orthancResponse.status());

            Series series = seriesRepository.findByUid(seriesInstanceUid).orElse(null);
            if (series == null) {
                series = Series.builder()
                        .uid(seriesInstanceUid)
                        .studyKey(study)
                        .seriesNum(seriesNum)
                        .bodyPart(bodyPart)
                        .modality(modality)
                        .createdAt(parseDicomDateTime(
                                StringUtils.hasText(seriesDate) ? seriesDate : studyDate,
                                StringUtils.hasText(seriesTime) ? seriesTime : studyTime))
                        .orthancId(orthancResponse.parentSeries())
                        .hiddenFlag(false)
                        // 우리 DB엔 이번이 이 시리즈의 첫 등록이므로, Orthanc의 이전 저장 여부와 무관하게 프레임 수(numberOfFrames)로 시작
                        // (일반 이미지는 numberOfFrames=1이라 기존과 동일하게 1로 시작됨)
                        .totalImagesCount(numberOfFrames)
                        .build();
                seriesRepository.save(series);
                log.info("새 Series 저장 완료. UID: {}", seriesInstanceUid);
            } else {
                boolean changed = false;

                if (series.getOrthancId() == null && orthancResponse.parentSeries() != null) {
                    series.setOrthancId(orthancResponse.parentSeries());
                    changed = true;
                }
                // 기존 시리즈에 인스턴스 추가 시 — 인스턴스 1개당 +1이 아니라, 그 인스턴스가 가진 프레임 수(+numberOfFrames)만큼 더한다.
                if (isNewInstance) {
                    int current = series.getTotalImagesCount() == null ? 0 : series.getTotalImagesCount();
                    series.setTotalImagesCount(current + numberOfFrames);
                    changed = true;
                }
                if (changed) {
                    seriesRepository.save(series);
                    log.info("기존 Series 갱신 완료(orthanc_id/이미지 수). UID: {}", seriesInstanceUid);
                } else {
                    log.info("이미 등록된 Series 인스턴스입니다. UID: {}", seriesInstanceUid);
                }
            }
        }
    }

    // Orthanc REST API로 DICOM 파일 원본을 그대로 POST 업로드한다.
    // 응답에 이 인스턴스가 속한 Study/Series의 Orthanc 내부 ID(ParentStudy/ParentSeries)가 들어있다.
    private OrthancInstanceResponse uploadToOrthanc(byte[] fileBytes) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("application/dicom"));
        HttpEntity<byte[]> requestEntity = new HttpEntity<>(fileBytes, headers);

        OrthancInstanceResponse response = restTemplate.postForObject(
                orthancUrl + "/instances",
                requestEntity,
                OrthancInstanceResponse.class
        );

        if (response == null) {
            throw new BaseException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
        return response;
    }

    // Orthanc의 POST /instances 응답 중 우리가 필요한 필드만 뽑아서 받는 record.
    // 예) {"ID": "...", "ParentStudy": "...", "ParentSeries": "...", "Status": "Success"}
    private record OrthancInstanceResponse(
            @JsonProperty("ID") String id,
            @JsonProperty("ParentStudy") String parentStudy,
            @JsonProperty("ParentSeries") String parentSeries,
            @JsonProperty("Status") String status
    ) {}

    // "7", "007" 같은 DICOM 숫자 태그 문자열을 정수로 안전하게 변환한다. 값이 없거나 이상하면 0.
    private Integer parseIntTag(String value) {
        if (!StringUtils.hasText(value)) {
            return 0;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return 0;
        }
    }

    // DICOM Date(yyyyMMdd) + Time(HHmmss.ffffff)을 LocalDateTime으로 합쳐준다.
    // Study/Series 양쪽에서 재사용. 날짜/시간이 없거나 형식이 이상하면 최대한 방어적으로 처리한다.
    private LocalDateTime parseDicomDateTime(String date, String time) {
        if (!StringUtils.hasText(date)) {
            log.warn("날짜 태그가 없어 현재 시각으로 대체합니다.");
            return LocalDateTime.now();
        }

        LocalDate localDate;
        try {
            localDate = LocalDate.parse(date.trim(), DateTimeFormatter.BASIC_ISO_DATE);
        } catch (DateTimeParseException e) {
            log.warn("날짜 형식이 올바르지 않습니다: {}", date);
            return LocalDateTime.now();
        }

        LocalTime localTime = LocalTime.MIDNIGHT;
        if (StringUtils.hasText(time)) {
            // 소수점 이하(초 미만) 제거 후 HHmmss 6자리로 보정 (HH, HHmm만 오는 경우도 방어)
            String digitsOnly = time.trim().split("\\.")[0];
            String padded = (digitsOnly + "000000").substring(0, 6);
            try {
                localTime = LocalTime.parse(padded, DateTimeFormatter.ofPattern("HHmmss"));
            } catch (DateTimeParseException e) {
                log.warn("시간 형식이 올바르지 않아 00:00:00으로 대체합니다: {}", time);
            }
        }

        return LocalDateTime.of(localDate, localTime);
    }

    // 픽셀 데이터가 없어 이미지 뷰어로 열 수 없는 modality (Presentation State, Structured Report, Key Object 등)
    //  DB에는 그대로 보관하되, 이미지 뷰어용 시리즈 목록에서는 제외한다.
    //  PR (Presentation State): 영상의 밝기, 대조도, 확대, 회전, 주석 등 화면에 보이는 출력 상태를 원본 파괴 없이 저장하는 기능
    //  SR (Structured Report): 영상 진단 결과, 측정 수치, 의사의 소견 등을 기계가 읽을 수 있는 표준화된 문서 형식으로 기록하는 기능
    //  KO (Key Object Selection): 수많은 영상 중 진단이나 연구에 중요한 핵심 영상만을 골라내어 따로 표시하고 관리하는 기능
    //  DOC (Document): 환자의 진료 기록, 소견서, 외부 보고서 등의 일반 문서 데이터를 DICOM 규격 내에 캡슐화하여 저장하는 기능
    //  AU (Audio): 수술 중 녹음된 음성 소견이나 심장 초음파의 도플러 혈류 소리 등 의료 영상과 관련된 오디오 데이터를 저장하는 기능
    //  REG (Registration): CT와 MRI 등 서로 다른 시점이나 다른 장비로 촬영한 영상을 정밀하게 겹쳐서 비교할 수 있도록 정렬하는 기능
    //  SEG (Segmentation): 의료 영상 인공지능(AI)이나 분석 소프트웨어가 장기, 종양 등 특정 관심 부위의 경계를 지정하고 분할하여 표시하는 기능
    private static final Set<String> NON_IMAGE_MODALITIES = Set.of("PR");

//    // 프론트엔드 뷰어를 위한 시리즈 내 인스턴스(단면) ID 목록 가져오기
//    // InstanceNumber 기준으로 정렬해서 반환
//    @SuppressWarnings("unchecked")
//    public List<String> getInstanceIdsBySeries(Long seriesKey) {
//        Series series = seriesRepository.findById(seriesKey)
//                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND)); // STUDY_NOT_FOUND -> SERIES_NOT_FOUND
//
//        if (series.getOrthancSeriesId() == null) {
//            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
//            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
//        }
//
//        // expand=true로 instance별 MainDicomTags(InstanceNumber 포함)까지 한 번에 조회
//        String url = orthancUrl + "/series/" + series.getOrthancSeriesId() + "/instances?expand=true";
//        List<Map<String, Object>> instances = restTemplate.getForObject(url, List.class);
//
//        if (instances == null || instances.isEmpty()) {
//            return java.util.Collections.emptyList();
//        }
//
//        return instances.stream()
//                .sorted(Comparator.comparingInt(this::extractInstanceNumber))
//                .map(inst -> (String) inst.get("ID"))
//                .collect(Collectors.toList());
//    }
//
//    @SuppressWarnings("unchecked")
//    private int extractInstanceNumber(Map<String, Object> instance) {
//        try {
//            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
//            String num = (String) tags.get("InstanceNumber");
//            return num != null ? Integer.parseInt(num.trim()) : Integer.MAX_VALUE;
//        } catch (Exception e) {
//            return Integer.MAX_VALUE; // 파싱 실패 시 맨 뒤로 밀어서 순서를 깨뜨리지 않음
//        }
//    }
//
//    public DicomResponseDto.StudyDto getStudyDetail(Long studyKey) {
//        Study study = studyRepository.findById(studyKey)
//                .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));
//
//        Patient patient = study.getPatient();
//
//        return new DicomResponseDto.StudyDto(
//                study.getStudyKey(),
//                study.getDescription(),
//                study.getStudyDateTime(),
//                study.getTotalSeriesCount(),
//                study.getTotalInstanceCount(),
//                study.getAllowedResearch() != null && study.getAllowedResearch() == 1,
//                study.getDelFlag() != null && study.getDelFlag() == 1,
//                patient.getName(),
//                patient.getBirth()
//        );
//    }
//
//    // Study에 속한 Series 목록
//    public List<DicomResponseDto.SeriesDto> getSeriesByStudy(Long studyKey) {
//        if (!studyRepository.existsById(studyKey)) {
//            throw new BaseException(ErrorCode.STUDY_NOT_FOUND);
//        }
//
//        List<Series> seriesEntities = seriesRepository.findByStudy_StudyKeyAndDelFlag(studyKey, 0);
//
//        return seriesEntities.stream()
//                // PR/SR/KO 등 픽셀 데이터 없는 시리즈는 이미지 뷰어 목록에서 제외 (DB에는 그대로 남아있음)
//                .filter(s -> s.getModality() == null || !NON_IMAGE_MODALITIES.contains(s.getModality().toUpperCase()))
//                .map(s -> new DicomResponseDto.SeriesDto(
//                        s.getSeriesKey(),
//                        s.getSeriesNum(),
//                        null,
//                        s.getSeriesNum(),
//                        s.getBodyPart(),
//                        s.getTotalInstanceCount(),
//                        s.getSeriesDescription(),
//                        s.getDelFlag() != null && s.getDelFlag() == 1
//                ))
//                .collect(Collectors.toList());
//    }
//
//    // 인스턴스 하나의 raw DICOM 바이너리 프록시
//    // seriesKey를 같이 받아서, 해당 시리즈에 실제로 속한 인스턴스인지 검증 (임의 UUID 요청 방지)
//    public StreamingResponseBody getInstanceFile(Long seriesKey, String instanceId) {
//        Series series = seriesRepository.findById(seriesKey)
//                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));
//
//        if (series.getOrthancSeriesId() == null) {
//            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
//        }
//
//        List<String> validInstanceIds = getInstanceIdsBySeries(seriesKey);
//        if (!validInstanceIds.contains(instanceId)) {
//            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
//        }
//
//        String url = orthancUrl + "/instances/" + instanceId + "/file";
//
//        return outputStream -> restTemplate.execute(url, HttpMethod.GET, null, clientHttpResponse -> {
//            StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
//            return null;
//        });
//    }
}