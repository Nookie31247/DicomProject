package com.allegro.dicomback.entity.anonymization;

import com.allegro.dicomback.entity.Series;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "anonymization_series")
@Getter
@Setter
@NoArgsConstructor
public class AnonymizationSeries {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    //가명화 시리즈 고유 번호
    @Column(name = "AnonSeriesKey")
    private Long anonSeriesKey;

    //시리즈 내부 식별 키
   @ManyToOne(fetch = FetchType.LAZY)
   @JoinColumn(name = "SeriesKey", nullable = false)
    private Series seriesKey;

    //가명화 로그 고유 번호
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "AnonKey", nullable = false)
    private AnonymizationStudy anonStudyKey;

    //가명화된 SeriesInstanceUID
    @Column(name = "AnonSeriesUID" , unique = true, nullable = false,length = 128)
    private String anonSeriesUID;


}
