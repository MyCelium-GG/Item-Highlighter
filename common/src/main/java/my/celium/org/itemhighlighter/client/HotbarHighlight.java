package my.celium.org.itemhighlighter.client;

import my.celium.org.client.MycelScreens;
import my.celium.org.itemhighlighter.HighlightKeys;
import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;

/**
 * Highlights highlighted item textures themselves - never their slots - with
 * a tint and outline that hug the item's opaque texels.
 *
 * <p>Drawn in the HUD layer after everything else, using vanilla's fixed
 * hotbar geometry (182x22 bar centered at the bottom, 20px cells, item
 * sprites 16x16 at +3,+3 inside each cell, offhand slot to the left).
 * The shape comes from {@link ItemTextureMask}: only opaque sprite texels
 * are tinted, the crisp border lands on transparent texels directly adjacent
 * to the item, and a single soft halo ring sits one texel further out.
 * Empty sprite corners stay untouched, so the glow reads as attached to the
 * item rather than a box around the slot. Client only.
 */
public final class HotbarHighlight {
    /** Vanilla hotbar width; cells are 20 wide, bar 22 tall. */
    static final int BAR_WIDTH = 182;
    static final int BAR_HEIGHT = 22;
    static final int CELL = 20;
    /** Vanilla item sprites are 16x16, drawn at +3,+3 inside their cell. */
    static final int SPRITE = 16;
    static final int SPRITE_OFFSET = 3;
    /** Tint over opaque texels; halo ring just outside the crisp outline. */
    static final int TINT_ALPHA = 0x2E;
    static final int HALO_ALPHA = 0x50;

    private HotbarHighlight() {
    }

    /** Draws the tint for every highlighted hotbar and offhand stack. Never throws. */
    public static void render(GuiGraphicsExtractor gfx) {
        if (gfx == null) {
            return;
        }
        try {
            renderInner(gfx);
        } catch (RuntimeException e) {
            // HUD must never crash the frame.
        }
    }

    private static void renderInner(GuiGraphicsExtractor gfx) {
        if (!ItemHighlighterMod.isModEnabled()) {
            return;
        }
        if (!readFlag(() -> ItemHighlighterMod.hotbarHighlight.get(), true)) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null) {
            return;
        }
        if (isAnyScreenOpen(minecraft)) {
            // The HUD hotbar sits behind open menus (and vanilla dims it);
            // the menu's own slots are highlighted by ContainerHighlight
            // instead, so drawing here would only leave a misplaced
            // duplicate behind.
            return;
        }
        LocalPlayer player = minecraft.player;
        Inventory inventory = player.getInventory();
        int left = gfx.guiWidth() / 2 - BAR_WIDTH / 2;
        int top = gfx.guiHeight() - BAR_HEIGHT;
        for (int slot = 0; slot < 9; slot++) {
            outlineItem(gfx, inventory.getItem(slot),
                    left + slot * CELL + SPRITE_OFFSET, top + SPRITE_OFFSET);
        }
        outlineItem(gfx, player.getOffhandItem(), left - 29 + SPRITE_OFFSET, top + SPRITE_OFFSET);
    }

    /**
     * Texture-hugging highlight for one 16x16 item sprite at {@code (x, y)}.
     * Tints opaque texels, outlines transparent texels touching the item, and
     * adds one soft halo ring beyond that. Falls back to a tight sprite-edge
     * border when no alpha mask is available (3D models). Shared with
     * container-screen highlights so hotbar and inventory look identical.
     */
    static void outlineItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        Integer color = HighlightStore.getColor(HighlightKeys.keyFor(stack));
        if (color == null) {
            return;
        }
        highlightItem(gfx, stack, x, y, color & 0xFFFFFF);
    }

    /**
     * Draws the texture highlight for a stack known to be highlighted.
     * Never throws.
     */
    public static void highlightItem(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y, int rgb) {
        if (gfx == null || stack == null || stack.isEmpty()) {
            return;
        }
        try {
            boolean[] mask = ItemTextureMask.maskFor(stack);
            if (mask == null) {
                fallbackOutline(gfx, x, y, rgb);
            } else {
                textureOutline(gfx, mask, x, y, rgb);
            }
        } catch (RuntimeException e) {
            // HUD must never crash the frame.
        }
    }

    /** Tight sprite-edge border used when the true texel shape is unknown. */
    private static void fallbackOutline(GuiGraphicsExtractor gfx, int x, int y, int rgb) {
        int opaque = 0xFF000000 | rgb;
        gfx.fill(x, y, x + SPRITE, y + SPRITE, (TINT_ALPHA << 24) | rgb);
        gfx.fill(x, y, x + SPRITE, y + 1, opaque);
        gfx.fill(x, y + SPRITE - 1, x + SPRITE, y + SPRITE, opaque);
        gfx.fill(x, y, x + 1, y + SPRITE, opaque);
        gfx.fill(x + SPRITE - 1, y, x + SPRITE, y + SPRITE, opaque);
    }

    /**
     * Per-texel highlight: tint where the item is opaque, crisp outline where
     * transparency touches the item, soft halo one texel further out. Empty
     * sprite corners are never painted.
     */
    private static void textureOutline(GuiGraphicsExtractor gfx, boolean[] mask, int x, int y, int rgb) {
        int size = ItemTextureMask.SIZE;
        int opaque = 0xFF000000 | rgb;
        int tint = (TINT_ALPHA << 24) | rgb;
        int halo = (HALO_ALPHA << 24) | rgb;
        for (int py = 0; py < size; py++) {
            for (int px = 0; px < size; px++) {
                if (mask[py * size + px]) {
                    gfx.fill(x + px, y + py, x + px + 1, y + py + 1, tint);
                }
            }
        }
        for (int py = -1; py <= size; py++) {
            for (int px = -1; px <= size; px++) {
                if (isOpaque(mask, size, px, py)) {
                    continue;
                }
                if (touchesOpaque(mask, size, px, py, 1)) {
                    gfx.fill(x + px, y + py, x + px + 1, y + py + 1, opaque);
                } else if (touchesOpaque(mask, size, px, py, 2)) {
                    gfx.fill(x + px, y + py, x + px + 1, y + py + 1, halo);
                }
            }
        }
    }

    private static boolean isOpaque(boolean[] mask, int size, int x, int y) {
        return x >= 0 && y >= 0 && x < size && y < size && mask[y * size + x];
    }

    /** Whether any opaque texel lies within Chebyshev distance {@code radius}. */
    private static boolean touchesOpaque(boolean[] mask, int size, int x, int y, int radius) {
        for (int dy = -radius; dy <= radius; dy++) {
            for (int dx = -radius; dx <= radius; dx++) {
                if (dx == 0 && dy == 0) {
                    continue;
                }
                if (isOpaque(mask, size, x + dx, y + dy)) {
                    return true;
                }
            }
        }
        return false;
    }

    private static boolean readFlag(java.util.function.BooleanSupplier reader, boolean fallback) {
        try {
            return reader.getAsBoolean();
        } catch (RuntimeException e) {
            return fallback;
        }
    }

    /** Whether any screen (menu, chat, ...) is currently open. */
    private static boolean isAnyScreenOpen(Minecraft minecraft) {
        try {
            return minecraft.gui != null && MycelScreens.current() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
