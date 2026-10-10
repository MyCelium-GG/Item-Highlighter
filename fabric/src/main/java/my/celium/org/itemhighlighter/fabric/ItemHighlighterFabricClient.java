package my.celium.org.itemhighlighter.fabric;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;

import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import my.celium.org.itemhighlighter.client.ContainerHighlight;
import my.celium.org.itemhighlighter.client.EntityGlowSync;
import my.celium.org.itemhighlighter.client.HighlightActions;
import my.celium.org.itemhighlighter.client.HotbarHighlight;
import my.celium.org.itemhighlighter.client.TooltipLines;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.screen.v1.ScreenEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

public final class ItemHighlighterFabricClient implements ClientModInitializer {
    private static final String HUD_PATH = "highlight-hud";
    private static final String COMMAND_ROOT = "hl";
    private static final String ARGUMENT_COLOR = "color";
    private static final String LITERAL_CLEAR = "clear";
    private static final String LITERAL_LIST = "list";
    private static final Identifier HUD_ID =
            Identifier.fromNamespaceAndPath(ItemHighlighterMod.ID, HUD_PATH);

    @Override
    public void onInitializeClient() {
        HighlightStore.load();
        HudElementRegistry.addLast(HUD_ID, (gfx, tracker) -> HotbarHighlight.render(gfx));
        ClientTickEvents.END_CLIENT_TICK.register(EntityGlowSync::tickClient);
        ScreenEvents.AFTER_INIT.register((client, screen, width, height) -> ScreenEvents.afterForeground(screen)
                .register((foregroundScreen, gfx, mouseX, mouseY, tickDelta) ->
                        ContainerHighlight.renderScreen(gfx, foregroundScreen, mouseX, mouseY)));
        ItemTooltipCallback.EVENT.register((stack, context, flag, lines) -> {
            if (TooltipLines.applies(stack)) {
                Component line = TooltipLines.lineFor(stack);
                if (line != null) {
                    lines.add(line);
                }
            }
        });
        ClientCommandRegistrationCallback.EVENT.register(
                (dispatcher, registryAccess) -> dispatcher.register(buildCommand()));
    }

    private static LiteralArgumentBuilder<FabricClientCommandSource> buildCommand() {
        // Literals first: the color argument is a greedy string (so #, & and
        // § colors parse unquoted) and would otherwise also match these words.
        // dispatchColorArgument routes them as a safety net either way.
        return LiteralArgumentBuilder.<FabricClientCommandSource>literal(COMMAND_ROOT)
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal(LITERAL_CLEAR)
                        .executes(context -> deliver(context.getSource(), HighlightActions.clearHeld())))
                .then(LiteralArgumentBuilder.<FabricClientCommandSource>literal(LITERAL_LIST)
                        .executes(context -> deliver(context.getSource(), HighlightActions.list())))
                .then(RequiredArgumentBuilder.<FabricClientCommandSource, String>argument(
                                ARGUMENT_COLOR, StringArgumentType.greedyString())
                        .executes(context -> deliver(context.getSource(),
                                HighlightActions.dispatchColorArgument(
                                        StringArgumentType.getString(context, ARGUMENT_COLOR)))))
                .executes(context -> deliver(context.getSource(), HighlightActions.highlightHeld(null)));
    }

    private static int deliver(FabricClientCommandSource source, HighlightActions.Result result) {
        if (result.success()) {
            source.sendFeedback(result.message());
        } else {
            source.sendError(result.message());
        }
        return result.code();
    }
}
