package com.minewaku.chatter.profilenext.profile.internal.application;

import static org.assertj.core.api.Assertions.assertThat;

import com.minewaku.chatter.profilenext.profile.internal.application.command.ImageSniffer;
import com.minewaku.chatter.profilenext.profile.internal.application.command.SniffedImage;
import com.minewaku.chatter.profilenext.profile.internal.application.fakes.ImageFixtures;
import com.minewaku.chatter.profilenext.profile.internal.model.ImageDimensions;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ImageSnifferTest {

    private static Optional<SniffedImage> sniff(byte[] bytes) {
        return ImageSniffer.sniff(bytes, bytes.length);
    }

    @Test
    void readsAPng() {
        assertThat(sniff(ImageFixtures.png(640, 480)))
                .contains(new SniffedImage("png", "image/png", new ImageDimensions(640, 480)));
    }

    @Test
    void readsAJpegAfterAMetadataSegment() {
        assertThat(sniff(ImageFixtures.jpeg(800, 600, 200)))
                .contains(new SniffedImage("jpg", "image/jpeg", new ImageDimensions(800, 600)));
    }

    @Test
    void readsEachKindOfWebp() {
        ImageDimensions expected = new ImageDimensions(1000, 1000);
        assertThat(sniff(ImageFixtures.webpLossy(1000, 1000)))
                .contains(new SniffedImage("webp", "image/webp", expected));
        assertThat(sniff(ImageFixtures.webpLossless(1000, 1000)))
                .contains(new SniffedImage("webp", "image/webp", expected));
        assertThat(sniff(ImageFixtures.webpExtended(1000, 1000)))
                .contains(new SniffedImage("webp", "image/webp", expected));
    }

    @Test
    void readsLargeWebpDimensions() {
        assertThat(sniff(ImageFixtures.webpExtended(4096, 3000)).orElseThrow().dimensions())
                .isEqualTo(new ImageDimensions(4096, 3000));
        assertThat(sniff(ImageFixtures.webpLossless(16384, 1)).orElseThrow().dimensions())
                .isEqualTo(new ImageDimensions(16384, 1));
    }

    @Test
    void anEmptyOrUnknownFileIsNotAnImage() {
        assertThat(sniff(new byte[0])).isEmpty();
        assertThat(ImageSniffer.sniff(null, 10)).isEmpty();
        assertThat(sniff("<html><body>hi</body></html>".getBytes(StandardCharsets.UTF_8))).isEmpty();
        assertThat(sniff("<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"10\" height=\"10\"/>"
                        .getBytes(StandardCharsets.UTF_8)))
                .isEmpty();
        assertThat(sniff("GIF89a\u0010\u0000\u0010\u0000".getBytes(StandardCharsets.ISO_8859_1))).isEmpty();
        assertThat(sniff(new byte[200])).isEmpty();
    }

    @Test
    void aTruncatedHeaderIsNotAnImage() {
        assertThat(sniff(Arrays.copyOf(ImageFixtures.png(10, 10), 20))).isEmpty();
        assertThat(sniff(Arrays.copyOf(ImageFixtures.webpLossy(10, 10), 25))).isEmpty();
        assertThat(sniff(Arrays.copyOf(ImageFixtures.jpeg(10, 10, 50), 30))).isEmpty();
    }

    @Test
    void aJpegWithNoFrameHeaderInTheBytesGivenIsNotAnImage() {
        // 64 KiB of metadata pushes the frame header beyond a head that is only 1 KiB long
        byte[] big = ImageFixtures.jpeg(10, 10, 60_000);
        assertThat(sniff(Arrays.copyOf(big, 1024))).isEmpty();
        assertThat(sniff(big)).isPresent();
    }

    @Test
    void aZeroSideIsNotAnImage() {
        assertThat(sniff(ImageFixtures.png(0, 10))).isEmpty();
        assertThat(sniff(ImageFixtures.jpeg(10, 0))).isEmpty();
    }

    @Test
    void aWrongStartCodeInALossyWebpIsNotAnImage() {
        byte[] bytes = ImageFixtures.webpLossy(10, 10);
        bytes[23] = 0;
        assertThat(sniff(bytes)).isEmpty();
    }

    @Test
    void aPngHeaderFollowedByGarbageStillReportsItsDeclaredSizeAndLeavesTheVerdictToThePolicy() {
        byte[] bytes = ImageFixtures.padded(ImageFixtures.png(50, 50), 4000);
        assertThat(sniff(bytes).orElseThrow().dimensions()).isEqualTo(new ImageDimensions(50, 50));
    }
}
