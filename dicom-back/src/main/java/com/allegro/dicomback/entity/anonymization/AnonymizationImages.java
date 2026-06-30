//package com.allegro.dicomback.entity.anonymization;
//
//import com.allegro.dicomback.entity.Image;
//import jakarta.persistence.*;
//import lombok.Getter;
//import lombok.NoArgsConstructor;
//import lombok.Setter;
//
//@Entity
//@Table(name = "anonymization_images")
//@Getter
//@Setter
//@NoArgsConstructor
//public class AnonymizationImages {
//    @Id
//    @GeneratedValue(strategy = GenerationType.IDENTITY)
//    //가명화 이미지 고유 번호
//    @Column(name = "AnonImageKey")
//    private Long anonImageKey;
//
//    //인스턴스 단면 내부 고유 키
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "ImageKey", nullable = false)
//    private Image imageKey;
//
//    //가명화 작업 번호
//    @ManyToOne(fetch = FetchType.LAZY)
//    @JoinColumn(name = "AnonKey", nullable = false)
//    private AnonymizationStudy anonStudyKey;
//
//    //가명화된 SOPInstanceUID
//    @Column(name = "AnonSOPUID" , unique = true, nullable = false,length = 128)
//    private String anonSopUid;
//
//}
