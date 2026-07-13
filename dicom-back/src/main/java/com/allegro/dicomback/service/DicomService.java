package com.allegro.dicomback.service;

import com.allegro.dicomback.dto.DicomRequestDto.*;
import com.allegro.dicomback.dto.DicomResponseDto.PatientDto;
import com.allegro.dicomback.dto.DicomResponseDto.SeriesDto;
import com.allegro.dicomback.dto.DicomResponseDto.StudyDto;
import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.entity.User;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.PatientRepository;
import com.allegro.dicomback.repository.SeriesRepository;
import com.allegro.dicomback.repository.StudyRepository;
import com.allegro.dicomback.repository.UserRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * DICOM 관련 비즈니스 로직을 처리하는 서비스입니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    private final SeriesRepository seriesRepository;
    private final StudyRepository studyRepository;
    private final PatientRepository patientRepository;
    private final UserRepository userRepository;
    private final AnonymizationService anonymizationService;

    private final RestTemplate restTemplate = new RestTemplate();
    @Value("${orthanc.url:http://localhost:8042}")
    private String orthancUrl;

    /**
     * 주어진 매개변수로 필터링된 환자 목록을 검색합니다.
     *
     * @param doctorKey 의사 키
     * @param start 시작 날짜 (yyyy-MM-dd)
     * @param end 종료 날짜 (yyyy-MM-dd)
     * @param search 검색어
     * @return PatientDto 목록
     */
    public List<PatientDto> getPatients(Long doctorKey, String start, String end, String search) {
        List<Patient> patientList;
        List<PatientDto> patientDtoList = new ArrayList<>();
        LocalDateTime startDay;
        LocalDateTime endDay;

        // 시작일, 종료일이 입력되지 않았을 때
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            // 모든 검색 결과를 다 전송하면 랙걸리니까, 기본값은 최근 1년으로 제한
            startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusYears(1);
            endDay = LocalDateTime.now();
        }
        // 시작일과 종료일이 모두 입력되었을 때
        else {
            try {
                startDay = LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN);
                endDay = LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX);
            } catch (DateTimeParseException e) {
                startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusYears(1);
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

    /**
     * 주어진 매개변수로 필터링된 검사(study) 목록을 검색합니다.
     *
     * @param doctorKey 의사 키
     * @param patientKey 환자 키
     * @param start 시작 날짜
     * @param end 종료 날짜
     * @param search 검색어
     * @return StudyDto 목록
     */
    public List<StudyDto> getStudies(Long doctorKey, Long patientKey, String start, String end, String search) {
        List<Study> studyList;
        LocalDateTime startDay;
        LocalDateTime endDay;

        // 시작 날짜(start)와 종료 날짜(end)가 입력되었을 때에는 검색 범위를 입력된 값으로 하고
        // 입력되지 않았을 때에는 기본값인 최근으로부터 3개월치를 검색합니다. (임시로 150년까지)
        if (!StringUtils.hasText(start) || !StringUtils.hasText(end)) {
            startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(1800);
            endDay = LocalDateTime.now();
        }
        else {
            try {
                startDay = LocalDateTime.of(LocalDate.parse(start), LocalTime.MIN);
                endDay = LocalDateTime.of(LocalDate.parse(end), LocalTime.MAX);
            } catch (DateTimeParseException e) {
                // 날짜 형식이 안맞으면 강제로 기본값으로 변경
                startDay = LocalDateTime.of(LocalDate.now(), LocalTime.MIN).minusMonths(1800);
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

    /**
     * 주어진 매개변수로 필터링된 시리즈 목록을 검색합니다.
     *
     * @param doctorKey 의사 키
     * @param studyKey 검사(study) 키
     * @return SeriesDto 목록
     */
    public List<SeriesDto> getSeries(Long doctorKey, Long studyKey) {
        List<Series> seriesList = seriesRepository.getSeries(doctorKey, studyKey);
        List<SeriesDto> seriesDtoList = new ArrayList<>();
        seriesList.forEach(s -> seriesDtoList.add(new SeriesDto(
                        s.getKey(),
                        s.getSeriesNum(),
                        s.getCreatedAt(),
                        s.getSeriesNum(),
                        s.getBodyPart(),
                        s.getSeriesDescription(),
                        s.getHiddenFlag()
                ))
        );

        return seriesDtoList;
    }

    /**
     * 지정된 환자에 대한 숨김 플래그를 설정합니다.
     * 정보를 수정하는 메서드에는 @Transactional(readOnly = true)를 사용할 수 없다.
     *
     * @param doctorKey 의사 키
     * @param requests 숨김 설정을 포함하는 PatientHideDto 목록
     */
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

    /**
     * 지정된 검사(study)에 대한 숨김 플래그를 설정합니다.
     *
     * @param doctorKey 의사 키
     * @param requests 숨김 설정을 포함하는 StudyHideDto 목록
     */
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

    /**
     * 지정된 시리즈에 대한 숨김 플래그를 설정합니다.
     *
     * @param doctorKey 의사 키
     * @param requests 숨김 설정을 포함하는 SeriesHideDto 목록
     */
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

    /**
     * 지정된 검사(study)에 대한 연구 허용 플래그를 설정합니다.
     *
     * @param doctorKey 의사 키
     * @param requests 연구 설정을 포함하는 StudyResearchDto 목록
     */
    @Transactional
    public void setAllowResearchStudies(Long doctorKey, List<StudyResearchDto> requests) {
        List<Long> allowedStudies = new ArrayList<>();
        List<Long> disallowedStudies = new ArrayList<>();
        requests.forEach(r -> {
            if(r.allowResearch())
                allowedStudies.add(r.studyKey());
            else
                disallowedStudies.add(r.studyKey());
        });

        if(!allowedStudies.isEmpty())
            studyRepository.changeAllowResearch(doctorKey, allowedStudies, true);
        if(!disallowedStudies.isEmpty())
            studyRepository.changeAllowResearch(doctorKey, disallowedStudies, false);

        List<Study> allowedStudyEntities = studyRepository.findWithPatientByKeys(allowedStudies);

        Map<String, Integer> orthancUidToDateOffsetDays = new HashMap<>();
        for (Study study : allowedStudyEntities) {
            if (study.getOrthancId() == null) {
                continue;
            }
            Long patientKey = study.getPatientKey().getKey();
            orthancUidToDateOffsetDays.put(study.getOrthancId(), computeDateOffsetDays(patientKey));
        }

        anonymizationService.anonymize(orthancUidToDateOffsetDays);
    }

    private static final long DATE_OFFSET_SALT = 913_047_211L; // 임의로 고른 고정값(의미 없는 매직넘버)
    private static final int MIN_SHIFT_DAYS = 365;      // 1년 (하한)
    private static final int MAX_SHIFT_DAYS = 365 * 3;  // 3년 (상한)

    /**
     * 환자 키로부터 환자마다 다르지만, 같은 환자는 항상 같은 날짜 시프트 오프셋(일수)을 계산합니다.
     * 그래서 절대 날짜는 환자별 오프셋만큼 day shift를 사용하여 일정 수치만큼 밀어내고, 간격은 그대로 보존하는 방식을 쓴다.
     *
     * @param patientKey 환자 키 (patients 테이블 PK)
     * @return -1095 ~ -365일 또는 +365 ~ +1095일 범위의 날짜 오프셋(일수).
     *         같은 patientKey를 넣으면 항상 같은 값이 나온다.
     */
    private int computeDateOffsetDays(Long patientKey) {
        long seed = (patientKey == null ? 0L : patientKey) ^ DATE_OFFSET_SALT;
        // java.util.Random은 같은 seed를 주면 항상 같은 순서의 난수를 뽑으므로, 같은 환자는 항상 같은 오프셋을 받는다.
        java.util.Random random = new java.util.Random(seed);

        // 크기를 1~3년 안으로 범위에서 뽑는다.
        int magnitude = MIN_SHIFT_DAYS + random.nextInt(MAX_SHIFT_DAYS - MIN_SHIFT_DAYS + 1);

        // 얼마나 밀지는 랜덤으로 정한다 해당 기능으로 검사일 특정을 방지한다.
        boolean shiftIntoPast = random.nextBoolean();

        return shiftIntoPast ? -magnitude : magnitude;
    }

    /**
     * 전체 시리즈를 ZIP 파일로 다운로드합니다.
     * GET http://localhost:8080/api/medical/dicom/series/download?series-key=1
     *
     * @param seriesKey 시리즈 키
     * @return ZIP 파일을 포함하는 스트리밍 응답 본문
     */
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

    /**
     * 전체 검사(study)를 ZIP 파일로 다운로드합니다.
     * GET http://localhost:8080/api/medical/dicom/studies/download?study-key=1
     *
     * @param studyKey 검사(study) 키
     * @return ZIP 파일을 포함하는 스트리밍 응답 본문
     */
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

    /**
     * 여러 검사(study)와 시리즈를 단일 ZIP 파일로 다운로드합니다.
     * 연구 자료 다운로드 페이지에서 체크된 study/series 여러 개를 zip 하나로 묶어서 받는다
     * 원래는 각각 따로 호출해서 <a> 태그를 여러 번 클릭시키는 방식이었는데 이렇게 자동 다운로드를 연달아 여러 번 트리거하면 브라우저에서 막히는 경우 발생함
     * Orthanc의 /tools/create-archive는 여러 리소스(Study/Series) ID를 한 번에 넘기면 그걸 다 합친 zip 하나를 만들어줌
     * 해당 기능으로 하나의 zip으로 묶어서 1회의 요청으로 처리함
     *
     * @param request 일괄 다운로드 요청 DTO
     * @return ZIP 파일을 포함하는 스트리밍 응답 본문
     */
    public StreamingResponseBody downloadBatchAsZip(BatchDownloadDto request) {
        List<String> orthancIds = new ArrayList<>();

        if (request.studyKeys() != null) {
            for (Long studyKey : request.studyKeys()) {
                Study study = studyRepository.findById(studyKey)
                        .orElseThrow(() -> new BaseException(ErrorCode.STUDY_NOT_FOUND));
                if (study.getOrthancId() == null) {
                    log.warn("orthancStudyId가 없습니다. studyKey: {} (동기화 필요)", studyKey);
                    throw new BaseException(ErrorCode.STUDY_NOT_SYNCED);
                }
                orthancIds.add(study.getOrthancId());
            }
        }
        if (request.seriesKeys() != null) {
            for (Long seriesKey : request.seriesKeys()) {
                Series series = seriesRepository.findById(seriesKey)
                        .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));
                if (series.getOrthancId() == null) {
                    log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
                    throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
                }
                orthancIds.add(series.getOrthancId());
            }
        }

        if (orthancIds.isEmpty()) {
            throw new BaseException(ErrorCode.EMPTY_DOWNLOAD_SELECTION);
        }

        String url = orthancUrl + "/tools/create-archive";

        // Orthanc가 요청 바디: {"Resources": ["id1", "id2", ...]}
        // orthancId는 Orthanc가 내부적으로 발급한 16진수 해시라 특수문자가 섞일 일이 없어 직접 조립해도 안전할거임
        String requestBody = "{\"Resources\":[" +
                orthancIds.stream().map(id -> "\"" + id + "\"").collect(Collectors.joining(",")) +
                "]}";

        //스트리밍 프록시 (GET이 아니라 POST라는 것만 다르고 나머지 흐름은 위 study/series 다운로드와 동일)
        return outputStream -> restTemplate.execute(
                url,
                HttpMethod.POST,
                clientHttpRequest -> {
                    clientHttpRequest.getHeaders().setContentType(MediaType.APPLICATION_JSON);
                    clientHttpRequest.getBody().write(requestBody.getBytes(StandardCharsets.UTF_8));
                },
                clientHttpResponse -> {
                    StreamUtils.copy(clientHttpResponse.getBody(), outputStream);
                    return null;
                }
        );
    }

    /**
     * 새 환자를 추가합니다.
     *
     * @param doctorKey 의사 키
     * @param request 환자 요청 DTO
     */
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

    /**
     * 업로드된 DICOM 파일을 처리하여 태그를 추출하고 Orthanc에 업로드합니다.
     *
     * @param patientKey 환자 키
     * @param file 업로드된 multipart DICOM 파일
     * @throws IOException 파일 읽기에 실패한 경우
     */
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
        String seriesDescription = attrs.getString(Tag.SeriesDescription);
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
                        .seriesDescription(seriesDescription)
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

    /**
     * REST API를 통해 Orthanc에 원본 DICOM 파일을 업로드합니다.
     * Orthanc REST API로 DICOM 파일 원본을 그대로 POST 업로드한다.
     * 응답에 이 인스턴스가 속한 Study/Series의 Orthanc 내부 ID(ParentStudy/ParentSeries)가 들어있다.
     *
     * @param fileBytes DICOM 파일 바이트
     * @return Orthanc 인스턴스 응답
     */
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

    /**
     * Orthanc POST /instances 응답을 나타내는 Record입니다.
     * Orthanc의 POST /instances 응답 중 우리가 필요한 필드만 뽑아서 받는 record.
     * 예) {"ID": "...", "ParentStudy": "...", "ParentSeries": "...", "Status": "Success"}
     */
    private record OrthancInstanceResponse(
            @JsonProperty("ID") String id,
            @JsonProperty("ParentStudy") String parentStudy,
            @JsonProperty("ParentSeries") String parentSeries,
            @JsonProperty("Status") String status
    ) {}

    /**
     * DICOM 숫자 문자열을 정수로 안전하게 구문 분석합니다.
     * "7", "007" 같은 DICOM 숫자 태그 문자열을 정수로 안전하게 변환한다. 값이 없거나 이상하면 0.
     *
     * @param value 문자열 값
     * @return 구문 분석된 정수
     */
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

    /**
     * DICOM 날짜 및 시간을 LocalDateTime으로 구문 분석합니다.
     * DICOM Date(yyyyMMdd) + Time(HHmmss.ffffff)을 LocalDateTime으로 합쳐준다.
     * Study/Series 양쪽에서 재사용. 날짜/시간이 없거나 형식이 이상하면 최대한 방어적으로 처리한다.
     *
     * @param date 날짜 문자열
     * @param time 시간 문자열
     * @return 구문 분석된 LocalDateTime
     */
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
}
