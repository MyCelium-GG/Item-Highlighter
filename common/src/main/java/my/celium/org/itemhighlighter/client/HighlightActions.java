package my.celium.org.itemhighlighter.client;

import java.util.Map;

import my.celium.org.itemhighlighter.HighlightColor;
import my.celium.org.itemhighlighter.HighlightKeys;
import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.world.item.ItemStack;

/**
 * Shared backs for the {@code /hl} client command on every loader:
 * highlights the held item type with a custom color. Each loader delivers
 * the resulting message through its own command source. Client only.
 */
public final class HighlightActions {
    /** Outcome of a highlight action: brigadier code plus chat message. */
    public record Result(int code, Component message, boolean success) {
    }

    private HighlightActions() {
    }

    /**
     * Routes the raw {@code /hl} color-argument text to the right action.
     *
     * <p>The color argument is a greedy string (so {@code #}, {@code &} and
     * {@code §} colors parse unquoted), which means it also matches the words
     * {@code clear} and {@code list} and can shadow their sibling literals
     * depending on dispatcher tie-breaking. Routing those words here keeps
     * {@code /hl clear} and {@code /hl list} working identically on every
     * loader no matter which branch wins.
     */
    public static Result dispatchColorArgument(String rawColor) {
        if (rawColor != null) {
            String word = rawColor.trim();
            if (word.equalsIgnoreCase("clear")) {
                return clearHeld();
            }
            if (word.equalsIgnoreCase("list")) {
                return list();
            }
        }
        return highlightHeld(rawColor);
    }

    /**
     * Highlights the held item type. A {@code null} color uses the
     * configured default.
     */
    public static Result highlightHeld(String rawColor) {
        ItemStack held = heldItem();
        if (held == null) {
            return new Result(0, Component.literal("Hold an item to highlight it."), false);
        }
        int rgb;
        if (rawColor == null || rawColor.isBlank()) {
            rgb = ItemHighlighterMod.effectiveDefaultColor();
        } else {
            Integer parsed = HighlightColor.parseLenient(rawColor);
            if (parsed == null) {
                return new Result(0,
                        Component.literal(
                                "Not a color. Start with #RRGGBB or a legacy code like &a (§a works too)."),
                        false);
            }
            rgb = parsed;
        }
        String key = HighlightKeys.keyFor(held);
        if (key.isEmpty()) {
            return new Result(0, Component.literal("Could not identify the held item."), false);
        }
        HighlightStore.setColor(key, rgb);
        MutableComponent message = Component.literal("Highlighted ");
        message.append(held.getHoverName().copy());
        message.append(Component.literal(" with "));
        message.append(swatch(rgb));
        if (HighlightKeys.isExactKey(key)) {
            message.append(Component.literal(" (exact match)"));
        }
        return new Result(1, message, true);
    }

    /** Removes the held item type's highlight. */
    public static Result clearHeld() {
        ItemStack held = heldItem();
        if (held == null) {
            return new Result(0, Component.literal("Hold an item to clear its highlight."), false);
        }
        String key = HighlightKeys.keyFor(held);
        if (key.isEmpty()) {
            return new Result(0, Component.literal("Could not identify the held item."), false);
        }
        if (!HighlightStore.remove(key)) {
            return new Result(0, Component.literal("That item is not highlighted."), false);
        }
        return new Result(1,
                Component.literal("Cleared highlight for ").append(held.getHoverName().copy()), true);
    }

    /** Lists every highlighted item. */
    public static Result list() {
        Map<String, Integer> entries = HighlightStore.entries();
        if (entries.isEmpty()) {
            return new Result(1,
                    Component.literal("No highlighted items. Hold an item and run /hl #RRGGBB."), true);
        }
        MutableComponent message = Component.literal("Highlighted items:");
        for (Map.Entry<String, Integer> entry : entries.entrySet()) {
            message.append(Component.literal("\n"));
            message.append(swatch(entry.getValue()));
            message.append(Component.literal(" " + HighlightKeys.displayId(entry.getKey())));
            if (HighlightKeys.isExactKey(entry.getKey())) {
                message.append(Component.literal(" (exact)"));
            }
        }
        return new Result(1, message, true);
    }

    /** A colored swatch plus hex, e.g. {@code ■ #FF55AA}. */
    static Component swatch(int rgb) {
        MutableComponent swatch = Component.literal("■ " + HighlightColor.toHex(rgb));
        TextColor color = TextColor.fromRgb(rgb & 0xFFFFFF);
        if (color != null) {
            swatch.withStyle(Style.EMPTY.withColor(color));
        }
        return swatch;
    }

    /** The local player's held stack, or {@code null} when there is none. */
    static ItemStack heldItem() {
        try {
            Minecraft minecraft = Minecraft.getInstance();
            LocalPlayer player = minecraft.player;
            if (player == null) {
                return null;
            }
            ItemStack held = player.getMainHandItem();
            return held == null || held.isEmpty() ? null : held;
        } catch (RuntimeException e) {
            return null;
        }
    }
}
