package my.celium.org.itemhighlighter.client;

import java.lang.reflect.Field;

import org.joml.Matrix3x2fStack;

import my.celium.org.itemhighlighter.HighlightKeys;
import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * Draws the same texture-hugging highlight as the hotbar over every slot of
 * an open container screen (inventory, chests, furnaces, creative menu, ...)
 * plus the cursor-carried stack.
 *
 * <p>Called after the screen's foreground is extracted, so highlights sit on
 * top of items but under tooltips. Slot positions come from the menu's slot
 * list plus the screen's {@code leftPos}/{@code topPos} (read reflectively;
 * any failure simply skips rendering), minus whatever translation the
 * extractor's pose already carries: Forge fires its foreground event
 * <em>inside</em> the screen's {@code translate(leftPos, topPos)} pose while
 * Fabric/NeoForge fire after it is popped, so compensating for the live pose
 * keeps slot math correct on every loader. Client only, never throws.
 */
public final class ContainerHighlight {
    private static volatile Field leftPosField;
    private static volatile Field topPosField;
    private static volatile boolean fieldsResolved;

    private ContainerHighlight() {
    }

    /** Highlights highlighted items in an open container screen. Never throws. */
    public static void renderScreen(GuiGraphicsExtractor gfx, Screen screen, int mouseX, int mouseY) {
        if (gfx == null || screen == null) {
            return;
        }
        try {
            renderInner(gfx, screen, mouseX, mouseY);
        } catch (RuntimeException e) {
            // Screens must never crash the frame.
        }
    }

    private static void renderInner(GuiGraphicsExtractor gfx, Screen screen, int mouseX, int mouseY) {
        if (!ItemHighlighterMod.isModEnabled()) {
            return;
        }
        if (!readFlag(() -> ItemHighlighterMod.hotbarHighlight.get(), true)) {
            return;
        }
        if (!(screen instanceof AbstractContainerScreen<?> container)) {
            return;
        }
        int left = readOffset(container, true);
        int top = readOffset(container, false);
        if (left == Integer.MIN_VALUE || top == Integer.MIN_VALUE) {
            return;
        }
        int poseX = poseTranslation(gfx, true);
        int poseY = poseTranslation(gfx, false);
        int baseX = left - poseX;
        int baseY = top - poseY;
        for (Slot slot : container.getMenu().slots) {
            if (slot == null || !slot.isActive()) {
                continue;
            }
            ItemStack stack;
            try {
                stack = slot.getItem();
            } catch (RuntimeException e) {
                continue;
            }
            if (stack == null || stack.isEmpty()) {
                continue;
            }
            Integer color;
            try {
                color = HighlightStore.getColor(HighlightKeys.keyFor(stack));
            } catch (RuntimeException e) {
                continue;
            }
            if (color == null) {
                continue;
            }
            HotbarHighlight.highlightItem(gfx, stack, baseX + slot.x, baseY + slot.y, color & 0xFFFFFF);
        }
        ItemStack carried;
        try {
            carried = container.getMenu().getCarried();
        } catch (RuntimeException e) {
            return;
        }
        if (carried == null || carried.isEmpty()) {
            return;
        }
        Integer carriedColor = HighlightStore.getColor(HighlightKeys.keyFor(carried));
        if (carriedColor == null) {
            return;
        }
        HotbarHighlight.highlightItem(gfx, carried, mouseX - 8 - poseX, mouseY - 8 - poseY,
                carriedColor & 0xFFFFFF);
    }

    /**
     * Live translation of the extractor pose (rounded to whole pixels).
     * Container screens draw their contents inside a {@code translate(leftPos,
     * topPos)} pose; whether our hook runs inside it (Forge) or after it is
     * popped (Fabric/NeoForge), subtracting it keeps screen-space math exact.
     * Any failure degrades to zero, never throws.
     */
    static int poseTranslation(GuiGraphicsExtractor gfx, boolean xAxis) {
        try {
            if (gfx == null) {
                return 0;
            }
            Matrix3x2fStack pose = gfx.pose();
            if (pose == null) {
                return 0;
            }
            return Math.round(xAxis ? pose.m20() : pose.m21());
        } catch (RuntimeException e) {
            return 0;
        }
    }

    private static int readOffset(AbstractContainerScreen<?> screen, boolean left) {
        resolveFields();
        try {
            Field field = left ? leftPosField : topPosField;
            if (field == null) {
                return Integer.MIN_VALUE;
            }
            return field.getInt(screen);
        } catch (RuntimeException | IllegalAccessException e) {
            return Integer.MIN_VALUE;
        }
    }

    private static void resolveFields() {
        if (fieldsResolved) {
            return;
        }
        synchronized (ContainerHighlight.class) {
            if (fieldsResolved) {
                return;
            }
            try {
                Field left = AbstractContainerScreen.class.getDeclaredField("leftPos");
                left.setAccessible(true);
                leftPosField = left;
            } catch (RuntimeException | NoSuchFieldException e) {
                leftPosField = null;
            }
            try {
                Field top = AbstractContainerScreen.class.getDeclaredField("topPos");
                top.setAccessible(true);
                topPosField = top;
            } catch (RuntimeException | NoSuchFieldException e) {
                topPosField = null;
            }
            fieldsResolved = true;
        }
    }

    private static boolean readFlag(java.util.function.BooleanSupplier reader, boolean fallback) {
        try {
            return reader.getAsBoolean();
        } catch (RuntimeException e) {
            return fallback;
        }
    }
}
