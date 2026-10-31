package my.celium.org.itemhighlighter.client;

import com.mojang.blaze3d.platform.InputConstants;

/** Keyboard calls that differ between Minecraft lines; the 26.3 copy (one per line in common/src/mc). */
final class KeyInput {
    /** Keyboard key codes. */
    static final InputConstants.Type KEYBOARD = InputConstants.Type.KEYBOARD;

    private KeyInput() {
    }

    /** Whether a keyboard key is held right now. */
    static boolean isDown(InputConstants.Key key) {
        return InputConstants.isKeyDown(key.getValue());
    }
}
