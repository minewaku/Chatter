package com.minewaku.chatter.profilenext.profile.internal.model;

import java.text.BreakIterator;
import java.util.Locale;

/** The text rules shared by display name and bio: trimming, control characters, and counting what a person sees. */
final class PerceivedText {

    /** Raw input longer than this many UTF-16 units is refused before any counting, to bound the work. */
    static final int MAX_RAW_UNITS = 1024;

    private PerceivedText() {}

    static String trim(String raw) {
        return raw.strip();
    }

    /**
     * Control characters, line and paragraph separators, and invisible format characters (zero-width space,
     * direction overrides, byte-order mark). The joiners and the tag characters that emoji sequences are built from
     * are allowed.
     */
    static boolean hasControlCharacters(String text) {
        return text.codePoints().anyMatch(PerceivedText::isControl);
    }

    private static boolean isControl(int codePoint) {
        if (Character.isISOControl(codePoint)) {
            return true;
        }
        return switch (Character.getType(codePoint)) {
            case Character.LINE_SEPARATOR, Character.PARAGRAPH_SEPARATOR -> true;
            case Character.FORMAT -> !isEmojiGlue(codePoint);
            default -> false;
        };
    }

    private static boolean isEmojiGlue(int codePoint) {
        return codePoint == 0x200C // zero width non-joiner (needed by some scripts)
                || codePoint == 0x200D // zero width joiner
                || (codePoint >= 0xE0020 && codePoint <= 0xE007F); // tag characters in flag sequences
    }

    /** Extended grapheme clusters, so a family emoji or an accented letter counts as one. */
    static int count(String text) {
        BreakIterator iterator = BreakIterator.getCharacterInstance(Locale.ROOT);
        iterator.setText(text);
        int count = 0;
        iterator.first();
        while (iterator.next() != BreakIterator.DONE) {
            count++;
        }
        return count;
    }
}
