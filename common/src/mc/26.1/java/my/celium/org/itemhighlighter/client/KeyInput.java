package my.celium.org.itemhighlighter.client;

import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.Minecraft;

/** Keyboard calls that differ between Minecraft lines; the 26.1 copy (one per line in common/src/mc). */
final class KeyInput {
    /** Keyboard key codes. */
    static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYSYM;

    private KeyInput() {
    }

    /** Whether a keyboard key is held right now. */
    static boolean isDown(InputConstants.Key key) {
        return InputConstants.isKeyDown(Minecraft.getInstance().getWindow(), key.getValue());
    }
}
