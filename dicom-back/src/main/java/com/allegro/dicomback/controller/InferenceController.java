package com.allegro.dicomback.controller;

import com.allegro.dicomback.AI.AiModelRegistry;
import com.allegro.dicomback.AI.DicomPixelReader;
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
import java.util.List;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class InferenceController {
    private final InferenceService service;
    private final DicomService dicomService;
    private final AiModelRegistry modelRegistry;

    private final AiService aiService;

    @Value("${ai.model-path:models/CR_pneumonia_yolov8n.onnx}")
    private String modelPath;

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
    public DetectRawResponse detectRaw(@RequestBody RawDetectRequest req) throws Exception {
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
        return new DetectRawResponse(boxDtos, null, null);
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
            String modelKey
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