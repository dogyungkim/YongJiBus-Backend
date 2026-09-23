package com.yongjibus.place.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;

class PlaceImageProcessorTest {
    private final PlaceImageProcessor processor = new PlaceImageProcessor();

    @Test
    void createsBoundedDisplayAndThumbnailJpegs() throws Exception {
        BufferedImage source = new BufferedImage(4_000, 2_000, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, Color.RED.getRGB());
        byte[] input = encode(source, "png");

        PlaceImageProcessor.ProcessedImages result = processor.process(
                new MockMultipartFile("images", "place.png", "image/png", input));

        BufferedImage display = ImageIO.read(new java.io.ByteArrayInputStream(result.displayBytes()));
        BufferedImage thumbnail = ImageIO.read(new java.io.ByteArrayInputStream(result.thumbnailBytes()));
        assertThat(display.getWidth()).isEqualTo(2_048);
        assertThat(display.getHeight()).isEqualTo(1_024);
        assertThat(thumbnail.getWidth()).isEqualTo(480);
        assertThat(thumbnail.getHeight()).isEqualTo(240);
        assertThat(result.displayBytes()).startsWith((byte) 0xff, (byte) 0xd8, (byte) 0xff);
        assertThat(result.thumbnailBytes()).startsWith((byte) 0xff, (byte) 0xd8, (byte) 0xff);
    }

    @Test
    void flattensTransparencyOnWhite() throws Exception {
        BufferedImage source = new BufferedImage(2, 2, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00000000);

        PlaceImageProcessor.ProcessedImages result = processor.process(
                new MockMultipartFile("images", "transparent.png", "image/png", encode(source, "png")));
        BufferedImage output = ImageIO.read(new java.io.ByteArrayInputStream(result.displayBytes()));

        assertThat(new Color(output.getRGB(0, 0))).satisfies(color -> {
            assertThat(color.getRed()).isGreaterThan(245);
            assertThat(color.getGreen()).isGreaterThan(245);
            assertThat(color.getBlue()).isGreaterThan(245);
        });
    }

    @Test
    void resizesTransparentImageBeforeFlattening() throws Exception {
        BufferedImage source = new BufferedImage(4_000, 2_000, BufferedImage.TYPE_INT_ARGB);
        source.setRGB(0, 0, 0x00000000);

        PlaceImageProcessor.ProcessedImages result = processor.process(
                new MockMultipartFile("images", "large-transparent.png", "image/png", encode(source, "png")));
        BufferedImage display = ImageIO.read(new java.io.ByteArrayInputStream(result.displayBytes()));

        assertThat(display.getWidth()).isEqualTo(2_048);
        assertThat(display.getHeight()).isEqualTo(1_024);
        assertThat(new Color(display.getRGB(0, 0))).satisfies(color -> {
            assertThat(color.getRed()).isGreaterThan(245);
            assertThat(color.getGreen()).isGreaterThan(245);
            assertThat(color.getBlue()).isGreaterThan(245);
        });
    }

    @Test
    void rejectsInvalidDimensionsBeforeDecoding() throws Exception {
        BufferedImage source = new BufferedImage(5_001, 5_000, BufferedImage.TYPE_BYTE_GRAY);

        assertThatThrownBy(() -> processor.process(
                new MockMultipartFile("images", "large.png", "image/png", encode(source, "png"))))
                .isInstanceOfSatisfying(PlaceException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PLACE_IMAGE));
    }

    @Test
    void rejectsMismatchedMimeAndSignature() {
        assertThatThrownBy(() -> processor.process(new MockMultipartFile(
                "images", "place.jpg", "image/jpeg", new byte[] {(byte) 0x89, 0x50, 0x4e, 0x47})))
                .isInstanceOfSatisfying(PlaceException.class,
                        error -> assertThat(error.getErrorCode()).isEqualTo(ErrorCode.INVALID_PLACE_IMAGE));
    }

    @Test
    void appliesJpegExifOrientation() throws Exception {
        BufferedImage source = new BufferedImage(2, 1, BufferedImage.TYPE_INT_RGB);
        source.setRGB(0, 0, Color.RED.getRGB());
        source.setRGB(1, 0, Color.BLUE.getRGB());
        byte[] jpeg = withOrientation(encode(source, "jpg"), 6);

        PlaceImageProcessor.ProcessedImages result = processor.process(
                new MockMultipartFile("images", "oriented.jpg", "image/jpeg", jpeg));
        BufferedImage output = ImageIO.read(new java.io.ByteArrayInputStream(result.displayBytes()));

        assertThat(output.getWidth()).isEqualTo(1);
        assertThat(output.getHeight()).isEqualTo(2);
    }

    @Test
    void decodesWebpWhenTheMimeAndSignatureMatch() throws Exception {
        byte[] webp = Base64.getMimeDecoder().decode("""
                UklGRqgBAABXRUJQVlA4WAoAAAAQAAAADwAADwAAQUxQSMMAAAABJ6KokSTleucYX+ffKpmImP90cY3gJjDi4Yt3MsjBEVyDKzDosHgVjnhRNcEIDAJPkqBqsFUZHNa2bUYvTsZ2PLbtd/uvKa4hov9J0f2PkPe6REkkGzolkTTzFG0Ox9PlFiD0CxS+kOGDtxoynjaCfx0pfk52CPuInrOR75lzRugygtv4zEiy90UwfSD9NheMITJWLaXWayO8XeOlWRXVnIGk2W6WdYoYMQ+KqixQNPowgt+6a1BSKbUtz+lUFAoBAAAAVlA4IL4AAACQAgCdASoQABAAAwA0JbACdDBPCIUMfAMdCCz96AD+/XSg/QKbH4r3Q3ycN/bSDK/T/zVo4u6nvclvG/SqxWOuup+XhN9BojvaW+Tv+MvxvX/hr/o/5Qns9LtmX/+qKdl/yWznhuasl7nkxvSTI4xf3Y85VSB/lU/8Ofj/b9JrA+ifvIOYZm2x1RP/dhfmsf5diuSfR7+z+r/+HR3zEo/+XM/B+vkYw73Pzx+ROaAB/ZoBSzEs3rzZe6qsAAAA
                """);

        PlaceImageProcessor.ProcessedImages result = processor.process(
                new MockMultipartFile("images", "place.webp", "image/webp", webp));

        assertThat(ImageIO.read(new java.io.ByteArrayInputStream(result.displayBytes()))).isNotNull();
    }

    private static byte[] encode(BufferedImage image, String format) throws Exception {
        try (ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            ImageIO.write(image, format, output);
            return output.toByteArray();
        }
    }

    private static byte[] withOrientation(byte[] jpeg, int orientation) {
        byte[] exif = {
                'E', 'x', 'i', 'f', 0, 0,
                'I', 'I', 42, 0, 8, 0, 0, 0,
                1, 0,
                0x12, 0x01,
                3, 0,
                1, 0, 0, 0,
                (byte) orientation, 0, 0, 0,
                0, 0, 0, 0
        };
        int length = exif.length + 2;
        byte[] result = new byte[jpeg.length + exif.length + 4];
        result[0] = (byte) 0xff;
        result[1] = (byte) 0xd8;
        result[2] = (byte) 0xff;
        result[3] = (byte) 0xe1;
        result[4] = (byte) (length >>> 8);
        result[5] = (byte) length;
        System.arraycopy(exif, 0, result, 6, exif.length);
        System.arraycopy(jpeg, 2, result, 6 + exif.length, jpeg.length - 2);
        return result;
    }
}
