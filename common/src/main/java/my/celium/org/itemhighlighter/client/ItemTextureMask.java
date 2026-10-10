package my.celium.org.itemhighlighter.client;

import java.util.LinkedHashMap;
import java.util.Map;

import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.SpriteContents;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;

/**
 * 16x16 opacity mask for an item's GUI sprite, used to hug the item texture
 * instead of drawing a rectangular slot box.
 *
 * <p>Sampling goes through the item's baked particle sprite
 * ({@link ItemStackRenderState#pickParticleMaterial}), so transparent texels
 * stay unhighlighted and the outline follows the real item shape (swords,
 * pickaxes, bottles, ...). Results are cached per item id. Every lookup is
 * defensive: anything unknown returns {@code null} and callers fall back to
 * a tight rectangular border. Client only, never throws.
 */
public final class ItemTextureMask {
    /** GUI item sprites are treated as 16x16. */
    public static final int SIZE = 16;
    private static final int CACHE_MAX = 256;

    private static final Map<String, boolean[]> CACHE = new LinkedHashMap<>(64, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, boolean[]> eldest) {
            return size() > CACHE_MAX;
        }
    };

    private ItemTextureMask() {
    }

    /**
     * Opaque-texel mask ({@code SIZE * SIZE}, row-major) for a stack, or
     * {@code null} when the sprite cannot be resolved (3D-only models,
     * headless tests, missing level, ...). The returned array must not be
     * mutated.
     */
    public static boolean[] maskFor(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        String id;
        try {
            id = TooltipLines.itemId(stack);
        } catch (RuntimeException e) {
            return null;
        }
        if (id == null || id.isEmpty()) {
            return null;
        }
        synchronized (CACHE) {
            boolean[] cached = CACHE.get(id);
            if (cached != null) {
                return cached;
            }
        }
        boolean[] resolved;
        try {
            resolved = resolve(stack);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (resolved == null) {
            return null;
        }
        synchronized (CACHE) {
            CACHE.put(id, resolved);
            boolean[] stored = CACHE.get(id);
            return stored != null ? stored : resolved;
        }
    }

    /** Drops cached masks (resource reloads change sprites). Never throws. */
    public static void clearCache() {
        try {
            synchronized (CACHE) {
                CACHE.clear();
            }
        } catch (RuntimeException e) {
            // Cache must never break rendering.
        }
    }

    private static boolean[] resolve(ItemStack stack) {
        Minecraft minecraft;
        try {
            minecraft = Minecraft.getInstance();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (minecraft == null || minecraft.player == null) {
            return null;
        }
        ItemModelResolver resolver;
        try {
            resolver = minecraft.getItemModelResolver();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (resolver == null) {
            return null;
        }
        ItemStackRenderState state = new ItemStackRenderState();
        try {
            resolver.updateForNonLiving(state, stack, ItemDisplayContext.GUI, minecraft.player);
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (state.isEmpty()) {
            return null;
        }
        Material.Baked material;
        try {
            material = state.pickParticleMaterial(RandomSource.create());
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (material == null) {
            return null;
        }
        TextureAtlasSprite sprite;
        try {
            sprite = material.sprite();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (sprite == null) {
            return null;
        }
        SpriteContents contents;
        try {
            contents = sprite.contents();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (contents == null) {
            return null;
        }
        int width;
        int height;
        try {
            width = contents.width();
            height = contents.height();
        } catch (RuntimeException | LinkageError e) {
            return null;
        }
        if (width <= 0 || height <= 0) {
            return null;
        }
        boolean[] mask = new boolean[SIZE * SIZE];
        boolean anyOpaque = false;
        boolean anyTransparent = false;
        for (int y = 0; y < SIZE; y++) {
            int srcY = Math.min(height - 1, (y * height) / SIZE);
            for (int x = 0; x < SIZE; x++) {
                int srcX = Math.min(width - 1, (x * width) / SIZE);
                boolean transparent;
                try {
                    transparent = contents.isTransparent(0, srcX, srcY);
                } catch (RuntimeException | LinkageError e) {
                    return null;
                }
                boolean opaque = !transparent;
                mask[y * SIZE + x] = opaque;
                anyOpaque |= opaque;
                anyTransparent |= transparent;
            }
        }
        if (!anyOpaque || !anyTransparent) {
            // Fully solid or fully empty: no shape to hug, let callers use
            // the rectangular fallback.
            return null;
        }
        return mask;
    }
}
