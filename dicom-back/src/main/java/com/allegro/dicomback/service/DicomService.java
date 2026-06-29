package com.allegro.dicomback.service;

import com.allegro.dicomback.entity.Image;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody; //->생성되는 즉시 조금씩 클라이언트에게 전송하는 방식 대용량에 적합

import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    @Value("${orthanc.url}")
    private String orthancUrl;

    private final ImageRepository imageRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    // Orthanc Instance ID(image.getPath())로 실제 파일 다운로드 URL 조합
    public String buildDownloadUrl(Image image) {
        return orthancUrl + "/instances/" + image.getPath() + "/file";
    }

    // Orthanc에서 단일 인스턴스(.dcm) 바이트를 받아옴. 없으면 null
    private byte[] fetchFromOrthanc(Image image) {
        String url = buildDownloadUrl(image);
        try {
            return restTemplate.getForObject(url, byte[].class);
        } catch (Exception e) {
            log.warn("Orthanc에서 파일을 가져오지 못함: {} ({})", image.getPath(), e.getMessage());
            return null;
        }
    }

    ///단일 이미지(.dcm) 다운 - Orthanc에서 바로 받아옴
    public Resource downloadImage(Long imageKey) {
        Image image = imageRepository.findById(imageKey)
                .orElseThrow(() -> new BaseException(ErrorCode.IMAGE_NOT_FOUND));

        if (image.getDelFlag() == 1) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND); // 삭제된 파일 접근 차단
        }

        byte[] fileBytes = fetchFromOrthanc(image);
        if (fileBytes == null) {
            throw new BaseException(ErrorCode.FILE_NOT_FOUND_ON_DISK);
        }

        return new ByteArrayResource(fileBytes);
    }

    ///시리즈 전체를 Zip으로 압축하여 다운로드 (StreamingResponseBody 사용)
    public StreamingResponseBody downloadSeriesAsZip(Long seriesKey) {
        //해당 series에 속한 모든 정상 데이터를 조회한다.
        List<Image> images = imageRepository.findBySeries_SeriesKeyAndDelFlagOrderByInstanceNumAsc(seriesKey, 0);

        if (images.isEmpty()) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }
        return outputStream -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (Image image : images) {
                    byte[] fileBytes = fetchFromOrthanc(image);

                    //Orthanc에 파일이 없을 때
                    if (fileBytes == null) {
                        log.warn("작업 중 누락된 파일 건너뜀(Orthanc): {}", image.getPath());
                        continue;
                    }
                    //Instance번호_SOP_UID.dcm
                    String fileName = String.format("%04d_%s.dcm", image.getInstanceNum(), image.getSopInstanceUid());
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipOutputStream.putNextEntry(zipEntry);

                    zipOutputStream.write(fileBytes);
                    zipOutputStream.closeEntry();
                }
            } catch (Exception e) {
                log.error("시리즈용 ZIP 파일 생성 오류: {}: {}", seriesKey, e.getMessage());
            }
        };
    }

    ///스터디 전체를 ZIP으로 실시간 압축하여 다운로드 (시리즈별 폴더 생성)
    public StreamingResponseBody downloadStudyAsZip(Long studyKey) {
        // 해당 Study에 속한 모든 정상 이미지 조회
        List<Image> images = imageRepository.findBySeries_Study_StudyKeyAndDelFlag(studyKey, 0);

        if (images.isEmpty()) {
            throw new BaseException(ErrorCode.STUDY_NOT_FOUND);
        }

        return outputStream -> {
            try (ZipOutputStream zos = new ZipOutputStream(outputStream)) {
                for (Image img : images) {
                    byte[] fileBytes = fetchFromOrthanc(img);

                    if (fileBytes == null) {
                        log.warn("Study ZIP 작업 중 누락된 파일 건너뜀(Orthanc): {}", img.getPath());
                        continue;
                    }

                    // 시리즈 번호를 폴더명으로 사용하여 압축 파일 내부에 디렉토리 구조 생성
                    Integer seriesNum = img.getSeries().getSeriesNum();
                    String folderName = String.format("Series_%04d/", seriesNum != null ? seriesNum : 0);
                    String fileName = String.format("%04d_%s.dcm", img.getInstanceNum(), img.getSopInstanceUid());

                    ZipEntry zipEntry = new ZipEntry(folderName + fileName);
                    zos.putNextEntry(zipEntry);

                    zos.write(fileBytes);
                    zos.closeEntry();
                }
            } catch (Exception e) {
                log.error("Study ZIP 파일 생성 오류 {}: {}", studyKey, e.getMessage());
            }
        };
    }
}
