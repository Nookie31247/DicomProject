package com.allegro.dicomback.service;

import com.allegro.dicomback.entity.Image;
import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.entity.user.User;
import com.allegro.dicomback.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrthancSyncService {

    private final PatientRepository patientRepository;
    private final StudyRepository studyRepository;
    private final UserRepository userRepository;
    private final SeriesRepository seriesRepository;
    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // Orthanc 서버 주소
    private final String ORTHANC_URL = "http://localhost:8042";

    @Transactional
    public void syncInstancesFromOrthanc() {
        // Orthanc에 저장된 모든 Instance(단면 이미지)의 ID 목록을 가져옴
        String instancesUrl = ORTHANC_URL + "/instances";
        ResponseEntity<List<String>> response = restTemplate.exchange(
                instancesUrl, HttpMethod.GET, null, new ParameterizedTypeReference<List<String>>() {}
        );

        List<String> orthancInstanceIds = response.getBody();
        if (orthancInstanceIds == null) return;

        //ID를 하나씩 돌면서 상세 DICOM 태그(환자, 검사 정보 등)를 가져와서 DB에 저장
        for (String orthancId : orthancInstanceIds) {

            // Orthanc가 제공하는 해당 파일의 태그(DICOM 헤더) 정보 API
            String tagsUrl = ORTHANC_URL + "/instances/" + orthancId + "/simplified-tags";
            Map<String, String> tags = restTemplate.getForObject(tagsUrl, Map.class);

            if (tags == null) continue;

            String sopInstanceUid = tags.get("SOPInstanceUID");

            // 이미 DB에 있는 파일이면 스킵 (중복 저장 방지)
            if (imageRepository.findBySopInstanceUid(sopInstanceUid) != null) {
                continue;
            }

            // Orthanc가 파일을 바로 뱉어내는 API URL을 저장합니다.
            String orthancDownloadUrl = ORTHANC_URL + "/instances/" + orthancId + "/file";

            String patientId= tags.get("PatientID");
            Patient patient = patientRepository.findById(patientId).orElseGet(()-> {
                Patient p = Patient.builder()
                        .pId(patientId)
                        .pName(tags.get("PatientName"))
                        .pSex(tags.get("PatientSex"))
                        .build();
                return patientRepository.save(p);
            });

            String studyUid = tags.get("StudyInstanceUID");
            Study study= studyRepository.findByStudyInstanceUid(studyUid);
            if (study == null) {
                User defaultDoctor= userRepository.findByUserId("gojo_satoru").orElseThrow();

                study = Study.builder()
                        .studyInstanceUid(studyUid)
                        .patient(patient)
                        .doctor(defaultDoctor)
                        .modality(tags.get("Modality"))
                        .build();
                study= studyRepository.save(study);
            }

            String seriesUid = tags.get("SeriesInstanceUID");
            Series series= seriesRepository.findBySeriesInstanceUid(seriesUid);
            if (series == null) {
                series = Series.builder()
                        .seriesInstanceUid(seriesUid)
                        .study(study)
                        .seriesNum(Integer.parseInt(tags.getOrDefault("SeriesNumber", "0")))
                        .bodyPart(tags.get("BodyPartExamined"))
                        .build();
                series=seriesRepository.save(series);
            }

            Image image = Image.builder()
                    .series(series)
                    .sopInstanceUid(sopInstanceUid)
                    .sopClassUid(tags.get("SOPClassUID"))
                    .instanceNum(Integer.parseInt(tags.getOrDefault("InstanceNumber", "0")))
                    .path(orthancId) // 다운로드 URL 저장
                    .build();

            imageRepository.save(image);
            log.info("Synced Orthanc Instance: {}", orthancId);
        }
    }
}