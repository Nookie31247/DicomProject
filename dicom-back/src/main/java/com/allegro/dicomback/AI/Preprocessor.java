package com.allegro.dicomback.AI;

import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.io.DicomInputStream;
import org.springframework.stereotype.Component;

import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.io.ByteArrayInputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.ShortBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

@Component
public class Preprocessor {
    // originalRows/originalCols:  640x640 모델 좌표를 원본 이미지 좌표로 되돌릴 때 필요
    public record Tensor(float[] data, long[] shape, int originalRows, int originalCols, float scale, int padX, int padY) {
    }

    public Tensor preprocess(Path dicomPath, int size) throws Exception {
        byte[] dicomBytes = Files.readAllBytes(dicomPath);
        return preprocess(dicomBytes, size);
    }

    // Orthanc에서 바로 받은 byte[]로 전처리 (압축 DICOM이면 DicomPixelReader/OpenCV 코덱이 필요)
    public Tensor preprocess(byte[] dicomBytes, int size) throws Exception {
        Attributes a;
        try (DicomInputStream dis = new DicomInputStream(new ByteArrayInputStream(dicomBytes))) {
            a = dis.readDataset();
        }
        return preprocess(a, dicomBytes, size);
    }

    private Tensor preprocess(Attributes a, byte[] dicomBytes, int size) throws Exception {
        int rows = a.getInt(Tag.Rows, 0), cols = a.getInt(Tag.Columns, 0);
        double slope = a.getDouble(Tag.RescaleSlope, 1.0);
        double intercept = a.getDouble(Tag.RescaleIntercept, 0.0);
        double WL = a.getDouble(Tag.WindowCenter, 2047);
        double WW = a.getDouble(Tag.WindowWidth, 4096);

        BufferedImage decoded = DicomPixelReader.decode(dicomBytes);
        Raster raster = decoded.getRaster();

        float[] windowed = window(rows, cols, slope, intercept, WL, WW,
                (x, y) -> raster.getSample(x, y, 0));

        return buildTensor(windowed, rows, cols, size);
    }

    // 프론트(cornerstone)가 화면에 띄우면서 이미 압축을 풀어놓은 픽셀 배열을 그대로 받아서 전처리.
    // 여기서는 DICOM 압축 해제를 다시 할 필요가 없으므로 DicomPixelReader/dcm4che-imageio-opencv가
    // 전혀 필요 없다 (뷰어에서 오는 AI 판독 요청은 전부 이 경로를 탄다).
    //
    // windowCenter/windowWidth는 nullable(Double)로 받는다: DICOM에 실제 (0028,1050)/(0028,1051)
    // 태그가 있는 이미지는 그 값을 그대로 신뢰해서 쓰고(누군가 임상적으로 의미있게 정해둔 값이니까),
    // 진짜로 태그가 없는 이미지만 픽셀 분포(상하위 0.5% 제외 백분위수)로 대신 계산한다.
    public Tensor preprocessRaw(int rows, int cols, Double windowCenter, Double windowWidth,
                                double slope, double intercept, boolean signed,
                                byte[] pixelBytes, int size) {
        // 프론트(JS 타입드 배열)와 DICOM 둘 다 기본이 little-endian이라 별도 변환 없이 그대로 읽으면 된다.
//        ShortBuffer sb = ByteBuffer.wrap(pixelBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
//        PixelSampler sampler = (x, y) -> {
//            short raw = sb.get(y * cols + x);
//            return signed ? raw : (raw & 0xFFFF);
//        };
        PixelSampler sampler = buildSampler(pixelBytes, rows, cols, signed);

        double wc, ww;
        if (windowCenter != null && windowWidth != null) {
            wc = windowCenter;
            ww = windowWidth;
        } else {
            double[] percentileWindow = computePercentileWindow(rows, cols, slope, intercept, sampler);
            wc = percentileWindow[0];
            ww = percentileWindow[1];
        }

        float[] windowed = window(rows, cols, slope, intercept, wc, ww, sampler);
        return buildTensor(windowed, rows, cols, size);
    }

