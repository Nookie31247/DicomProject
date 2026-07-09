package com.allegro.dicomback.AI;

import javax.imageio.ImageIO; //dcm4che-imageio가 클래스패스에 있으면 ImageIO가 DICOM용 리더를 자동으로 찾아서 압축을 풀어준다.
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.util.Iterator;

/**
 * DICOM PixelData를 디코딩하기 위한 유틸리티입니다.
 * 압축 여부(JPEG, JPEG2000, RLE 등)에 상관없이 DICOM PixelData를 디코딩해주는 공용 유틸리티.
 */
public class DicomPixelReader {
    /**
     * 주어진 DICOM 바이트 배열을 BufferedImage로 디코딩합니다.
     *
     * @param dicomBytes DICOM 데이터 바이트
     * @return 디코딩된 BufferedImage
     * @throws Exception 디코딩 실패 시
     */
    public static BufferedImage decode(byte[] dicomBytes) throws Exception {
        try (ImageInputStream iis = ImageIO.createImageInputStream(new ByteArrayInputStream(dicomBytes))) {
            Iterator<ImageReader> readers = ImageIO.getImageReaders(iis);
            if (!readers.hasNext()) {
                throw new IllegalStateException("No reader found");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(iis);
                return reader.read(0);
            } finally {
                reader.dispose();
            }
        }
    }
}