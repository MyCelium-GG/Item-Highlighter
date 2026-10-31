package my.celium.org.itemhighlighter;

import java.util.HashMap;
import java.util.Map;

/**
 * Parses highlight colors from user-supplied command input into packed
 * {@code 0xRRGGBB} integers.
 *
 * <p>Supported forms (all case-insensitive, surrounding whitespace ignored):
 * <ul>
 * <li>Hex {@code #RRGGBB} or {@code &#RRGGBB}, e.g. {@code #ff55aa}.</li>
 * <li>Legacy codes {@code &0}-{@code &9}, {@code &a}-{@code &f} (standard
 * Minecraft palette). The vanilla section-sign form ({@code §a}) is accepted
 * too, since that is what players copy out of vanilla contexts.</li>
 * </ul>
 *
 * <p>Pure logic - no registries, safe for headless tests.
 */
public final class HighlightColor {
    /** Legacy palette, packed {@code 0xRRGGBB}. */
    private static final Map<Character, Integer> LEGACY = new HashMap<>();

    static {
        LEGACY.put('0', 0x000000);
        LEGACY.put('1', 0x0000AA);
        LEGACY.put('2', 0x00AA00);
        LEGACY.put('3', 0x00AAAA);
        LEGACY.put('4', 0xAA0000);
        LEGACY.put('5', 0xAA00AA);
        LEGACY.put('6', 0xFFAA00);
        LEGACY.put('7', 0xAAAAAA);
        LEGACY.put('8', 0x555555);
        LEGACY.put('9', 0x5555FF);
        LEGACY.put('a', 0x55FF55);
        LEGACY.put('b', 0x55FFFF);
        LEGACY.put('c', 0xFF5555);
        LEGACY.put('d', 0xFF55FF);
        LEGACY.put('e', 0xFFFF55);
        LEGACY.put('f', 0xFFFFFF);
    }

    private HighlightColor() {
    }

