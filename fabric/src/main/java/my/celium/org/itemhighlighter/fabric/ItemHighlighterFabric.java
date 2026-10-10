package my.celium.org.itemhighlighter.fabric;

import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint: server-safe bootstrap; everything client-only lives
 * in {@link ItemHighlighterFabricClient} so dedicated servers never load
 * client classes.
 */
public final class ItemHighlighterFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        ItemHighlighterMod.init();
    }
}
