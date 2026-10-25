package my.celium.org.itemhighlighter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import my.celium.org.Mycel;
import my.celium.org.itemhighlighter.client.ContainerHighlight;
import my.celium.org.itemhighlighter.client.EntityGlowSync;
import my.celium.org.itemhighlighter.client.HotbarHighlight;
import my.celium.org.testing.MycelTesting;

/** The mod boots on Mycel with a tiny, well-defaulted config. */
class ItemHighlighterModTest {
    @BeforeAll
    static void initOnce(@TempDir Path tempDir) {
        MycelTesting.useConfigDir(tempDir);
        ItemHighlighterMod.init();
    }

    @Test
    void registersWithMycel() {
        assertTrue(Mycel.getRegisteredMods().contains(ItemHighlighterMod.META));
        assertEquals("itemhighlighter", ItemHighlighterMod.META.id());
    }

    @Test
    void configDefaults() {
        assertTrue(ItemHighlighterMod.enabled.get());
        assertEquals(0xFFA657, ItemHighlighterMod.defaultColor.get());
        assertTrue(ItemHighlighterMod.showTooltipLine.get());
        assertTrue(ItemHighlighterMod.hotbarHighlight.get());
        assertFalse(ItemHighlighterMod.matchComponents.get());
    }

    @Test
    void enableToggleFlowsThrough() {
        assertTrue(ItemHighlighterMod.isModEnabled());
        Mycel.setEnabled(ItemHighlighterMod.ID, false);
        try {
            assertTrue(!ItemHighlighterMod.isModEnabled());
        } finally {
            Mycel.setEnabled(ItemHighlighterMod.ID, true);
        }
        assertTrue(ItemHighlighterMod.isModEnabled());
    }

    @Test
    void defaultColorClamps() {
        assertEquals(0xFFA657, ItemHighlighterMod.effectiveDefaultColor());
    }

    @Test
    void hotbarRenderToleratesNulls() {
        // Loader HUD hooks must never crash the frame.
        HotbarHighlight.render(null);
    }

    @Test
    void containerRenderToleratesNulls() {
        // Loader screen hooks must never crash the frame.
        ContainerHighlight.renderScreen(null, null, 0, 0);
    }

    @Test
    void glowTickToleratesNulls() {
        // Loader tick hooks must never crash the tick.
        EntityGlowSync.tickClient(null);
    }
}
