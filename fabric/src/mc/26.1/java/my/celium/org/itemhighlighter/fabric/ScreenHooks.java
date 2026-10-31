package my.celium.org.itemhighlighter.fabric;

import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;

/** Fabric screen events that differ between Minecraft lines; the 26.1 copy (one per line in fabric/src/mc). */
final class ScreenHooks {
    private ScreenHooks() {
    }

    interface Draw {
        void draw(Screen screen, GuiGraphicsExtractor gfx, int mouseX, int mouseY);
    }

    /** Runs {@code draw} every frame, below the screen's items and tooltip. */
    static void afterForeground(Screen screen, Draw draw) {
        // Fabric API for 26.1 has no afterForeground, and afterExtract draws
        // over tooltips. Here the glow sits under the real items instead.
        ScreenEvents.afterBackground(screen).register((current, gfx, mouseX, mouseY, tickDelta) -> draw.draw(current, gfx, mouseX, mouseY));
    }
}
