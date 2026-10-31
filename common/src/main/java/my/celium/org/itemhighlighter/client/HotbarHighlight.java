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
 * The highlight look, shared by the hotbar, every container screen and the
 * menu's preview: a soft glow fading inward from the slot's edge, with the
 * item and its count drawn again on top so both stay fully readable.
 *
 * <p>Hooks run after the game drew its items and the renderer sorts fills
 * below items below text within one layer, so drawing glow, item, count in
 * that order works wherever the hook falls on each loader. Client only.
 */
public final class HotbarHighlight {
    /** Vanilla hotbar width; cells are 20 wide, bar 22 tall. */
    static final int BAR_WIDTH = 182;
    static final int BAR_HEIGHT = 22;
    static final int CELL = 20;
    /** Vanilla item sprites are 16x16, drawn at +3,+3 inside their cell. */
    static final int SPRITE = 16;
    static final int SPRITE_OFFSET = 3;
    /** Alpha of each ring, from the crisp slot edge inward. */
    private static final int[] RING_ALPHA = {0xF0, 0x78, 0x48, 0x28, 0x14};
    /** Alpha of the area the rings leave in the middle. */
    private static final int CENTER_ALPHA = 0x0C;

    private HotbarHighlight() {
    }

    /** Draws the highlight for every highlighted hotbar and offhand stack. Never throws. */
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
        if (!ItemHighlighterMod.isModEnabled() || !slotsEnabled()) {
            return;
        }
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft == null || minecraft.player == null || isAnyScreenOpen(minecraft)) {
            // Open menus dim the HUD hotbar and ContainerHighlight covers their
            // own slots, so drawing here would leave a stray copy behind.
            return;
        }
        LocalPlayer player = minecraft.player;
        Inventory inventory = player.getInventory();
        int left = gfx.guiWidth() / 2 - BAR_WIDTH / 2;
        int top = gfx.guiHeight() - BAR_HEIGHT;
        for (int slot = 0; slot < 9; slot++) {
            highlightIfMarked(gfx, inventory.getItem(slot), left + slot * CELL + SPRITE_OFFSET, top + SPRITE_OFFSET);
        }
        highlightIfMarked(gfx, player.getOffhandItem(), left - 29 + SPRITE_OFFSET, top + SPRITE_OFFSET);
    }

    /** Highlights a slot's stack at {@code (x, y)} when it has a color. Never throws. */
    static void highlightIfMarked(GuiGraphicsExtractor gfx, ItemStack stack, int x, int y) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        try {
            Integer color = HighlightStore.getColor(HighlightKeys.keyFor(stack));
            if (color != null) {
                glow(gfx, x, y, color & 0xFFFFFF);
                gfx.item(stack, x, y);
                gfx.itemDecorations(Minecraft.getInstance().font, stack, x, y);
            }
        } catch (RuntimeException e) {
            // Rendering must never crash the frame.
        }
    }

    /** The glow alone, for the 16x16 slot interior at {@code (x, y)}. */
    static void glow(GuiGraphicsExtractor gfx, int x, int y, int rgb) {
        int x0 = x - 1;
        int y0 = y - 1;
        int x1 = x + SPRITE + 1;
        int y1 = y + SPRITE + 1;
        for (int ring = 0; ring < RING_ALPHA.length; ring++) {
            int color = (RING_ALPHA[ring] << 24) | rgb;
            int l = x0 + ring;
            int t = y0 + ring;
            int r = x1 - ring;
            int b = y1 - ring;
            // The outer ring skips its corners, which softens the square.
            int inset = ring == 0 ? 1 : 0;
            gfx.fill(l + inset, t, r - inset, t + 1, color);
            gfx.fill(l + inset, b - 1, r - inset, b, color);
            gfx.fill(l, t + 1, l + 1, b - 1, color);
            gfx.fill(r - 1, t + 1, r, b - 1, color);
        }
        int inset = RING_ALPHA.length;
        gfx.fill(x0 + inset, y0 + inset, x1 - inset, y1 - inset, (CENTER_ALPHA << 24) | rgb);
    }

    static boolean slotsEnabled() {
        try {
            return ItemHighlighterMod.hotbarHighlight.get();
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static boolean isAnyScreenOpen(Minecraft minecraft) {
        try {
            return minecraft.gui != null && MycelScreens.current() != null;
        } catch (RuntimeException e) {
            return false;
        }
    }
}
