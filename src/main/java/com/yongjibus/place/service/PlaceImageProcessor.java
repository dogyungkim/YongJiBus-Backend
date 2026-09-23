package com.yongjibus.place.service;

import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;

import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import com.yongjibus.global.error.code.ErrorCode;
import com.yongjibus.global.error.exception.PlaceException;

import net.coobird.thumbnailator.Thumbnails;

/** Validates an upload and creates the two JPEG variants kept by the service. */
@Component
public class PlaceImageProcessor {
    public static final long MAX_FILE_SIZE = 5L * 1024 * 1024;
    public static final int MAX_DIMENSION = 12_000;
    public static final long MAX_PIXELS = 25_000_000L;
    public static final int DISPLAY_MAX_EDGE = 2_048;
    public static final int THUMBNAIL_MAX_EDGE = 480;

    static {
        ImageIO.scanForPlugins();
    }

    public ProcessedImages process(MultipartFile file) {
        if (file == null || file.isEmpty() || file.getSize() > MAX_FILE_SIZE) {
            throw invalidImage();
        }
        try {
            return process(file.getBytes(), file.getContentType());
        } catch (IOException | RuntimeException e) {
            if (e instanceof PlaceException placeException) {
                throw placeException;
            }
            throw invalidImage();
        }
    }

    public ProcessedImages process(byte[] bytes, String contentType) {
        String format = format(contentType);
        if (bytes == null || bytes.length == 0 || bytes.length > MAX_FILE_SIZE
                || !matchesSignature(bytes, format)) {
            throw invalidImage();
        }

        BufferedImage decoded = read(bytes);
        BufferedImage oriented = applyOrientation(decoded, "jpg".equals(format) ? exifOrientation(bytes) : 1);
        BufferedImage display = flattenOnWhite(resize(oriented, DISPLAY_MAX_EDGE));
        BufferedImage thumbnail = resize(display, THUMBNAIL_MAX_EDGE);
        try {
            return new ProcessedImages(encodeJpeg(display, 0.85f), encodeJpeg(thumbnail, 0.75f));
        } catch (IOException | RuntimeException e) {
            throw invalidImage();
        }
    }

