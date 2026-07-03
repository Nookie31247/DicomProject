package com.allegro.dicomback.controller;

import com.allegro.dicomback.AI.DicomPixelReader;
import com.allegro.dicomback.service.DicomService;
import com.allegro.dicomback.service.InferenceService;
import com.fasterxml.jackson.annotation.JsonProperty;
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
public class InferenceController {
    private final InferenceService service;
    private final DicomService dicomService;

    @Value("${ai.model-path:models/best.onnx}")
    private String modelPath;

    InferenceController(InferenceService s, DicomService dicomService) {
        this.service = s;
        this.dicomService = dicomService;
    }

    @PostMapping("/infer")
    public List<InferenceService.BoundingBox> infer(@RequestBody InferRequest req) throws Exception {
        return service.infer(Path.of(req.dicomPath()), Path.of("models/best.onnx"));
    }
    record InferRequest(String dicomPath) {}

    // 레거시 경로: 서버가 Orthanc에서 원본 DICOM byte[]를 다시 받아 직접 디코딩(dcm4che-imageio-opencv 필요)
    @GetMapping("/series/{seriesKey}/instances/{instanceId}/detect")
    public List<BoxDto> detect(@PathVariable Long seriesKey, @PathVariable String instanceId) throws Exception {
        byte[] dicomBytes = dicomService.getInstanceBytes(seriesKey, instanceId);
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

    // 현재 뷰어가 실제로 호출하는 경로: 프론트(cornerstone)가 화면에 띄우면서 이미 압축을 풀어놓은 픽셀 배열을 그대로 받는다. 서버는 DICOM을 다시 디코딩하지 않으므로 OpenCV/Weasis 네이티브
    @PostMapping("/detect-raw")
    public List<BoxDto> detectRaw(@RequestBody RawDetectRequest req) throws Exception {
        byte[] pixelBytes = java.util.Base64.getDecoder().decode(req.pixelDataBase64());
        List<InferenceService.BoundingBox> boxes = service.inferOnRawPixels(
                req.rows(), req.cols(),
                req.windowCenter(), req.windowWidth(),
                req.slope(), req.intercept(),
                req.signed(), pixelBytes, Path.of(modelPath));

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

    // 프론트 cornerstone image 객체에서 그대로 뽑아 보낸다.
    // windowCenter/windowWidth는 nullable(Double)이다 - 프론트는  데이터셋에(0028,1050)/(0028,1051) 태그의 유무 판단있 있으면 그 값을 그대로 보내고
    // 없으면 null을 보낸다. 서버는 null일 때만 Preprocessor.preprocessRaw의 백분위수 fallback을 쓴다.
    public record RawDetectRequest(
            int rows,
            int cols,
            Double windowCenter,
            Double windowWidth,
            double slope,
            double intercept,
            boolean signed,
            String pixelDataBase64
    ) {}

    public record BoxDto(
            @JsonProperty("x") float x,
            @JsonProperty("y") float y,
            @JsonProperty("width") float width,
            @JsonProperty("height") float height,
            @JsonProperty("confidence") float confidence
    ) {}

    @GetMapping(value = "/visualize", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] visualize(@RequestParam String dicomPath) throws Exception {
        List<InferenceService.BoundingBox> boxes = service.infer(Path.of(dicomPath), Path.of("models/best.onnx"));

        BufferedImage img = createDicomImage(Path.of(dicomPath));

        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));
        g.setFont(new Font("Arial", Font.BOLD, 20));

        for (var b : boxes) {
            double scaleX = (double) img.getWidth() / 640.0;
            double scaleY = (double) img.getHeight() / 640.0;

            int x = (int) (b.x_min() * scaleX);
            int y = (int) (b.y_min() * scaleY);
            int w = (int) ((b.x_max() - b.x_min()) * scaleX);
            int h = (int) ((b.y_max() - b.y_min()) * scaleY);

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