    // min/max를 쓰면 노이즈/아티팩트 픽셀 하나에도 전체 대비가 뭉개질 수 있어서 WC/WW 태그가 진짜로 없는 이미지에 한해서만 쓰이는 fallback.
    // (예: 튀는 값 하나 때문에 windowWidth가 확 늘어나 진짜 조직 신호가 0~1 중 아주 작은구간으로 쪼그라듦), 상하위 0.5%를 잘라낸 백분위수로 계산한다.
    private double[] computePercentileWindow(int rows, int cols, double slope, double intercept, PixelSampler sampler) {
        double[] values = new double[rows * cols];
        int idx = 0;
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                values[idx++] = sampler.sample(x, y) * slope + intercept;
            }
        }
        double[] sorted = values.clone();
        Arrays.sort(sorted);
        int n = sorted.length;
        double low = sorted[(int) Math.max(0, n * 0.005)];
        double high = sorted[(int) Math.min(n - 1, n * 0.995)];

        double windowWidth = (high - low) <= 0 ? 1 : (high - low); // 완전 단색 이미지 방어
        double windowCenter = (high + low) / 2;
        return new double[]{windowCenter, windowWidth};
    }

    @FunctionalInterface
    private interface PixelSampler {
        int sample(int x, int y);
    }

    // HU 변환(slope/intercept) +WW/WL을 0~1 사이로 정규화
    private float[] window(int rows, int cols, double slope, double intercept,
                            double windowCenter, double windowWidth, PixelSampler sampler) {
        double low = windowCenter - windowWidth / 2;
        float[] windowed = new float[rows * cols];
        for (int y = 0; y < rows; y++) {
            for (int x = 0; x < cols; x++) {
                int p = sampler.sample(x, y);
                double hu = p * slope + intercept;
                windowed[y * cols + x] = (float) Math.min(1, Math.max(0, (hu - low) / windowWidth));
            }
        }
        return windowed;
    }

    // ③ ONNX 요구 규격에 맞게 Resize(Nearest Neighbor) 및 3채널(RGB) 복제
    // 목표: [1(배치), 3(채널), size(640), size(640)]
    private Tensor buildTensor(float[] windowed, int rows, int cols, int size) {
        float scale= (float) Math.min((double) size/ rows,  (double) size / cols);
        int scaledRows = Math.round(rows * scale);
        int scaledCols = Math.round(cols * scale);

        //가운데 정렬
        int padY = (size - scaledRows) / 2;
        int padX = (size - scaledCols) / 2;

        float[] resizedAndRGB = new float[3 * size * size];
        float padValue = 114f /255f;  //Ultralytics 기본 letterbox 패딩 색(회색)과 동일하게 맞춤
        Arrays.fill(resizedAndRGB, padValue);

        int channelOffset = size * size;

        for (int y = 0; y < scaledRows; y++) {
            for (int x = 0; x < scaledCols; x++) {
                int srcY= Math.min((int) (y/scale), rows-1);
                int srcX= Math.min((int) (x/scale), cols-1);

                float pixelValue = windowed[srcY * cols + srcX];
                int dstX= x+padX;
                int dstY= y+padY;

                resizedAndRGB[0 * channelOffset + dstY * size + dstX] = pixelValue;
                resizedAndRGB[1 * channelOffset + dstY * size + dstX] = pixelValue;
                resizedAndRGB[2 * channelOffset + dstY * size + dstX] = pixelValue;
            }
        }

        // Tensor Shape를 [1, 3, 640, 640]으로 명시하고, 원본 크기도 같이 반환
        return new Tensor(resizedAndRGB, new long[]{1, 3, size, size}, rows, cols,scale,padX,padY);
    }
    private PixelSampler buildSampler(byte[] pixelBytes, int rows, int cols, boolean signed) {
        int numPixels = rows * cols;
        if (pixelBytes.length == numPixels) {
            // 8bit (CR)
            return (x, y) -> pixelBytes[y * cols + x] & 0xFF;
        } else {
            ShortBuffer sb = ByteBuffer.wrap(pixelBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer();
            return (x, y) -> {
                short raw = sb.get(y * cols + x);
                return signed ? raw : (raw & 0xFFFF);
            };
        }
    }
}