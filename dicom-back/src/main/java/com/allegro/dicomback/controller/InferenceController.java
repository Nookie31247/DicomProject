package com.allegro.dicomback.controller;

import com.allegro.dicomback.AI.AiModelRegistry;
import com.allegro.dicomback.AI.DicomPixelReader;
import lombok.extern.slf4j.Slf4j;
import com.allegro.dicomback.config.JwtTokenProvider;
import com.allegro.dicomback.entity.ai.AuditLog;
import com.allegro.dicomback.repository.AiDetectionRepository;
import com.allegro.dicomback.repository.AiResultsRepository;
import com.allegro.dicomback.repository.AuditLogRepository;
import com.allegro.dicomback.entity.ai.AiDetection;
import com.allegro.dicomback.entity.ai.AiResults;
import com.allegro.dicomback.service.AiService;
import com.allegro.dicomback.service.DicomService;
import com.allegro.dicomback.service.InferenceService;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class InferenceController {
    private final InferenceService service;
    private final DicomService dicomService;
    private final AiModelRegistry modelRegistry;
    private final AiService aiService;

    //ai 결과 Db 저장
    private final AiResultsRepository aiResultsRepository;
    private final AiDetectionRepository aiDetectionRepository;
    private final AuditLogRepository auditLogRepository;
    private final JwtTokenProvider jwtTokenProvider;

    @Value("${ai.model-path:models/CR_pneumonia_yolov8n.onnx}")
    private String modelPath;

    // 레거시/디버그용: 서버 로컬 파일 경로를 직접 읽어서 고정 모델로 추론. 실제 뷰어는 안 씀.
    @PostMapping("/infer")
    public List<InferenceService.BoundingBox> infer(@RequestBody InferRequest req) throws Exception {
        return service.infer(Path.of(req.dicomPath()), Path.of("models/CR_pneumonia_yolov8n.onnx"));
    }
    record InferRequest(String dicomPath) {}

    // 레거시 경로: 서버가 Orthanc에서 원본 DICOM byte[]를 다시 받아 직접 디코딩(dcm4che-imageio-opencv 필요)
    @GetMapping("/series/{seriesKey}/instances/{instanceId}/detect")
    public List<BoxDto> detect(@PathVariable Long seriesKey, @PathVariable String instanceId) throws Exception {
        byte[] dicomBytes = aiService.getInstanceBytes(seriesKey, instanceId);
        List<InferenceService.BoundingBox> boxes = service.inferOnOriginalImage(dicomBytes, Path.of(modelPath));

        return boxes.stream()
                .map(b -> new BoxDto(
                        b.x_min(),
                        b.y_min(),
                        b.x_max() - b.x_min(),
                        b.y_max() - b.y_min(),
                        b.confidence()
                ))
                .toList();
    }

    // 현재 뷰어가 실제로 호출하는 경로: 프론트가 화면에 띄우면서 이미 압축을 풀어놓은 픽셀 배열을 그대로 받는다.
    // modality(+bodyPart)로 AiModelRegistry가 어떤 onnx를 쓸지 자동으로 고른다.
    // - 확정되면 바로 추론해서 boxes를 채워 반환
    // - 애매하면(같은 modality를 여러 모델이 공유하는데 bodyPart로도 못 좁힐 때(솔직히 혹시 몰라서 넣은거지 이럴일은 없을듯?)) candidates만 채워 반환 → 프론트가 선택 UI를 띄움
    // - 아예 지원 안 하면(태그 없음/미등록 modality) unsupportedReason만 채워 반환
    // modelKey가 요청에 실려오면(사용자가 candidates 중 하나를 직접 골랐을 때) 자동 판단을 건너뛰고 그 모델을 강제로 사용(아까말한 대로 솔직히 안 할 것 같음)
    @PostMapping("/detect-raw")
    public DetectRawResponse detectRaw(
            @RequestBody RawDetectRequest req,
            // token 파라미터 추가: 이 요청을 보낸 사람이 누군지 알아내서 감사 로그(AuditLog)에 userKey로 남기기 위함.
            // required = false인 이유는 쿠키가 없을 수도 있기 때문에 없으면 그냥 "누가 했는지 모름(null)"으로 기록하고 넘어간다.
            @CookieValue(name = "token", required = false) String token
    ) throws Exception {
        AiModelRegistry.ModelRule rule;

        if (req.modelKey() != null && !req.modelKey().isBlank()) {
            rule = modelRegistry.findByKey(req.modelKey())
                    .orElse(null);
            if (rule == null) {
                return new DetectRawResponse(null, null, "알 수 없는 모델 key입니다: " + req.modelKey());
            }
        } else {
            var result = modelRegistry.resolve(req.modality(), req.bodyPart());

            if (result instanceof AiModelRegistry.Unsupported u) {
                return new DetectRawResponse(null, null, u.reason());
            }
            if (result instanceof AiModelRegistry.Ambiguous a) {
                var choices = a.candidates().stream()
                        .map(r -> new ModelChoiceDto(r.key(), r.displayName()))
                        .toList();
                return new DetectRawResponse(null, choices, null);
            }
            rule = ((AiModelRegistry.Resolved) result).rule();
        }

        byte[] pixelBytes = java.util.Base64.getDecoder().decode(req.pixelDataBase64());
        List<InferenceService.BoundingBox> boxes = service.inferOnRawPixels(
                req.rows(), req.cols(),
                req.windowCenter(), req.windowWidth(),
                req.slope(), req.intercept(),
                req.signed(), pixelBytes, rule.modelPath(), rule.confidenceThreshold());

        var boxDtos = boxes.stream()
                .map(b -> new BoxDto(
                        b.x_min(),
                        b.y_min(),
                        b.x_max() - b.x_min(),
                        b.y_max() - b.y_min(),
                        b.confidence()
                ))
                .toList();

        if (req.seriesKey() != null) {
            saveInferenceResult(req.seriesKey(), req.instanceId(), rule, boxes, token);
        }

        return new DetectRawResponse(boxDtos, null, null);
    }

    // I 추론 결과와 탐지 박스를 DB에 저장하고, 감사 로그도 함께 남긴다.
    //DB저장이 실패해도 사용자에게 보여줄 AI 판독 결과에는 영향이 없도록 예외처리
    //저장 실패 시 서버 로그에 경고만 남기고 API 응답은 정상적으로 나간다
    private void saveInferenceResult(
            Long seriesKey, String instanceId,
            AiModelRegistry.ModelRule rule,
            List<InferenceService.BoundingBox> boxes,
            String token) {
        try {
            //추론 작업 자체를 기록
            AiResults result = new AiResults();
            result.setSeriesKey(seriesKey);
            result.setModelKey(rule.key());             //"tumor(조양)" / "pneumonia(폐렴)" 같은 내부 식별자
            result.setModelName(rule.displayName());     //"뇌종양(CT/MR)" 같은 화면 표시용 이름
            result.setCreatedAt(LocalDateTime.now());    //지금은 동기 처리 시작 시각
            result.setFinishedAt(LocalDateTime.now());   //종료 시각도 시작 시각이랑 사실상 거의 같은 시점
            //박스가 하나도 없으면 "NO_DETECTION", 있으면 "SUCCESS"으로 상태 표시
            result.setStatus(boxes.isEmpty() ? "NO_DETECTION" : "SUCCESS");
            aiResultsRepository.save(result);

            //박스 하나당 한 행씩. resultKey로 result 행과 연결.
            for (InferenceService.BoundingBox b : boxes) {
                AiDetection d = new AiDetection();
                d.setResultKey(result.getResultKey());
                d.setInstanceId(instanceId); //정확하게 어떤 인스턴스에서 나온 박스인지 표시
                d.setConfidence(b.confidence());
                // BoundingBox는 x_min,y_min,x_max,y_max인데
                // 테이블은 x,y,width,height 형태라 변환해서 저장
                d.setBoxX((int) b.x_min());
                d.setBoxY((int) b.y_min());
                d.setBoxWidth((int) (b.x_max() - b.x_min()));
                d.setBoxHeight((int) (b.y_max() - b.y_min()));
                d.setClassName(rule.displayName()); // 모델당 클래스가 1개뿐이라 모델 이름을 그대로 병변명으로 사용
                aiDetectionRepository.save(d);
            }

            //AI 추론도 환자 영상 데이터를 들여다본 행위이므로 반드시 로그에 남긴다.
            AuditLog log = new AuditLog();
            log.setUserKey(resolveUserKeyOrNull(token)); // 토큰이 없거나 이상하면 null(익명)로 기록
            log.setActionType("AI_INFER");
            log.setTargetType("SERIES");
            log.setTargetUID(String.valueOf(seriesKey));
            log.setCreatedAt(LocalDateTime.now());
            auditLogRepository.save(log);
        } catch (Exception e) {
            // DB 저장 중 어떤 이유로든 실패하면 경고 로그를 남긴다.
            log.warn("AI 결과 저장 실패 (seriesKey={}): {}", seriesKey, e.getMessage());
        }
    }

    // 쿠키에 담긴 JWT 토큰 문자열을 넣으면 그 안에 들어있는 사용자 고유 번호(userKey)를 꺼내고
    // token이 null이면 토큰이 만료/위조되어 파싱에 실패하면 예외 대신 null을 반환해서
    // 로그인 안 한 사람이 요청했다 정도로 처리하고 넘어간다.
    private Long resolveUserKeyOrNull(String token) {
        if (token == null) return null;
        try { return jwtTokenProvider.getUserKey(token); } catch (Exception e) { return null; }
    }

    // 프론트 cornerstone image 객체에서 그대로 뽑아 보낸다.
    // windowCenter/windowWidth는 nullable(Double)이다 - 프론트는  데이터셋에(0028,1050)/(0028,1051) 태그의 유무 판단있 있으면 그 값을 그대로 보내고
    // 없으면 null을 보낸다. 서버는 null일 때만 Preprocessor.preprocessRaw의 백분위수 fallback을 쓴다.
    // modality/bodyPart: AiModelRegistry가 자동으로 모델을 고르는 데 쓴다.
    // modelKey: candidates 중 사용자가 직접 골랐을 때만 채워서 보낸다 . 그 외엔 null
    public record RawDetectRequest(
            int rows,
            int cols,
            Double windowCenter,
            Double windowWidth,
            double slope,
            double intercept,
            boolean signed,
            String pixelDataBase64,
            String modality,
            String bodyPart,
            String modelKey,
            //API가 받던 JSON에는 이게 몇 번 시리즈의 어떤 이미지인가에 대해서는 정보가 아예 없기에 AI 판독은 결과를 DB에 저장하기 위해서 추가
            Long seriesKey,
            String instanceId   //
    ) {}

    // detect-raw 응답: boxes(성공)
    // candidates(모델 선택 필요)
    // unsupportedReason(지원 안 함)
    public record DetectRawResponse(
            List<BoxDto> boxes,
            List<ModelChoiceDto> candidates,
            String unsupportedReason
    ) {}

    public record ModelChoiceDto(String key, String displayName) {}

    public record BoxDto(
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("width") float width,
            @JsonProperty("height") float height,
            @JsonProperty("confidence") float confidence
    ) {}

    @GetMapping(value = "/visualize", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] visualize(@RequestParam String dicomPath) throws Exception {

        //infer에서 원본 Dicom 픽셀 좌표를 받아옴
        //createDicomImage()가 만드는 img도 가로: cols, 세로 rows로 변환(원본 크기로 그리기
        List<InferenceService.BoundingBox> boxes = service.infer(Path.of(dicomPath), Path.of("models/CR_pneumonia_yolov8n.onnx"));

        BufferedImage img = createDicomImage(Path.of(dicomPath));

        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));
        g.setFont(new Font("Arial", Font.BOLD, 20));

        for (var b : boxes) {
//            double scaleX = (double) img.getWidth() / 640.0;
//            double scaleY = (double) img.getHeight() / 640.0;
//
//            int x = (int) (b.x_min() * scaleX);
//            int y = (int) (b.y_min() * scaleY);
//            int w = (int) ((b.x_max() - b.x_min()) * scaleX);
//            int h = (int) ((b.y_max() - b.y_min()) * scaleY);

            int x=(int) b.x_min();
            int y=(int) b.y_min();
            int w=(int) (b.x_max() - b.x_min());
            int h=(int) (b.y_max() - b.y_min());

            g.drawRect(x, y, w, h);
            g.drawString(String.format("%.1f%%", b.confidence() * 100), x, y - 5);
        }
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private BufferedImage createDicomImage(Path dicomPath) throws Exception {
        byte[] dicomBytes = Files.readAllBytes(dicomPath);
        Attributes a;
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomBytes))) {
            a = dis.readDataset();
        }
        int rows = a.getInt(Tag.Rows, 0), cols = a.getInt(Tag.Columns, 0);
        double slope = a.getDouble(Tag.RescaleSlope, 1.0);
        double intercept = a.getDouble(Tag.RescaleIntercept, 0.0);

        double WL = a.getDouble(Tag.WindowCenter, 40);
        double WW = a.getDouble(Tag.WindowWidth, 80);
        double low = WL - WW / 2;

        BufferedImage decoded = DicomPixelReader.decode(dicomBytes);
        Raster raster = decoded.getRaster();
        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int p = raster.getSample(x, y, 0);
                double hu = p * slope + intercept;
                float normalized = (float) Math.min(1, Math.max(0, (hu - low) / WW));

                int gray = (int) (normalized * 255);
                int rgb = (gray << 16) | (gray << 8) | gray;
                img.setRGB(x, y, rgb);
            }
        }
        return img;
    }
}