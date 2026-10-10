package my.celium.org.itemhighlighter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

/**
 * {@link HighlightKeys} key helpers. Only pure string logic is tested here;
 * building keys from real stacks needs Minecraft registries.
 */
class HighlightKeysTest {
    @Test
    void nullKeysAreBlank() {
        assertEquals("", HighlightKeys.keyFor(null));
        assertEquals("", HighlightKeys.typeId(null));
        assertEquals("", HighlightKeys.exactKeyFor(null));
    }

    @Test
    void exactKeysDetected() {
        assertFalse(HighlightKeys.isExactKey("minecraft:diamond_sword"));
        assertFalse(HighlightKeys.isExactKey(null));
        assertTrue(HighlightKeys.isExactKey("minecraft:diamond_sword{minecraft:damage=3}"));
    }

    @Test
    void displayStripsFingerprint() {
        assertEquals("minecraft:diamond_sword",
                HighlightKeys.displayId("minecraft:diamond_sword"));
        assertEquals("minecraft:diamond_sword",
                HighlightKeys.displayId("minecraft:diamond_sword{minecraft:damage=3}"));
        assertEquals("", HighlightKeys.displayId(null));
        assertEquals("", HighlightKeys.displayId("   "));
    }

    @Test
    void exactMatchDefaultsOff() {
        // No Mycel init here on purpose: an uninitialized config must degrade
        // to type-wide matching, never crash.
        assertFalse(HighlightKeys.useExactMatch());
    }
}
