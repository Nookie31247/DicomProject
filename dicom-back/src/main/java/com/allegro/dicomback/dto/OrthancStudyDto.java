//package com.allegro.dicomback.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//
//import java.util.List;
//
//@Data
//public class OrthancStudyDto {
//
//    @JsonProperty("ID")
//    private String ID; // Orthanc 내부 ID
//
//    @JsonProperty("MainDicomTags")
//    private MainDicomTags mainDicomTags;
//
//    @JsonProperty("PatientMainDicomTags")
//    private PatientMainDicomTags patientMainDicomTags;
//
//    @JsonProperty("Series")
//    private List<String> series; // 시리즈 UUID 리스트
//
//    @Data
//    public static class MainDicomTags {
//        @JsonProperty("StudyInstanceUID")
//        private String studyInstanceUID;
//        @JsonProperty("StudyDate")
//        private String studyDate;
//        @JsonProperty("StudyTime")
//        private String studyTime;
//        @JsonProperty("StudyDescription")
//        private String studyDescription;
//        @JsonProperty("AccessionNumber")
//        private String accessionNumber;
//    }
//
//    @Data
//    public static class PatientMainDicomTags {
//        @JsonProperty("PatientID")
//        private String patientID;
//        @JsonProperty("PatientName")
//        private String patientName;
//        @JsonProperty("PatientBirthDate")
//        private String patientBirthDate;
//    }
//}