    private static BufferedImage read(byte[] bytes) {
        try (ImageInputStream input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            if (input == null) {
                throw invalidImage();
            }
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) {
                throw invalidImage();
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(input, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_DIMENSION || height > MAX_DIMENSION
                        || (long) width * height > MAX_PIXELS) {
                    throw invalidImage();
                }
                BufferedImage image = reader.read(0);
                if (image == null) {
                    throw invalidImage();
                }
                return image;
            } finally {
                reader.dispose();
            }
        } catch (PlaceException e) {
            throw e;
        } catch (IOException | RuntimeException e) {
            throw invalidImage();
        }
    }

    private static BufferedImage flattenOnWhite(BufferedImage source) {
        BufferedImage result = new BufferedImage(source.getWidth(), source.getHeight(), BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.setComposite(AlphaComposite.Src);
            graphics.setColor(Color.WHITE);
            graphics.fillRect(0, 0, result.getWidth(), result.getHeight());
            graphics.setComposite(AlphaComposite.SrcOver);
            graphics.drawImage(source, 0, 0, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static BufferedImage resize(BufferedImage source, int maxEdge) {
        int width = source.getWidth();
        int height = source.getHeight();
        int longEdge = Math.max(width, height);
        if (longEdge <= maxEdge) {
            return source;
        }
        int targetWidth = Math.max(1, (int) Math.round((double) width * maxEdge / longEdge));
        int targetHeight = Math.max(1, (int) Math.round((double) height * maxEdge / longEdge));
        try {
            return Thumbnails.of(source)
                    .size(targetWidth, targetHeight)
                    .keepAspectRatio(true)
                    .asBufferedImage();
        } catch (IOException | RuntimeException e) {
            throw invalidImage();
        }
    }

    private static byte[] encodeJpeg(BufferedImage image, float quality) throws IOException {
        ImageWriter writer = ImageIO.getImageWritersByFormatName("JPEG").hasNext()
                ? ImageIO.getImageWritersByFormatName("JPEG").next() : null;
        if (writer == null) {
            throw new IOException("JPEG writer unavailable");
        }
        try (ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                ImageOutputStream output = ImageIO.createImageOutputStream(bytes)) {
            writer.setOutput(output);
            ImageWriteParam param = writer.getDefaultWriteParam();
            param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            param.setCompressionQuality(quality);
            writer.write(null, new IIOImage(image, null, null), param);
            output.flush();
            return bytes.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private static BufferedImage applyOrientation(BufferedImage source, int orientation) {
        if (orientation == 1) {
            return source;
        }
        int width = source.getWidth();
        int height = source.getHeight();
        int outputWidth = orientation >= 5 && orientation <= 8 ? height : width;
        int outputHeight = orientation >= 5 && orientation <= 8 ? width : height;
        AffineTransform transform = new AffineTransform();
        switch (orientation) {
            case 2 -> {
                transform.translate(width, 0);
                transform.scale(-1, 1);
            }
            case 3 -> {
                transform.translate(width, height);
                transform.rotate(Math.PI);
            }
            case 4 -> {
                transform.translate(0, height);
                transform.scale(1, -1);
            }
            case 5 -> {
                transform.rotate(Math.PI / 2);
                transform.scale(1, -1);
            }
            case 6 -> {
                transform.translate(height, 0);
                transform.rotate(Math.PI / 2);
            }
            case 7 -> {
                transform.translate(height, width);
                transform.rotate(-Math.PI / 2);
                transform.scale(1, -1);
            }
            case 8 -> {
                transform.translate(0, width);
                transform.rotate(-Math.PI / 2);
            }
            default -> {
                return source;
            }
        }
        BufferedImage result = new BufferedImage(outputWidth, outputHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D graphics = result.createGraphics();
        try {
            graphics.drawImage(source, transform, null);
        } finally {
            graphics.dispose();
        }
        return result;
    }

    private static String format(String contentType) {
        String value = contentType == null ? "" : contentType.trim().toLowerCase(Locale.ROOT);
        return switch (value) {
            case "image/jpeg" -> "jpg";
            case "image/png" -> "png";
            case "image/webp" -> "webp";
            default -> throw invalidImage();
        };
    }

    private static boolean matchesSignature(byte[] value, String format) {
        return switch (format) {
            case "jpg" -> value.length >= 3
                    && unsigned(value[0]) == 0xff && unsigned(value[1]) == 0xd8 && unsigned(value[2]) == 0xff;
            case "png" -> value.length >= 8
                    && unsigned(value[0]) == 0x89 && value[1] == 0x50 && value[2] == 0x4e && value[3] == 0x47
                    && value[4] == 0x0d && value[5] == 0x0a && value[6] == 0x1a && value[7] == 0x0a;
            case "webp" -> value.length >= 12
                    && value[0] == 'R' && value[1] == 'I' && value[2] == 'F' && value[3] == 'F'
                    && value[8] == 'W' && value[9] == 'E' && value[10] == 'B' && value[11] == 'P';
            default -> false;
        };
    }

    private static int unsigned(byte value) {
        return value & 0xff;
    }

    private static int exifOrientation(byte[] bytes) {
        if (bytes.length < 4 || unsigned(bytes[0]) != 0xff || unsigned(bytes[1]) != 0xd8) {
            return 1;
        }
        int offset = 2;
        while (offset + 4 <= bytes.length) {
            if (unsigned(bytes[offset]) != 0xff) {
                return 1;
            }
            while (offset < bytes.length && unsigned(bytes[offset]) == 0xff) {
                offset++;
            }
            if (offset >= bytes.length) {
                return 1;
            }
            int marker = unsigned(bytes[offset++]);
            if (marker == 0xda || marker == 0xd9) {
                return 1;
            }
            if (offset + 2 > bytes.length) {
                return 1;
            }
            int length = unsigned(bytes[offset]) << 8 | unsigned(bytes[offset + 1]);
            if (length < 2 || offset + length > bytes.length) {
                return 1;
            }
            if (marker == 0xe1 && length >= 8
                    && bytes[offset + 2] == 'E' && bytes[offset + 3] == 'x'
                    && bytes[offset + 4] == 'i' && bytes[offset + 5] == 'f'
                    && bytes[offset + 6] == 0 && bytes[offset + 7] == 0) {
                return readTiffOrientation(bytes, offset + 8, offset + length);
            }
            offset += length;
        }
        return 1;
    }

    private static int readTiffOrientation(byte[] bytes, int base, int end) {
        if (base + 8 > end) {
            return 1;
        }
        boolean littleEndian;
        if (bytes[base] == 'I' && bytes[base + 1] == 'I') {
            littleEndian = true;
        } else if (bytes[base] == 'M' && bytes[base + 1] == 'M') {
            littleEndian = false;
        } else {
            return 1;
        }
        if (readShort(bytes, base + 2, littleEndian) != 42) {
            return 1;
        }
        long ifdOffset = readUnsignedInt(bytes, base + 4, littleEndian);
        if (ifdOffset > Integer.MAX_VALUE || base + ifdOffset + 2 > end) {
            return 1;
        }
        int ifd = (int) (base + ifdOffset);
        int entries = readShort(bytes, ifd, littleEndian);
        for (int index = 0; index < entries; index++) {
            int entry = ifd + 2 + index * 12;
            if (entry + 12 > end) {
                return 1;
            }
            if (readShort(bytes, entry, littleEndian) != 0x0112) {
                continue;
            }
            int type = readShort(bytes, entry + 2, littleEndian);
            long count = readUnsignedInt(bytes, entry + 4, littleEndian);
            if (type != 3 || count < 1) {
                return 1;
            }
            int valueOffset;
            if (count * 2 <= 4) {
                valueOffset = entry + 8;
            } else {
                long relativeOffset = readUnsignedInt(bytes, entry + 8, littleEndian);
                if (relativeOffset > Integer.MAX_VALUE || base + relativeOffset + 2 > end) {
                    return 1;
                }
                valueOffset = (int) (base + relativeOffset);
            }
            int orientation = readShort(bytes, valueOffset, littleEndian);
            return orientation >= 1 && orientation <= 8 ? orientation : 1;
        }
        return 1;
    }

    private static int readShort(byte[] bytes, int offset, boolean littleEndian) {
        if (offset < 0 || offset + 2 > bytes.length) {
            return 0;
        }
        int first = unsigned(bytes[offset]);
        int second = unsigned(bytes[offset + 1]);
        return littleEndian ? first | second << 8 : first << 8 | second;
    }

    private static long readUnsignedInt(byte[] bytes, int offset, boolean littleEndian) {
        if (offset < 0 || offset + 4 > bytes.length) {
            return Long.MAX_VALUE;
        }
        long first = unsigned(bytes[offset]);
        long second = unsigned(bytes[offset + 1]);
        long third = unsigned(bytes[offset + 2]);
        long fourth = unsigned(bytes[offset + 3]);
        return littleEndian ? first | second << 8 | third << 16 | fourth << 24
                : first << 24 | second << 16 | third << 8 | fourth;
    }

    private static PlaceException invalidImage() {
        return new PlaceException(ErrorCode.INVALID_PLACE_IMAGE);
    }

    public record ProcessedImages(byte[] displayBytes, byte[] thumbnailBytes) {
    }
}
