package com.allegro.dicomback.controller;

import com.allegro.dicomback.service.InferenceService;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Path;
import java.util.List;

@RestController
@RequestMapping("/api/ai")
public class InferenceController {
    private final InferenceService service;
    InferenceController(InferenceService s) { this.service = s; }

    @PostMapping("/infer")
    public List<InferenceService.BoundingBox> infer(@RequestBody InferRequest req) throws Exception {
        return service.infer(Path.of(req.dicomPath()), Path.of("models/best.onnx"));
    }
    record InferRequest(String dicomPath) {}

    @GetMapping(value = "/visualize", produces = MediaType.IMAGE_JPEG_VALUE)
    public byte[] visualize(@RequestParam String dicomPath) throws Exception {
        // 1. AI 추론 실행
        List<InferenceService.BoundingBox> boxes = service.infer(Path.of(dicomPath), Path.of("models/best.onnx"));

        // 2. DICOM 읽어서 도화지 만들기
        BufferedImage img = createDicomImage(Path.of(dicomPath));

        // 3. 그림 그릴 빨간 펜 준비
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.setStroke(new BasicStroke(3));
        g.setFont(new Font("Arial", Font.BOLD, 20));

        // 4. 종양 박스 그리기
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

        // 5. 브라우저로 전송
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "jpg", baos);
        return baos.toByteArray();
    }

    private BufferedImage createDicomImage(Path dicomPath) throws Exception {
        Attributes a;
        try (DicomInputStream dis = new DicomInputStream(dicomPath.toFile())) {
            a = dis.readDataset();
        }
        int rows = a.getInt(Tag.Rows, 0), cols = a.getInt(Tag.Columns, 0);
        byte[] raw = a.getBytes(Tag.PixelData);
        double slope = a.getDouble(Tag.RescaleSlope, 1.0);
        double intercept = a.getDouble(Tag.RescaleIntercept, 0.0);

        double WL = a.getDouble(Tag.WindowCenter, 40);
        double WW = a.getDouble(Tag.WindowWidth, 80);
        double low = WL - WW / 2;

        BufferedImage img = new BufferedImage(cols, rows, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int i = y * cols + x;
                // CT 음수 처리를 위한 short 캐스팅
                short p = (short) ((raw[i * 2] & 0xFF) | ((raw[i * 2 + 1] & 0xFF) << 8));
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