package com.allegro.dicomback.reprostore;

import com.allegro.dicomback.entity.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ImageRepository extends JpaRepository<Image, Long> {

    // Series에 속한 단면 이미지 목록을 슬라이스 순서대로 조회
    List<Image> findBySeries_SeriesKeyAndDelFlagOrderByInstanceNumAsc(Long seriesKey, Integer delFlag);

    // 단일 영상 고유 UID(SOPInstanceUID)로 단건 조회
    Image findBySOPInstanceUID(String sopInstanceUID);
}