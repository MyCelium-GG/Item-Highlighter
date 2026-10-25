package my.celium.org.itemhighlighter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** {@link HighlightStore} persists highlights across loads. */
class HighlightStoreTest {
    @TempDir
    Path tempDir;

    @BeforeEach
    void reset() {
        HighlightStore.clearForTesting();
    }

    @Test
    void setGetRemove() {
        assertNull(HighlightStore.getColor("minecraft:diamond_sword"));
        HighlightStore.setColor("minecraft:diamond_sword", 0xFF55AA);
        assertEquals(0xFF55AA, HighlightStore.getColor("minecraft:diamond_sword"));
        assertTrue(HighlightStore.remove("minecraft:diamond_sword"));
        assertNull(HighlightStore.getColor("minecraft:diamond_sword"));
        assertFalse(HighlightStore.remove("minecraft:diamond_sword"));
    }

    @Test
    void ignoresBlankIds() {
        HighlightStore.setColor(null, 0xFF55AA);
        HighlightStore.setColor("   ", 0xFF55AA);
        assertTrue(HighlightStore.entries().isEmpty());
    }

    @Test
    void survivesReload() {
        HighlightStore.load(tempDir);
        HighlightStore.setColor("minecraft:torch", 0x00AAAA);
        HighlightStore.setColor("minecraft:ender_pearl", 0x55FFFF);
        HighlightStore.load(tempDir);
        assertEquals(0x00AAAA, HighlightStore.getColor("minecraft:torch"));
        assertEquals(0x55FFFF, HighlightStore.getColor("minecraft:ender_pearl"));
    }

    @Test
    void brokenFileIsKeptInsteadOfOverwritten() throws Exception {
        Path data = tempDir.resolve(HighlightStore.FILE_NAME);
        Files.writeString(data, "{ \"highlights\": { broken");
        HighlightStore.load(tempDir);
        HighlightStore.setColor("minecraft:torch", 0x00AAAA);
        assertEquals("{ \"highlights\": { broken", Files.readString(tempDir.resolve(HighlightStore.FILE_NAME + ".broken")));
        assertEquals(0x00AAAA, HighlightStore.getColor("minecraft:torch"));
    }
}
