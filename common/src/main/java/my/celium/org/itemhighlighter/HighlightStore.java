package my.celium.org.itemhighlighter;

import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import my.celium.org.Mycel;
import my.celium.org.logging.MycelLog;

/**
 * Persistent client storage for highlighted items.
 *
 * <p>Keys are item registry ids (e.g. {@code "minecraft:diamond_sword"}), so
 * every stack of that item highlights at once - or, when
 * {@code matchComponents} is enabled, exact per-stack keys (see
 * {@link HighlightKeys}) so only identical items match. Values are packed
 * {@code 0xRRGGBB} colors. Lives in
 * {@code config/itemhighlighter_data.json} (alongside Mycel's own config
 * dir), so highlights survive restarts. Small enough to rewrite wholesale on
 * every mutation. No Minecraft classes - only Gson and files - so it is
 * fully unit-testable.
 */
public final class HighlightStore {
    static final String FILE_NAME = "itemhighlighter_data.json";
    /** Maximum highlighted items kept; oldest entries are dropped first. */
    static final int MAX_ENTRIES = 256;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static final Map<String, Integer> DATA = new LinkedHashMap<>();
    private static Path loadedFrom;

    private HighlightStore() {
    }

    private static final class Root {
        Map<String, Integer> highlights = new LinkedHashMap<>();
    }

    /** Loads from Mycel's config dir (client). */
    public static synchronized void load() {
        try {
            load(Mycel.getConfigDir());
        } catch (RuntimeException e) {
            MycelLog.warnMod(ItemHighlighterMod.ID, "Could not locate config dir: {}", e.toString());
        }
    }

    /** Loads from an explicit directory (used by tests). */
    public static synchronized void load(Path dir) {
        DATA.clear();
        loadedFrom = dir == null ? null : dir.resolve(FILE_NAME);
        if (loadedFrom == null || !Files.isRegularFile(loadedFrom)) {
            return;
        }
        try (Reader reader = Files.newBufferedReader(loadedFrom, StandardCharsets.UTF_8)) {
            Root root = GSON.fromJson(reader, new TypeToken<Root>() {
            }.getType());
            if (root != null && root.highlights != null) {
                for (Map.Entry<String, Integer> entry : root.highlights.entrySet()) {
                    if (entry.getKey() != null && !entry.getKey().isBlank() && entry.getValue() != null
                            && entry.getValue() >= 0 && entry.getValue() <= 0xFFFFFF) {
                        DATA.put(entry.getKey(), entry.getValue());
                    }
                }
            }
        } catch (Exception e) {
            MycelLog.warnMod(ItemHighlighterMod.ID, "Could not read highlight data, starting fresh: {}",
                    e.toString());
            DATA.clear();
        }
    }

    /** Highlight color for an item id, or {@code null} when unhighlighted. */
    public static synchronized Integer getColor(String itemId) {
        return itemId == null ? null : DATA.get(itemId);
    }

    /** Highlights (or recolors) an item. Persists. */
    public static synchronized void setColor(String itemId, int rgb) {
        if (itemId == null || itemId.isBlank()) {
            return;
        }
        DATA.remove(itemId);
        while (DATA.size() > MAX_ENTRIES - 1) {
            var oldest = DATA.keySet().iterator();
            if (!oldest.hasNext()) {
                break;
            }
            oldest.next();
            oldest.remove();
        }
        DATA.put(itemId, rgb & 0xFFFFFF);
        save();
    }

    /** Removes an item's highlight. Returns whether one existed. Persists. */
    public static synchronized boolean remove(String itemId) {
        if (itemId == null || DATA.remove(itemId) == null) {
            return false;
        }
        save();
        return true;
    }

    /** Snapshot of item id to packed color for every highlighted item. */
    public static synchronized Map<String, Integer> entries() {
        return new LinkedHashMap<>(DATA);
    }

    /** Forgets everything (used by tests). Does not touch disk. */
    static synchronized void clearForTesting() {
        DATA.clear();
        loadedFrom = null;
    }

    private static void save() {
        if (loadedFrom == null) {
            return;
        }
        try {
            Files.createDirectories(loadedFrom.getParent());
            Root root = new Root();
            root.highlights.putAll(DATA);
            try (Writer writer = Files.newBufferedWriter(loadedFrom, StandardCharsets.UTF_8)) {
                GSON.toJson(root, writer);
            }
        } catch (Exception e) {
            MycelLog.warnMod(ItemHighlighterMod.ID, "Could not save highlight data: {}", e.toString());
        }
    }
}
