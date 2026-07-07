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
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody;
import com.allegro.dicomback.dto.DicomResponseDto.*;
import com.allegro.dicomback.dto.DicomRequestDto.*;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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

    public void processDicomFile(MultipartFile file) throws IOException {
        try (DicomInputStream dis = new DicomInputStream(file.getInputStream())) {
            Attributes attrs = dis.readDataset(-1, -1);

            String studyInstanceUid = attrs.getString(Tag.StudyInstanceUID);
            String studyDate = attrs.getString(Tag.StudyDate);
            String studyTime = attrs.getString(Tag.StudyTime);
            String studyDescription = attrs.getString(Tag.StudyDescription);

            log.info("--- DICOM 태그 추출 성공 ---");
            log.info("UID: {}", studyInstanceUid);
            log.info("Date/Time: {} {}", studyDate, studyTime);
            log.info("Description: {}", studyDescription);
        }
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