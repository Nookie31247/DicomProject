package com.allegro.dicomback.component;

import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.dto.UserRequestDto;
import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.entity.Study;
import com.allegro.dicomback.entity.User;
import com.allegro.dicomback.repository.PatientRepository;
import com.allegro.dicomback.repository.SeriesRepository;
import com.allegro.dicomback.repository.StudyRepository;
import com.allegro.dicomback.repository.UserRepository;
import com.allegro.dicomback.service.DicomService;
import com.allegro.dicomback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbTestComponent implements CommandLineRunner {
    private final PatientRepository patientRepository;
    private final StudyRepository studyRepository;
    private final SeriesRepository seriesRepository;
    private final UserService userService;
    private final UserRepository userRepository;

    @Override
    public void run(String... args) throws Exception {
        userService.signup(new UserRequestDto.SignupRequest("test", "test", "테스트이름", "의료진"));
        User doctor = userRepository.findByUserId("test").orElseThrow();

        Patient patient1 = Patient.builder()
                .doctorKey(doctor)
                .name("홍길동")
                .birth(LocalDate.of(1999, 12, 24))
                .sex("M")
                .recentStudy(LocalDateTime.of(2017, 2, 2, 10, 41))
                .studyCount(3)
                .build();

        Patient patient2 = Patient.builder()
                .doctorKey(doctor)
                .name("김철수")
                .birth(LocalDate.of(1985, 5, 14))
                .sex("M")
                .recentStudy(LocalDateTime.of(2026, 4, 12, 9, 30))
                .studyCount(1)
                .build();

        patientRepository.save(patient1);
        patientRepository.save(patient2);

        Study study1 = Study.builder()
                .patientKey(patient1)
                .uid("1.2.410.200018.7.100.1.201608221411119450437")
                .description("SKULL")
                .createdAt(LocalDateTime.of(2016, 8, 22, 14,  31))
                .build();

        Study study2 = Study.builder()
                .patientKey(patient1)
                .uid("1.2.826.0.1.3680043.6.18930.1439.20170201163008.968.66")
                .description("Head^02HeadSeq (Adult)")
                .createdAt(LocalDateTime.of(2017, 2, 2, 10,  41))
                .build();

        studyRepository.save(study1);
        studyRepository.save(study2);


        Series series1 = Series.builder()
                .studyKey(study1)
                .uid("1.2.392.200036.9116.4.1.6116.40033.7002")
                .seriesNum(7)
                .bodyPart("SKULL")
                .totalImagesCount(152)
                .modality("MR")
                .build();

        Series series2 = Series.builder()
                .studyKey(study1)
                .uid("1.2.392.200036.9116.4.1.6116.40033.7004")
                .seriesNum(7)
                .bodyPart("SKULL")
                .totalImagesCount(36)
                .modality("MR")
                .build();

        seriesRepository.save(series1);
        seriesRepository.save(series2);
    }
}
