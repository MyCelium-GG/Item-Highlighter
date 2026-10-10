package my.celium.org.itemhighlighter.client;

import my.celium.org.itemhighlighter.HighlightKeys;
import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Tooltip helpers shared by every loader's tooltip hook: decides whether a
 * stack gets the highlight line and builds it. Never throws.
 */
public final class TooltipLines {
    static final String LABEL = "★ Highlighted";

    private TooltipLines() {
    }

    /** Whether this stack earns the colored tooltip line right now. */
    public static boolean applies(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return false;
        }
        if (!ItemHighlighterMod.isModEnabled()) {
            return false;
        }
        try {
            if (!ItemHighlighterMod.showTooltipLine.get()) {
                return false;
            }
        } catch (RuntimeException e) {
            // Fall through with the line shown; a broken config must not hide data.
        }
        return HighlightStore.getColor(HighlightKeys.keyFor(stack)) != null;
    }

    /** The colored line for a stack, or {@code null} when none applies. */
    public static Component lineFor(ItemStack stack) {
        Integer color = HighlightStore.getColor(HighlightKeys.keyFor(stack));
        if (color == null) {
            return null;
        }
        return line(color);
    }

    /** A {@code ★ Highlighted} line in the given packed color. */
    public static Component line(int rgb) {
        MutableComponent out = Component.literal(LABEL);
        TextColor color = TextColor.fromRgb(rgb & 0xFFFFFF);
        if (color != null) {
            out.withStyle(Style.EMPTY.withColor(color));
        }
        return out;
    }

    /** Registry id of a stack's item (e.g. {@code "minecraft:diamond_sword"}). */
    public static String itemId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return key == null ? "" : key.toString();
        } catch (RuntimeException e) {
            return "";
        }
    }
}
