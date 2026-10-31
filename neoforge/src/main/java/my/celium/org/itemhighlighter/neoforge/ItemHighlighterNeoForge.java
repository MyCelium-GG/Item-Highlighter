package my.celium.org.itemhighlighter.neoforge;

import my.celium.org.client.ClientHooks;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.client.gui.screens.Screen;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModLoadingContext;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;
import net.neoforged.neoforge.client.gui.IConfigScreenFactory;

/**
 * NeoForge entrypoint: server-safe bootstrap; client hooks (config screen,
 * highlight overlay, tooltip line, commands) are isolated in
 * {@link ClientItemHighlighter} so dedicated servers never load client
 * classes.
 */
@Mod(ItemHighlighterMod.ID)
public final class ItemHighlighterNeoForge {
    public ItemHighlighterNeoForge(IEventBus modBus,
            @SuppressWarnings("unused") ModContainer container) {
        ItemHighlighterMod.init();
        if (FMLEnvironment.getDist().isClient()) {
            ModLoadingContext.get().registerExtensionPoint(IConfigScreenFactory.class,
                    () -> (ModContainer ignored, Screen parent) -> ClientHooks.createConfigScreen(parent,
                            ItemHighlighterMod.ID));
            ClientItemHighlighter.register(modBus);
        }
    }
}
