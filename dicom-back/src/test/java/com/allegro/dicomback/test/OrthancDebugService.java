//package com.allegro.dicomback.test;
//
//import com.fasterxml.jackson.databind.JsonNode;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import org.springframework.stereotype.Service;
//import org.springframework.web.reactive.function.client.WebClient;
//
//@Service
//public class OrthancDebugService {
//
//    private final WebClient webClient;
//    private final ObjectMapper objectMapper;
//
//    public OrthancDebugService(WebClient.Builder webClientBuilder) {
//        this.webClient = webClientBuilder.baseUrl("http://localhost:8042").build();
//        this.objectMapper = new ObjectMapper();
//    }
//
//    /**
//     * Orthanc에 등록된 특정 Instance ID의 DICOM 태그 정보를 가져와 터미널에 쪼개어 출력합니다.
//     */
//    public void printDicomMetadata(String instanceId) {
//        try {
//            // [수정] 중요: /tags 대신 구조가 직관적인 /simplified-tags API를 호출합니다.
//            String jsonResponse = this.webClient.get()
//                    .uri("/instances/{id}/simplified-tags", instanceId)
//                    .retrieve()
//                    .bodyToMono(String.class)
//                    .block();
//
//            JsonNode rootNode = objectMapper.readTree(jsonResponse);
//
//            System.out.println("\n==================================================");
//            System.out.println(" [ORTHANC DICOM DATA PARSING TEST] ");
//            System.out.println("==================================================");
//
//            // 1. Patient 계층 데이터 쪼개기
//            System.out.println("\n[1] PATIENT INFORMATION");
//            System.out.println(" - Patient ID   : " + getSimplifiedTagValue(rootNode, "PatientID"));
//            System.out.println(" - Patient Name : " + getSimplifiedTagValue(rootNode, "PatientName"));
//            System.out.println(" - Birth Date   : " + getSimplifiedTagValue(rootNode, "PatientBirthDate"));
//            System.out.println(" - Sex          : " + getSimplifiedTagValue(rootNode, "PatientSex"));
//
//            // 2. Study 계층 데이터 쪼개기
//            System.out.println("\n[2] STUDY INFORMATION");
//            System.out.println(" - Study Instance UID : " + getSimplifiedTagValue(rootNode, "StudyInstanceUID"));
//            System.out.println(" - Study Date         : " + getSimplifiedTagValue(rootNode, "StudyDate"));
//            System.out.println(" - Description        : " + getSimplifiedTagValue(rootNode, "StudyDescription"));
//
//            // 3. Series 계층 데이터 쪼개기
//            System.out.println("\n[3] SERIES INFORMATION");
//            System.out.println(" - Series Instance UID: " + getSimplifiedTagValue(rootNode, "SeriesInstanceUID"));
//            System.out.println(" - Modality (장비 종류): " + getSimplifiedTagValue(rootNode, "Modality"));
//            System.out.println(" - Body Part Examined : " + getSimplifiedTagValue(rootNode, "BodyPartExamined"));
//
//            // 4. Instance (Image 픽셀 정보) 계층 데이터 쪼개기
//            System.out.println("\n[4] INSTANCE & IMAGE SPEC");
//            System.out.println(" - SOP Instance UID   : " + getSimplifiedTagValue(rootNode, "SOPInstanceUID"));
//            System.out.println(" - Rows (세로 해상도)  : " + getSimplifiedTagValue(rootNode, "Rows"));
//            System.out.println(" - Columns (가로 해상도): " + getSimplifiedTagValue(rootNode, "Columns"));
//            System.out.println(" - Bits Allocated     : " + getSimplifiedTagValue(rootNode, "BitsAllocated"));
//
//            System.out.println("==================================================\n");
//
//        } catch (Exception e) {
//            System.err.println("Orthanc 통신 또는 파싱 실패: " + e.getMessage());
//        }
//    }
//
//    /**
//     * [수정] simplified-tags 응답 구조 {"PatientID": "값"} 에서 곧바로 스트링을 추출하는 메소드
//     */
//    private String getSimplifiedTagValue(JsonNode rootNode, String tagName) {
//        JsonNode tagNode = rootNode.path(tagName);
//        if (!tagNode.isMissingNode() && !tagNode.isNull()) {
//            return tagNode.asText();
//        }
//        return "N/A (데이터 없음)";
//    }
//}