    /**
     * Parses {@code raw} into packed {@code 0xRRGGBB}, or {@code null} when
     * it is not a supported color.
     */
    public static Integer parse(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.startsWith("&") || text.startsWith("§")) {
            text = text.substring(1);
            if (text.startsWith("#")) {
                return parseHex(text.substring(1));
            }
            if (text.length() == 1) {
                return LEGACY.get(Character.toLowerCase(text.charAt(0)));
            }
            return null;
        }
        if (text.startsWith("#")) {
            return parseHex(text.substring(1));
        }
        return null;
    }

    /** A named color in the menu's palette. */
    public record Preset(String name, int rgb) {
    }

    /** The menu's palette: the 16 dye colors, then brighter extras. */
    public static final Preset[] PRESETS = {
            new Preset("White", 0xF9FFFE), new Preset("Light Gray", 0x9D9D97), new Preset("Gray", 0x474F52),
            new Preset("Black", 0x1D1D21), new Preset("Brown", 0x835432), new Preset("Red", 0xB02E26),
            new Preset("Orange", 0xF9801D), new Preset("Yellow", 0xFED83D), new Preset("Lime", 0x80C71F),
            new Preset("Green", 0x5E7C16), new Preset("Cyan", 0x169C9C), new Preset("Light Blue", 0x3AB3DA),
            new Preset("Blue", 0x3C44AA), new Preset("Purple", 0x8932B8), new Preset("Magenta", 0xC74EBD),
            new Preset("Pink", 0xF38BAA),
            new Preset("Crimson", 0xFF1A3C), new Preset("Gold", 0xFFC400), new Preset("Mint", 0x5CFFB0),
            new Preset("Aqua", 0x00F0FF), new Preset("Sky", 0x7CC8FF), new Preset("Royal", 0x3D5AFE),
            new Preset("Violet", 0xB388FF), new Preset("Hot Pink", 0xFF2E9A),
    };

    /** Packed {@code 0xRRGGBB} from hue in [0,1) and saturation and value in [0,1]. */
    public static int hsv(float hue, float saturation, float value) {
        float h = (hue - (float) Math.floor(hue)) * 6f;
        float s = Math.max(0f, Math.min(1f, saturation));
        float v = Math.max(0f, Math.min(1f, value));
        int sector = (int) Math.floor(h) % 6;
        float f = h - (float) Math.floor(h);
        float p = v * (1 - s);
        float q = v * (1 - s * f);
        float t = v * (1 - s * (1 - f));
        return switch (sector) {
            case 0 -> packed(v, t, p);
            case 1 -> packed(q, v, p);
            case 2 -> packed(p, v, t);
            case 3 -> packed(p, q, v);
            case 4 -> packed(t, p, v);
            default -> packed(v, p, q);
        };
    }

    /** {hue, saturation, value} of a packed color, each in [0,1]. */
    public static float[] toHsv(int rgb) {
        float r = ((rgb >> 16) & 0xFF) / 255f;
        float g = ((rgb >> 8) & 0xFF) / 255f;
        float b = (rgb & 0xFF) / 255f;
        float max = Math.max(r, Math.max(g, b));
        float delta = max - Math.min(r, Math.min(g, b));
        float hue = 0;
        if (delta > 0) {
            if (max == r) {
                hue = ((g - b) / delta) % 6;
            } else if (max == g) {
                hue = (b - r) / delta + 2;
            } else {
                hue = (r - g) / delta + 4;
            }
            hue /= 6;
            if (hue < 0) {
                hue += 1;
            }
        }
        return new float[] {hue, max == 0 ? 0 : delta / max, max};
    }

    private static int packed(float r, float g, float b) {
        return (Math.round(r * 255) << 16) | (Math.round(g * 255) << 8) | Math.round(b * 255);
    }

    /** Formats packed {@code 0xRRGGBB} as {@code #RRGGBB}. */
    public static String toHex(int rgb) {
        return String.format("#%06X", rgb & 0xFFFFFF);
    }

    /**
     * Parses a leading color from free-form input, ignoring anything after it
     * (extra formatting codes like {@code &l}, spaces, item names, ...), so
     * {@code &a&lMy Axe} and {@code &a My Axe} both mean green. Returns
     * {@code null} when the input does not <em>start</em> with a color.
     */
    public static Integer parseLenient(String raw) {
        if (raw == null) {
            return null;
        }
        String text = raw.trim();
        if (text.length() < 2) {
            return null;
        }
        char prefix = text.charAt(0);
        if (prefix != '&' && prefix != '§' && prefix != '#') {
            return null;
        }
        String body = prefix == '#' ? text : text.substring(1);
        if (body.startsWith("#")) {
            String hex = body.substring(1);
            if (hex.length() < 6) {
                return null;
            }
            return parseHex(hex.substring(0, 6));
        }
        if (body.isEmpty()) {
            return null;
        }
        return LEGACY.get(Character.toLowerCase(body.charAt(0)));
    }

    /**
     * Nearest legacy palette code ({@code '0'}-{@code '9'}, {@code 'a'}-{
     * {@code 'f'}}) for a packed {@code 0xRRGGBB} color, by RGB distance.
     * Used for world outlines, which only support the 16 palette colors.
     */
    public static char nearestLegacyCode(int rgb) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        char best = 'f';
        int bestDist = Integer.MAX_VALUE;
        for (Map.Entry<Character, Integer> entry : LEGACY.entrySet()) {
            int palette = entry.getValue();
            int dr = r - ((palette >> 16) & 0xFF);
            int dg = g - ((palette >> 8) & 0xFF);
            int db = b - (palette & 0xFF);
            int dist = dr * dr + dg * dg + db * db;
            if (dist < bestDist) {
                bestDist = dist;
                best = entry.getKey();
            }
        }
        return best;
    }

    /** Packs 6 hex digits, or {@code null} when they are not all hex. */
    private static Integer parseHex(String hex) {
        if (hex == null || hex.length() != 6) {
            return null;
        }
        int rgb = 0;
        for (int i = 0; i < 6; i++) {
            int digit = Character.digit(hex.charAt(i), 16);
            if (digit < 0) {
                return null;
            }
            rgb = (rgb << 4) | digit;
        }
        return rgb;
    }
}
