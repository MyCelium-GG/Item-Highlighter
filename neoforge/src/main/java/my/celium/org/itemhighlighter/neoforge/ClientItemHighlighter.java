package my.celium.org.itemhighlighter.neoforge;

import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.datafixers.util.Either;

import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import my.celium.org.itemhighlighter.client.ContainerHighlight;
import my.celium.org.itemhighlighter.client.EntityGlowSync;
import my.celium.org.itemhighlighter.client.HighlightActions;
import my.celium.org.itemhighlighter.client.HotbarHighlight;
import my.celium.org.itemhighlighter.client.TooltipLines;
import net.minecraft.client.Minecraft;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.client.event.RenderGuiEvent;
import net.neoforged.neoforge.client.event.RenderTooltipEvent;
import net.neoforged.neoforge.client.event.ScreenEvent;
import net.neoforged.neoforge.common.NeoForge;

public final class ClientItemHighlighter {
    private static final String COMMAND_ROOT = "hl";
    private static final String ARGUMENT_COLOR = "color";
    private static final String LITERAL_CLEAR = "clear";
    private static final String LITERAL_LIST = "list";

    private ClientItemHighlighter() {
    }

    public static void register() {
        HighlightStore.load();
        NeoForge.EVENT_BUS.addListener(ClientItemHighlighter::onRenderGui);
        NeoForge.EVENT_BUS.addListener(ClientItemHighlighter::onScreenForeground);
        NeoForge.EVENT_BUS.addListener(ClientItemHighlighter::onGatherTooltip);
        NeoForge.EVENT_BUS.addListener(ClientItemHighlighter::onClientTick);
        NeoForge.EVENT_BUS.addListener(ClientItemHighlighter::onRegisterCommands);
    }

    private static void onRenderGui(RenderGuiEvent.Post event) {
        HotbarHighlight.render(event.getGuiGraphics());
    }

    private static void onScreenForeground(ScreenEvent.Render.Foreground event) {
        ContainerHighlight.renderScreen(event.getGuiGraphics(), event.getScreen(), event.getMouseX(),
                event.getMouseY());
    }

    private static void onGatherTooltip(RenderTooltipEvent.GatherComponents event) {
        if (TooltipLines.applies(event.getItemStack())) {
            Component line = TooltipLines.lineFor(event.getItemStack());
            if (line != null) {
                event.getTooltipElements()
                        .add(Either.<FormattedText, TooltipComponent>left(line));
            }
        }
    }

    private static void onRegisterCommands(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(buildCommand());
    }

    private static void onClientTick(ClientTickEvent.Post event) {
        EntityGlowSync.tickClient(Minecraft.getInstance());
    }

    private static LiteralArgumentBuilder<CommandSourceStack> buildCommand() {
        // Literals first: the color argument is a greedy string (so #, & and
        // § colors parse unquoted) and would otherwise also match these words.
        // dispatchColorArgument routes them as a safety net either way.
        return Commands.literal(COMMAND_ROOT)
                .then(Commands.literal(LITERAL_CLEAR)
                        .executes(context -> deliver(context.getSource(), HighlightActions.clearHeld())))
                .then(Commands.literal(LITERAL_LIST)
                        .executes(context -> deliver(context.getSource(), HighlightActions.list())))
                .then(Commands.argument(ARGUMENT_COLOR, StringArgumentType.greedyString())
                        .executes(context -> deliver(context.getSource(),
                                HighlightActions.dispatchColorArgument(
                                        StringArgumentType.getString(context, ARGUMENT_COLOR)))))
                .executes(context -> deliver(context.getSource(), HighlightActions.highlightHeld(null)));
    }

    private static int deliver(CommandSourceStack source, HighlightActions.Result result) {
        if (result.success()) {
            source.sendSuccess(result::message, false);
        } else {
            source.sendFailure(result.message());
        }
        return result.code();
    }
}
