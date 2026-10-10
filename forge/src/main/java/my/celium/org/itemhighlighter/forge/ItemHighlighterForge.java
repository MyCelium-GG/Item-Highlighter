package my.celium.org.itemhighlighter.forge;

import my.celium.org.client.ClientHooks;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.ConfigScreenHandler;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLEnvironment;

/**
 * Forge entrypoint: server-safe bootstrap; client hooks (config screen,
 * highlight overlay, tooltip line, commands) are isolated in
 * {@link ClientItemHighlighter} so dedicated servers never load client
 * classes.
 */
@Mod(ItemHighlighterMod.ID)
public final class ItemHighlighterForge {
    public ItemHighlighterForge(@SuppressWarnings("unused") FMLJavaModLoadingContext context) {
        ItemHighlighterMod.init();
        if (FMLEnvironment.dist == Dist.CLIENT) {
            ModLoadingContext.get().registerExtensionPoint(ConfigScreenHandler.ConfigScreenFactory.class,
                    () -> new ConfigScreenHandler.ConfigScreenFactory(
                            (Minecraft minecraft, Screen parent) -> ClientHooks.createConfigScreen(parent,
                                    ItemHighlighterMod.ID)));
            ClientItemHighlighter.register();
        }
    }
}
