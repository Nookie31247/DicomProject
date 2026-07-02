package com.allegro.dicomback.component;

import com.allegro.dicomback.dto.UserRequestDto;
import com.allegro.dicomback.entity.Patient;
import com.allegro.dicomback.entity.user.User;
import com.allegro.dicomback.repository.PatientRepository;
import com.allegro.dicomback.repository.UserRepository;
import com.allegro.dicomback.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
public class DbTestComponent implements CommandLineRunner {
    private final PatientRepository pRepo;
    private final UserService userService;

    @Override
    public void run(String... args) throws Exception {
        userService.signup(new UserRequestDto.SignupRequest("test", "test", "테스트", "의료진"));

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
}
