package my.celium.org.itemhighlighter;

import my.celium.org.stats.ModStats;

/**
 * Anonymous stats for the FastStats project {@code item-highlighter}, sent through Mycel under its
 * consent: how many and which items get highlighted (item ids only), how the menu and commands get used, and the settings.
 */
public final class HighlightStats {
    private static final String TOKEN = "7208f8c554f77ca6bfc730f8d9a5a49e";

    private static volatile ModStats stats;

    private HighlightStats() {
    }

    static void start() {
        stats = ModStats.builder(ItemHighlighterMod.META, TOKEN)
                .number("highlighted_items", () -> HighlightStore.entries().size())
                .stringArray("highlighted_item_ids", HighlightStats::itemIds)
                .bool("enabled", () -> ItemHighlighterMod.enabled.get())
                .bool("show_tooltip_line", () -> ItemHighlighterMod.showTooltipLine.get())
                .bool("hotbar_highlight", () -> ItemHighlighterMod.hotbarHighlight.get())
                .bool("world_glow", () -> ItemHighlighterMod.worldGlow.get())
                .bool("match_components", () -> ItemHighlighterMod.matchComponents.get())
                .errors("my.celium.org.itemhighlighter.")
                .start();
    }

    /** Counts a feature use (sent as {@code usage}). */
    public static void count(String what) {
        ModStats current = stats;
        if (current != null) {
            current.count(what);
        }
    }

    /** Counts {@code key} in the counter map {@code id}. */
    public static void count(String id, String key) {
        ModStats current = stats;
        if (current != null && key != null) {
            current.count(id, key.startsWith("minecraft:") ? key.substring(10) : key);
        }
    }

    /** The highlighted items' ids without component data, like {@code diamond_pickaxe}; at most 100. */
    private static String[] itemIds() {
        java.util.Set<String> ids = new java.util.TreeSet<>();
        for (String key : HighlightStore.entries().keySet()) {
            String id = key.split("[{\\[|]", 2)[0];
            ids.add(id.startsWith("minecraft:") ? id.substring(10) : id);
            if (ids.size() >= 100) {
                break;
            }
        }
        return ids.isEmpty() ? null : ids.toArray(String[]::new);
    }
}
