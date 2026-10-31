package my.celium.org.itemhighlighter.client;

import java.lang.reflect.Field;

import com.mojang.blaze3d.platform.InputConstants;

import my.celium.org.client.MycelScreens;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;

/**
 * The way into the menu: the highlight key (held item in the world, hovered
 * item in any inventory) and {@code /hl}. Key mappings do not fire while a
 * screen is open, so inventories read the bound key directly. Client only.
 */
public final class HighlightClient {

    /** Registered by each loader. */
    public static final KeyMapping OPEN_KEY = new KeyMapping("key.itemhighlighter.open",
            KeyInput.KEYBOARD, InputConstants.KEY_H, KeyMapping.Category.INVENTORY);

    private static boolean menuRequested;
    private static AbstractContainerScreen<?> requestedParent;
    private static ItemStack requestedItem = ItemStack.EMPTY;
    private static boolean keyWasDown;
    private static Field hoveredSlotField;
    private static boolean hoveredSlotResolved;

    private HighlightClient() {
    }

    /** Opens the menu on the next tick, so it survives the chat screen closing after a command. */
    static void requestMenu(AbstractContainerScreen<?> parent, ItemStack item) {
        requestedParent = parent;
        requestedItem = item == null ? ItemStack.EMPTY : item.copy();
        menuRequested = true;
    }

    /** Runs every client tick on every loader. */
    public static void tick(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        EntityGlowSync.tickClient(minecraft);
        if (minecraft.player == null) {
            menuRequested = false;
            return;
        }
        while (OPEN_KEY.consumeClick()) {
            if (MycelScreens.current() == null) {
                requestMenu(null, minecraft.player.getMainHandItem());
            }
        }
        if (menuRequested) {
            menuRequested = false;
            MycelScreens.open(new HighlightScreen(requestedParent, requestedItem));
            requestedParent = null;
            requestedItem = ItemStack.EMPTY;
        }
    }

    /** Opens the menu on the hovered item when the key goes down. Runs every frame. */
    static void pollHoverKey(AbstractContainerScreen<?> screen) {
        boolean down = isOpenKeyDown();
        if (down && !keyWasDown && !(screen.getFocused() instanceof EditBox)) {
            openOnHovered(screen);
        }
        keyWasDown = down;
    }

    /** Opens the menu on the item under the cursor, if any. */
    static void openOnHovered(AbstractContainerScreen<?> screen) {
        Slot slot = hoveredSlot(screen);
        if (slot != null && slot.hasItem()) {
            requestMenu(screen, slot.getItem());
        }
    }

    private static boolean isOpenKeyDown() {
        try {
            InputConstants.Key key = InputConstants.getKey(OPEN_KEY.saveString());
            return key.getType() == KeyInput.KEYBOARD && KeyInput.isDown(key);
        } catch (RuntimeException e) {
            return false;
        }
    }

    private static Slot hoveredSlot(AbstractContainerScreen<?> screen) {
        try {
            if (!hoveredSlotResolved) {
                hoveredSlotResolved = true;
                Field field = AbstractContainerScreen.class.getDeclaredField("hoveredSlot");
                field.setAccessible(true);
                hoveredSlotField = field;
            }
            return hoveredSlotField == null ? null : (Slot) hoveredSlotField.get(screen);
        } catch (ReflectiveOperationException | RuntimeException e) {
            return null;
        }
    }
}
