//package com.allegro.dicomback.entity;
//
//import jakarta.persistence.*;
//import lombok.*;
//import java.time.LocalDateTime;
//
//@Entity
//@Table(name = "images")
//@Getter
//@Setter
//@NoArgsConstructor
//@AllArgsConstructor
//@Builder
//public class Image {
//
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    @Column(name = "ImageKey")
//    private Long imageKey;
//
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "SeriesKey", nullable = false)
//    private Series series;
//
//    @Column(name = "SOPInstanceUID", unique = true, length = 128)
//    private String sopInstanceUid;
//
//
//    @Column(name = "SOPClassUID", length = 128)
//    private String sopClassUid;
//
//    @Column(name = "InstanceNum")
//    private Integer instanceNum;
//
//    @Column(name = "Path")
//    private String path;
//
//    @Column(name = "ImageDateTime")
//    private LocalDateTime imageDateTime;
//
//    // 소프트 삭제 여부 (0: 정상, 1: 삭제)
//    @Builder.Default
//    @Column(name = "DelFlag", nullable = false)
//    private Integer delFlag = 0;
//
//    // 소프트 삭제
//    public void delete() {
//        this.delFlag = 1;
//    }
//}