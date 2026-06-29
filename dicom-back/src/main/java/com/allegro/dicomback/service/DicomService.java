package com.allegro.dicomback.service;

import com.allegro.dicomback.entity.Image;
import com.allegro.dicomback.exception.BaseException;
import com.allegro.dicomback.exception.ErrorCode;
import com.allegro.dicomback.repository.ImageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody; //->생성되는 즉시 조금씩 클라이언트에게 전송하는 방식 대용량에 적합

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DicomService {

    private final ImageRepository imageRepository;

    ///단일 이미지(.dom) 다운
    public Resource downloadImage(Long imageKey) {
        Image image = imageRepository.findById(imageKey)
                .orElseThrow(() -> new BaseException(ErrorCode.IMAGE_NOT_FOUND)); //에러코드

        if (image.getDelFlag() == 1) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND); // 삭제된 파일 접근 차단
        }

        //존재 여부 확인
        Path filePath = Paths.get(image.getPath());
        if (!Files.exists(filePath)) {
            log.error("File not found on disk: {}", filePath);
            throw new BaseException(ErrorCode.FILE_NOT_FOUND_ON_DISK);
        }

        return new FileSystemResource(filePath);
    }

    ///시리즈 전체를 Zip으로 압축하여 다운로드 (StreamingResponseBody 사용)
    public StreamingResponseBody downloadSeriesAsZip(Long seriesKey) {
        //해당 series에 속한 모든 데이터를 조회한다.
        List<Image> images = imageRepository.findBySeries_SeriesKeyAndDelFlagOrderByInstanceNumAsc(seriesKey, 1);

        if (images.isEmpty()) {
            throw new BaseException(ErrorCode.IMAGE_NOT_FOUND);
        }
        return outputStream -> {
            try (ZipOutputStream zipOutputStream = new ZipOutputStream(outputStream)) {
                for (Image image : images) {
                    Path filePath = Paths.get(image.getPath());

                    //파일이 없을 때
                    if (!Files.exists(filePath)) {
                        log.warn("작업 중 누락된 파일 건너뜀:: {}", filePath);
                        continue;
                    }
                    //Instance번호_SOP_UID.dcm
                    String fileName = String.format("%04d_%s.dcm", image.getInstanceNum(), image.getSopInstanceUid());
                    ZipEntry zipEntry = new ZipEntry(fileName);
                    zipOutputStream.putNextEntry(zipEntry);

                    // 디스크에서 읽어서 바로 클라이언트로 전송
                    Files.copy(filePath, zipOutputStream);
                    zipOutputStream.closeEntry();
                }
            }catch (Exception e) {
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
                    Path filePath = Paths.get(img.getPath());

                    if (!Files.exists(filePath)) {
                        log.warn("Study ZIP 작업 중 누락된 파일 건너뜀: {}", filePath);
                        continue;
                    }

                    // 시리즈 번호를 폴더명으로 사용하여 압축 파일 내부에 디렉토리 구조 생성
                    Integer seriesNum = img.getSeries().getSeriesNum();
                    String folderName = String.format("Series_%04d/", seriesNum != null ? seriesNum : 0);
                    String fileName = String.format("%04d_%s.dcm", img.getInstanceNum(), img.getSopInstanceUid());

                    ZipEntry zipEntry = new ZipEntry(folderName + fileName);
                    zos.putNextEntry(zipEntry);

                    Files.copy(filePath, zos);
                    zos.closeEntry();
                }
            } catch (Exception e) {
                log.error("Study ZIP 파일 생성 오류 {}: {}", studyKey, e.getMessage());
            }
        };
    }
}