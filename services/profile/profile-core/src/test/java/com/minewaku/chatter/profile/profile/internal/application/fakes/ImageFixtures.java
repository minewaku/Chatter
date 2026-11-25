package com.minewaku.chatter.profile.profile.internal.application.fakes;

import java.nio.charset.StandardCharsets;

/** Smallest byte arrays that look like each image format to a header reader: enough for a sniffer, not for a decoder. */
public final class ImageFixtures {

    private ImageFixtures() {}

    public static byte[] png(int width, int height) {
        byte[] b = new byte[33];
        byte[] signature = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A};
        System.arraycopy(signature, 0, b, 0, 8);
        b[11] = 13; // IHDR length
        System.arraycopy("IHDR".getBytes(StandardCharsets.US_ASCII), 0, b, 12, 4);
        putBigEndian(b, 16, width);
        putBigEndian(b, 20, height);
        b[24] = 8; // bit depth
        b[25] = 2; // colour type
        return b;
    }

    /** A JPEG whose start-of-frame comes after an APP1 segment of {@code metadataBytes} bytes. */
    public static byte[] jpeg(int width, int height, int metadataBytes) {
        int app1Length = metadataBytes + 2;
        byte[] b = new byte[2 + 2 + app1Length + 2 + 8 + 2];
        int p = 0;
        b[p++] = (byte) 0xFF;
        b[p++] = (byte) 0xD8;
        b[p++] = (byte) 0xFF;
        b[p++] = (byte) 0xE1;
        b[p++] = (byte) (app1Length >> 8);
        b[p++] = (byte) app1Length;
        p += metadataBytes;
        b[p++] = (byte) 0xFF;
        b[p++] = (byte) 0xC0;
        b[p++] = 0;
        b[p++] = 17;
        b[p++] = 8; // precision
        b[p++] = (byte) (height >> 8);
        b[p++] = (byte) height;
        b[p++] = (byte) (width >> 8);
        b[p++] = (byte) width;
        return b;
    }

    public static byte[] jpeg(int width, int height) {
        return jpeg(width, height, 10);
    }

    /** Lossy WebP ({@code VP8 }). */
    public static byte[] webpLossy(int width, int height) {
        byte[] b = webpHeader("VP8 ", 40);
        b[23] = (byte) 0x9D;
        b[24] = 0x01;
        b[25] = 0x2A;
        b[26] = (byte) width;
        b[27] = (byte) (width >> 8);
        b[28] = (byte) height;
        b[29] = (byte) (height >> 8);
        return b;
    }

    /** Lossless WebP ({@code VP8L}). */
    public static byte[] webpLossless(int width, int height) {
        byte[] b = webpHeader("VP8L", 40);
        b[20] = 0x2F;
        long bits = (width - 1L) | ((height - 1L) << 14);
        b[21] = (byte) bits;
        b[22] = (byte) (bits >> 8);
        b[23] = (byte) (bits >> 16);
        b[24] = (byte) (bits >> 24);
        return b;
    }

    /** Extended WebP ({@code VP8X}). */
    public static byte[] webpExtended(int width, int height) {
        byte[] b = webpHeader("VP8X", 40);
        int w = width - 1;
        int h = height - 1;
        b[24] = (byte) w;
        b[25] = (byte) (w >> 8);
        b[26] = (byte) (w >> 16);
        b[27] = (byte) h;
        b[28] = (byte) (h >> 8);
        b[29] = (byte) (h >> 16);
        return b;
    }

    /** Same bytes padded with zeros to {@code length}, to make a file of a chosen size. */
    public static byte[] padded(byte[] image, int length) {
        byte[] b = new byte[Math.max(length, image.length)];
        System.arraycopy(image, 0, b, 0, image.length);
        return b;
    }

    private static byte[] webpHeader(String chunk, int length) {
        byte[] b = new byte[length];
        System.arraycopy("RIFF".getBytes(StandardCharsets.US_ASCII), 0, b, 0, 4);
        System.arraycopy("WEBP".getBytes(StandardCharsets.US_ASCII), 0, b, 8, 4);
        System.arraycopy(chunk.getBytes(StandardCharsets.US_ASCII), 0, b, 12, 4);
        return b;
    }

    private static void putBigEndian(byte[] b, int offset, int value) {
        b[offset] = (byte) (value >> 24);
        b[offset + 1] = (byte) (value >> 16);
        b[offset + 2] = (byte) (value >> 8);
        b[offset + 3] = (byte) value;
    }
}
