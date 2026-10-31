package my.celium.org.itemhighlighter;

import my.celium.org.Mycel;
import my.celium.org.config.ConfigBuilder;
import my.celium.org.config.ConfigValue;
import my.celium.org.config.MycelConfig;
import my.celium.org.metadata.ModMetadata;
import my.celium.org.util.TextUtil;

/**
 * ItemHighlighter: marks items with a custom color wherever you see them: a
 * glow behind them in the hotbar, inventories and containers, a glowing outline
 * in the world and a colored tooltip line.
 *
 * <p>Client-side only. {@link #init()} registers metadata and config with
 * Mycel; each loader's <em>client</em> entrypoint then (a) registers the
 * {@code /hl} command and the menu key, (b) draws highlights through its native HUD
 * and screen hooks, and (c) appends the highlight line through its native
 * tooltip hook. Dedicated servers never touch client classes.
 */
public final class ItemHighlighterMod {
    public static final String ID = "itemhighlighter";

    public static final ModMetadata META = ModMetadata.builder(ID, "ItemHighlighter", "1.2.0")
            .author("MyCelium")
            .website("https://www.curseforge.com/minecraft/mc-mods/item-highlighter-mod")
            .updateUrl("https://wiki.mycelium-gg.org/updates/itemhighlighter.json")
            .build();

    /** Default gold highlight, as packed {@code 0xRRGGBB}. */
    public static final int DEFAULT_COLOR_FALLBACK = TextUtil.WARN;

    public static ConfigValue<Boolean> enabled;
    public static ConfigValue<Integer> defaultColor;
    public static ConfigValue<Boolean> showTooltipLine;
    public static ConfigValue<Boolean> hotbarHighlight;
    public static ConfigValue<Boolean> worldGlow;
    public static ConfigValue<Boolean> matchComponents;
    public static MycelConfig config;

    private ItemHighlighterMod() {
    }

    /** Called once from each loader's entrypoint. */
    public static void init() {
        Mycel.initialize();
        Mycel.registerMod(META);

        ConfigBuilder builder = Mycel.config(META);
        enabled = builder.booleanValue("enabled", true,
                "Whether item highlights are shown.");
        defaultColor = builder.intValue("defaultColor", DEFAULT_COLOR_FALLBACK, 0x000000, 0xFFFFFF,
                "Default highlight color (hex RGB) for items you highlight before picking one.");
        showTooltipLine = builder.booleanValue("showTooltipLine", true,
                "Append a colored \"★ Highlighted\" line to highlighted items' tooltips.");
        hotbarHighlight = builder.booleanValue("hotbarHighlight", true,
                "Show a colored glow behind highlighted items in the hotbar, offhand, inventories and containers.");
        worldGlow = builder.booleanValue("worldGlow", true,
                "Give highlighted dropped items and item frames a glowing outline in the world.");
        matchComponents = builder.booleanValue("matchComponents", false,
                "When true, only stacks identical to the highlighted one match (same enchantments, custom name, damage and other components). When false, every item of the same type highlights.");
        config = builder.build();
    }

    public static boolean isModEnabled() {
        return Mycel.isEnabled(ID) && enabled.get();
    }

    /** Reads the default color defensively clamped to its documented range. */
    public static int effectiveDefaultColor() {
        try {
            return Math.max(0x000000, Math.min(0xFFFFFF, defaultColor.get()));
        } catch (RuntimeException e) {
            return DEFAULT_COLOR_FALLBACK;
        }
    }
}
