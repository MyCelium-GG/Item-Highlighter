package my.celium.org.itemhighlighter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;

/** {@link HighlightColor} accepts hex and legacy codes, rejects the rest. */
class HighlightColorTest {
    @Test
    void hexForms() {
        assertEquals(0xFF55AA, HighlightColor.parse("#ff55aa"));
        assertEquals(0xFF55AA, HighlightColor.parse("&#FF55AA"));
        assertEquals(0xFFFFFF, HighlightColor.parse("  #ffffff  "));
    }

    @Test
    void legacyForms() {
        assertEquals(0x55FF55, HighlightColor.parse("&a"));
        assertEquals(0x55FF55, HighlightColor.parse("&A"));
        assertEquals(0x000000, HighlightColor.parse("&0"));
        assertEquals(0xFFFFFF, HighlightColor.parse("&f"));
    }

    @Test
    void sectionSignForms() {
        // Vanilla's own legacy prefix must work too.
        assertEquals(0x55FF55, HighlightColor.parse("§a"));
        assertEquals(0x55FF55, HighlightColor.parse("§A"));
        assertEquals(0x000000, HighlightColor.parse("§0"));
        assertEquals(0xFFFFFF, HighlightColor.parse("§f"));
        assertEquals(0xFF55AA, HighlightColor.parse("§#FF55AA"));
    }

    @Test
    void lenientLeadingColor() {
        // Extra formatting codes, spaces and names after the color are ignored.
        assertEquals(0x55FF55, HighlightColor.parseLenient("&a&lMy Axe"));
        assertEquals(0x55FF55, HighlightColor.parseLenient("&a My Axe"));
        assertEquals(0x55FF55, HighlightColor.parseLenient("&A&LMy Axe"));
        assertEquals(0x55FF55, HighlightColor.parseLenient("§a"));
        assertEquals(0xFF55AA, HighlightColor.parseLenient("#ff55aa and more"));
        assertEquals(0xFF55AA, HighlightColor.parseLenient("&#FF55AA whatever"));
        assertEquals(0x55FF55, HighlightColor.parseLenient("&ab"));
    }

    @Test
    void lenientRejectsNoLeadingColor() {
        assertNull(HighlightColor.parseLenient(null));
        assertNull(HighlightColor.parseLenient(""));
        assertNull(HighlightColor.parseLenient("   "));
        assertNull(HighlightColor.parseLenient("My Axe"));
        assertNull(HighlightColor.parseLenient("&k"));
        assertNull(HighlightColor.parseLenient("§k"));
        assertNull(HighlightColor.parseLenient("#fff"));
        assertNull(HighlightColor.parseLenient("&#12345"));
        assertNull(HighlightColor.parseLenient("clear"));
        assertNull(HighlightColor.parseLenient("list"));
        assertNull(HighlightColor.parseLenient("ff55aa"));
    }

    @Test
    void rejectsJunk() {
        assertNull(HighlightColor.parse(null));
        assertNull(HighlightColor.parse(""));
        assertNull(HighlightColor.parse("   "));
        assertNull(HighlightColor.parse("red"));
        assertNull(HighlightColor.parse("#fff"));
        assertNull(HighlightColor.parse("#gggggg"));
        assertNull(HighlightColor.parse("&"));
        assertNull(HighlightColor.parse("§"));
        assertNull(HighlightColor.parse("&#12345"));
        assertNull(HighlightColor.parse("&k"));
        assertNull(HighlightColor.parse("&ab"));
        assertNull(HighlightColor.parse("ff55aa"));
    }

    @Test
    void roundTrip() {
        assertEquals("#FF55AA", HighlightColor.toHex(0xFF55AA));
        assertEquals(Integer.valueOf(0xFF55AA), HighlightColor.parse(HighlightColor.toHex(0xFF55AA)));
    }

    @Test
    void nearestPalette() {
        assertEquals('a', HighlightColor.nearestLegacyCode(0x55FF55));
        assertEquals('c', HighlightColor.nearestLegacyCode(0xFF5555));
        assertEquals('0', HighlightColor.nearestLegacyCode(0x000000));
        assertEquals('f', HighlightColor.nearestLegacyCode(0xFFFFFF));
        // Near-gold maps to gold, near-cyan to aqua.
        assertEquals('6', HighlightColor.nearestLegacyCode(0xFFAA00));
        assertEquals('b', HighlightColor.nearestLegacyCode(0x55FFFF));
    }

    @Test
    void hsvRoundTrips() {
        for (HighlightColor.Preset preset : HighlightColor.PRESETS) {
            float[] hsv = HighlightColor.toHsv(preset.rgb());
            int back = HighlightColor.hsv(hsv[0], hsv[1], hsv[2]);
            for (int shift = 0; shift <= 16; shift += 8) {
                int expected = (preset.rgb() >> shift) & 0xFF;
                int actual = (back >> shift) & 0xFF;
                assertEquals(expected, actual, 1, preset.name());
            }
        }
    }
}
