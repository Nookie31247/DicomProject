//package com.allegro.dicomback.dto;
//
//import com.fasterxml.jackson.annotation.JsonProperty;
//import lombok.Data;
//import java.util.List;
//
//// PACS 서버에서 실제로 받는 데이터를 명세서에 맞게 변환하기 위한 dto
//@Data
//public class OrthancPatientDto {
//    @JsonProperty("ID")
//    private String orthancId;
//
//    @JsonProperty("LastUpdate")
//    private String lastUpdate;
//
//    @JsonProperty("MainDicomTags")
//    private MainDicomTags mainDicomTags;
//
//    @JsonProperty("Studies")
//    private List<String> studies;
//
//    @Data
//    public static class MainDicomTags {
//        @JsonProperty("PatientID")
//        private String patientId;
//
//        @JsonProperty("PatientName")
//        private String patientName;
//
//        @JsonProperty("PatientBirthDate")
//        private String patientBirthDate;
//
//        @JsonProperty("PatientSex")
//        private String patientSex;
//    }
//}