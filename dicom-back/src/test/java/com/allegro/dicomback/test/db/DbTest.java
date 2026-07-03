package com.allegro.dicomback.test.db;

import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.repository.PatientRepository;
import lombok.RequiredArgsConstructor;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestConstructor;

import java.time.LocalDateTime;
import java.util.List;

@SpringBootTest
@TestConstructor(autowireMode = TestConstructor.AutowireMode.ALL)   // JUnit5에서 Constructor Injection 활성화
@TestInstance(TestInstance.Lifecycle.PER_CLASS)                     // @BeforeAll을 non-static으로 사용하기 위함
@RequiredArgsConstructor
public class DbTest {
    private final PatientRepository pRepo;

    @BeforeAll
    public void setUpClass() {
        List<Patient> samples = List.of(
            Patient.builder()
                .id("1234")
                .name("홍길동")
                .birth(LocalDateTime.of(1999, 12, 24, 0, 0))
                .sex("M")
                .recentStudy(LocalDateTime.of(2026, 3, 29, 15, 27))
                .studyCount(3)
                .build(),

            Patient.builder()
                .id("1235")
                .name("김철수")
                .birth(LocalDateTime.of(1985, 5, 14, 0, 0))
                .sex("M")
                .recentStudy(LocalDateTime.of(2026, 4, 12, 9, 30))
                .studyCount(1)
                .build(),

            Patient.builder()
                .id("1236")
                .name("이영희")
                .birth(LocalDateTime.of(1992, 8, 21, 0, 0))
                .sex("F")
                .recentStudy(LocalDateTime.of(2026, 5, 1, 14, 15))
                .studyCount(5)
                .build(),

            Patient.builder()
                .id("1237")
                .name("김민수")
                .birth(LocalDateTime.of(1970, 11, 3, 0, 0))
                .sex("M")
                .recentStudy(LocalDateTime.of(2026, 6, 15, 11, 0))
                .studyCount(2)
                .build(),

            Patient.builder()
                .id("1238")
                .name("최수지")
                .birth(LocalDateTime.of(2005, 2, 10, 0, 0))
                .sex("F")
                .recentStudy(LocalDateTime.of(2026, 7, 2, 10, 45))
                .studyCount(0)
                .build()
        );

        pRepo.saveAll(samples);
    }

    @Test
    @DisplayName("환자 전체 불러오기")
    public void getAllPatients() {
        List<Patient> patients = pRepo.findAll();
        printPatientInfo(patients);
    }

    @Test
    @DisplayName("이름으로 검색하기")
    public void getPatientByName() {
        List<Patient> patients = pRepo.findByNameContains("김");    // 김민수와 김철수가 나와야됨
        printPatientInfo(patients);
    }

    @Test
    @DisplayName("날짜 범위 검색하기")
    public void getPatientByDateRange() {   // 김민수와 최수지가 나와야됨
        List<Patient> patients = pRepo.findByRecentStudyBetween(
                LocalDateTime.of(2026, 6, 1, 0, 0),
                LocalDateTime.of(2026, 7, 31, 0, 0)
        );
        printPatientInfo(patients);
    }

    @Test
    @DisplayName("날짜 + 이름으로 검색하기")
    public void getPatientByNameAndDateRange() {    // 이영희가 나와야됨
        List<Patient> patients = pRepo.findByNameContainsAndRecentStudyBetween(
                "이영희",
                LocalDateTime.of(2026, 5, 1, 0, 0),
                LocalDateTime.of(2026, 5, 5, 0, 0)
        );
        printPatientInfo(patients);
    }

    private void printPatientInfo(List<Patient> patients) {
        for(Patient patient : patients) {
            System.out.println(patient.getId() + ": " + patient.getName() + " (" + patient.getRecentStudy() + ")");
        }
    }
}
