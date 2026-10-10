package my.celium.org.itemhighlighter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.TypedDataComponent;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/**
 * Storage keys for highlighted items.
 *
 * <p>By default a key is the item registry id (e.g.
 * {@code "minecraft:diamond_sword"}), so every stack of that type highlights
 * at once. When {@code matchComponents} is enabled the key also carries a
 * fingerprint of the stack's non-default components
 * ({@code "minecraft:diamond_sword{minecraft:enchantments=...;...}"}), so
 * only stacks identical to the highlighted one match - a named, enchanted or
 * damaged item no longer drags every plain one along with it. Stacks without
 * extra components keep the plain id key in both modes.
 *
 * <p>Component entries are sorted by their serialized form, so keys are
 * stable across restarts (fastutil map iteration order is not). Everything is
 * defensive: any failure degrades to the plain type id, never throws.
 */
public final class HighlightKeys {
    private HighlightKeys() {
    }

    /** Whether exact per-stack matching is currently enabled. */
    public static boolean useExactMatch() {
        try {
            return ItemHighlighterMod.matchComponents.get();
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Store/lookup key for a stack under the current matching mode. */
    public static String keyFor(ItemStack stack) {
        if (stack == null) {
            return "";
        }
        if (useExactMatch()) {
            return exactKeyFor(stack);
        }
        return typeId(stack);
    }

    /** Plain registry id of a stack's item, or {@code ""} when unknown. */
    public static String typeId(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return "";
        }
        try {
            Identifier key = BuiltInRegistries.ITEM.getKey(stack.getItem());
            return key == null ? "" : key.toString();
        } catch (RuntimeException e) {
            return "";
        }
    }

    /**
     * Type id plus a sorted component fingerprint, or the plain type id when
     * the stack carries no non-default components (or the fingerprint cannot
     * be built).
     */
    public static String exactKeyFor(ItemStack stack) {
        String id = typeId(stack);
        if (id.isEmpty()) {
            return "";
        }
        String fingerprint;
        try {
            fingerprint = fingerprint(stack);
        } catch (RuntimeException e) {
            return id;
        }
        if (fingerprint == null || fingerprint.isEmpty()) {
            return id;
        }
        return id + "{" + fingerprint + "}";
    }

    /** Whether a stored key carries a component fingerprint. */
    public static boolean isExactKey(String key) {
        return key != null && key.contains("{");
    }

    /** Base item id of a stored key (fingerprint stripped). */
    public static String displayId(String key) {
        if (key == null || key.isBlank()) {
            return "";
        }
        int brace = key.indexOf('{');
        return brace < 0 ? key : key.substring(0, brace);
    }

    /** Sorted {@code type=value} snapshot of the stack's component patch. */
    static String fingerprint(ItemStack stack) {
        List<String> parts = new ArrayList<>();
        try {
            var split = stack.getComponentsPatch().split();
            for (TypedDataComponent<?> component : split.added()) {
                parts.add(componentName(component.type()) + "=" + component.value());
            }
            for (DataComponentType<?> removed : split.removed()) {
                parts.add("!" + componentName(removed));
            }
        } catch (RuntimeException e) {
            return fallbackFingerprint(stack);
        }
        Collections.sort(parts);
        return String.join(";", parts);
    }

    private static String fallbackFingerprint(ItemStack stack) {
        try {
            String dump = stack.getComponentsPatch().toString();
            return dump == null ? "" : dump;
        } catch (RuntimeException e) {
            return "";
        }
    }

    private static String componentName(DataComponentType<?> type) {
        try {
            Identifier key = BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(type);
            return key == null ? type.toString() : key.toString();
        } catch (RuntimeException e) {
            return type.toString();
        }
    }
}
