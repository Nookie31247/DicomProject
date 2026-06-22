package com.allegro.dicomback;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Component;

import java.nio.file.Path;

@Component
public class Preprocessor {
    public record Tensor(float[] data, long[] shape) {
    }

    public Tensor preprocess(Path dicomPath, int size) throws Exception {
        Attributes a;
        try (DicomInputStream dis = new DicomInputStream(dicomPath.toFile())) {
            a = dis.readDataset();
        }
        int rows = a.getInt(Tag.Rows, 0), cols = a.getInt(Tag.Columns, 0);
        byte[] raw = a.getBytes(Tag.PixelData);
        double slope = a.getDouble(Tag.RescaleSlope, 1.0);
        double intercept = a.getDouble(Tag.RescaleIntercept, 0.0);
        double WL = a.getDouble(Tag.WindowCenter, 2047);
        double WW = a.getDouble(Tag.WindowWidth, 4096);
        double low = WL - WW / 2;

        float[] windowed = new float[rows * cols];
        for (int i = 0; i < windowed.length; i++) {
            int p = (raw[i * 2] & 0xFF) | ((raw[i * 2 + 1] & 0xFF) << 8); // Little Endian
            double hu = p * slope + intercept;
            windowed[i] = (float) Math.min(1, Math.max(0, (hu - low) / WW));
        }

        // ③ ONNX 요구 규격에 맞게 Resize(Nearest Neighbor) 및 3채널(RGB) 복제
        // 목표: [1(배치), 3(채널), size(640), size(640)]
        float[] resizedAndRGB = new float[3 * size * size];

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                // 원본 DICOM 크기에서 현재 (x, y)에 해당하는 픽셀 위치 매핑 (비율 계산)
                int srcY = (int) (y * (double) rows / size);
                int srcX = (int) (x * (double) cols / size);

                // 원본 배열 범위를 넘지 않도록 방어 코드
                srcY = Math.min(srcY, rows - 1);
                srcX = Math.min(srcX, cols - 1);

                // 윈도잉된 흑백 픽셀 값 하나 가져오기
                float pixelValue = windowed[srcY * cols + srcX];

                // NCHW 포맷으로 저장 (메모리에 RRR... GGG... BBB... 순서로 적재)
                int channelOffset = size * size;

                // R 채널 (인덱스 0)
                resizedAndRGB[0 * channelOffset + (y * size) + x] = pixelValue;
                // G 채널 (인덱스 1)
                resizedAndRGB[1 * channelOffset + (y * size) + x] = pixelValue;
                // B 채널 (인덱스 2)
                resizedAndRGB[2 * channelOffset + (y * size) + x] = pixelValue;
            }
        }

        // Tensor Shape를 [1, 3, 640, 640]으로 명시하여 반환
        return new Tensor(resizedAndRGB, new long[]{1, 3, size, size});
    }
}