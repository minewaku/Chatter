package com.minewaku.chatter.profile.profile.internal.application.command;

import com.minewaku.chatter.profile.profile.internal.model.ImageDimensions;
import java.util.Optional;

/**
 * Tells what a stored file really is from its first bytes: PNG, JPEG or WebP, and its width and height. It reads
 * headers only and never decodes pixels, so a file that claims to be huge costs nothing. Anything it does not recognise,
 * or whose dimensions are not inside the bytes it was given, is simply not an image as far as this service is concerned.
 */
public final class ImageSniffer {

    private static final byte[] PNG_SIGNATURE = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};

    private ImageSniffer() {}

    /** @param head the first bytes of the file (at most a few hundred KiB), not necessarily all of it */
    public static Optional<SniffedImage> sniff(byte[] head) {
        if (head == null || head.length == 0) {
            return Optional.empty();
        }
        try {
            if (startsWith(head, PNG_SIGNATURE)) {
                return png(head);
            }
            if (head.length >= 3 && (head[0] & 0xFF) == 0xFF && (head[1] & 0xFF) == 0xD8 && (head[2] & 0xFF) == 0xFF) {
                return jpeg(head);
            }
            if (head.length >= 12 && ascii(head, 0, "RIFF") && ascii(head, 8, "WEBP")) {
                return webp(head);
            }
        } catch (ArrayIndexOutOfBoundsException e) {
            return Optional.empty();
        }
        return Optional.empty();
    }

    private static Optional<SniffedImage> png(byte[] b) {
        // 8 signature bytes, then the IHDR chunk: length(4) "IHDR"(4) width(4) height(4)
        if (b.length < 24 || !ascii(b, 12, "IHDR")) {
            return Optional.empty();
        }
        return result("png", "image/png", bigEndianInt(b, 16), bigEndianInt(b, 20));
    }

    private static Optional<SniffedImage> jpeg(byte[] b) {
        int pos = 2;
        while (pos + 3 < b.length) {
            if ((b[pos] & 0xFF) != 0xFF) {
                return Optional.empty();
            }
            int marker = b[pos + 1] & 0xFF;
            if (marker == 0xFF) { // fill byte
                pos++;
                continue;
            }
            if (marker == 0x01 || (marker >= 0xD0 && marker <= 0xD9)) { // standalone markers carry no length
                pos += 2;
                continue;
            }
            int length = ((b[pos + 2] & 0xFF) << 8) | (b[pos + 3] & 0xFF);
            if (isStartOfFrame(marker)) {
                // length(2) precision(1) height(2) width(2)
                if (pos + 8 >= b.length) {
                    return Optional.empty();
                }
                int height = ((b[pos + 5] & 0xFF) << 8) | (b[pos + 6] & 0xFF);
                int width = ((b[pos + 7] & 0xFF) << 8) | (b[pos + 8] & 0xFF);
                return result("jpg", "image/jpeg", width, height);
            }
            if (marker == 0xDA || length < 2) { // start of scan before any frame header: not a usable image
                return Optional.empty();
            }
            pos += 2 + length;
        }
        return Optional.empty();
    }

    private static boolean isStartOfFrame(int marker) {
        return marker >= 0xC0 && marker <= 0xCF && marker != 0xC4 && marker != 0xC8 && marker != 0xCC;
    }

    private static Optional<SniffedImage> webp(byte[] b) {
        if (b.length < 30) {
            return Optional.empty();
        }
        if (ascii(b, 12, "VP8 ")) {
            // frame tag(3) start code 9D 01 2A, then 14-bit width and height (little endian)
            if ((b[23] & 0xFF) != 0x9D || (b[24] & 0xFF) != 0x01 || (b[25] & 0xFF) != 0x2A) {
                return Optional.empty();
            }
            int width = littleEndian16(b, 26) & 0x3FFF;
            int height = littleEndian16(b, 28) & 0x3FFF;
            return result("webp", "image/webp", width, height);
        }
        if (ascii(b, 12, "VP8L")) {
            if ((b[20] & 0xFF) != 0x2F) {
                return Optional.empty();
            }
            long bits = (b[21] & 0xFFL) | ((b[22] & 0xFFL) << 8) | ((b[23] & 0xFFL) << 16) | ((b[24] & 0xFFL) << 24);
            int width = (int) (bits & 0x3FFF) + 1;
            int height = (int) ((bits >> 14) & 0x3FFF) + 1;
            return result("webp", "image/webp", width, height);
        }
        if (ascii(b, 12, "VP8X")) {
            // flags(1) reserved(3), then canvas width-1 and height-1 as 24-bit little endian
            int width = littleEndian24(b, 24) + 1;
            int height = littleEndian24(b, 27) + 1;
            return result("webp", "image/webp", width, height);
        }
        return Optional.empty();
    }

    private static Optional<SniffedImage> result(String format, String contentType, int width, int height) {
        if (width <= 0 || height <= 0) {
            return Optional.empty();
        }
        return Optional.of(new SniffedImage(format, contentType, new ImageDimensions(width, height)));
    }

    private static boolean startsWith(byte[] bytes, byte[] prefix) {
        if (bytes.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (bytes[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean ascii(byte[] b, int offset, String text) {
        if (b.length < offset + text.length()) {
            return false;
        }
        for (int i = 0; i < text.length(); i++) {
            if (b[offset + i] != (byte) text.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    private static int bigEndianInt(byte[] b, int offset) {
        return ((b[offset] & 0xFF) << 24)
                | ((b[offset + 1] & 0xFF) << 16)
                | ((b[offset + 2] & 0xFF) << 8)
                | (b[offset + 3] & 0xFF);
    }

    private static int littleEndian16(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8);
    }

    private static int littleEndian24(byte[] b, int offset) {
        return (b[offset] & 0xFF) | ((b[offset + 1] & 0xFF) << 8) | ((b[offset + 2] & 0xFF) << 16);
    }
}
