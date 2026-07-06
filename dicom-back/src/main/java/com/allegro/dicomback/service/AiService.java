package com.allegro.dicomback.service;

import com.allegro.dicomback.entity.Series;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.SeriesRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiService {
    private final SeriesRepository seriesRepository;
    private final RestTemplate restTemplate;
    @Value("${orthanc.url:http://localhost:8042}")
    private String orthancUrl;


    // AI 추론용: 인스턴스 하나의 raw DICOM byte[] 조회
    public byte[] getInstanceBytes(Long seriesKey, String instanceId) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND));

        if (series.getOrthancId() == null) {
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        List<String> validInstanceIds = getInstanceIdsBySeries(seriesKey);
        if (!validInstanceIds.contains(instanceId)) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }

        String url = orthancUrl + "/instances/" + instanceId + "/file";
        return restTemplate.getForObject(url, byte[].class);
    }

    // 프론트엔드 뷰어를 위한 시리즈 내 인스턴스(단면) ID 목록 가져오기
    // InstanceNumber 기준으로 정렬해서 반환
    @SuppressWarnings("unchecked")
    public List<String> getInstanceIdsBySeries(Long seriesKey) {
        Series series = seriesRepository.findById(seriesKey)
                .orElseThrow(() -> new BaseException(ErrorCode.SERIES_NOT_FOUND)); // STUDY_NOT_FOUND -> SERIES_NOT_FOUND

        if (series.getOrthancId() == null) {
            log.warn("orthancSeriesId가 없습니다. seriesKey: {} (동기화 필요)", seriesKey);
            throw new BaseException(ErrorCode.SERIES_NOT_SYNCED);
        }

        // expand=true로 instance별 MainDicomTags(InstanceNumber 포함)까지 한 번에 조회
        String url = orthancUrl + "/series/" + series.getOrthancId() + "/instances?expand=true";
        List<Map<String, Object>> instances = restTemplate.getForObject(url, List.class);

        if (instances == null || instances.isEmpty()) {
            return java.util.Collections.emptyList();
        }

        return instances.stream()
                .sorted(Comparator.comparingInt(this::extractInstanceNumber))
                .map(inst -> (String) inst.get("ID"))
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unchecked")
    private int extractInstanceNumber(Map<String, Object> instance) {
        try {
            Map<String, Object> tags = (Map<String, Object>) instance.get("MainDicomTags");
            String num = (String) tags.get("InstanceNumber");
            return num != null ? Integer.parseInt(num.trim()) : Integer.MAX_VALUE;
        } catch (Exception e) {
            return Integer.MAX_VALUE; // 파싱 실패 시 맨 뒤로 밀어서 순서를 깨뜨리지 않음
        }
    }
}
