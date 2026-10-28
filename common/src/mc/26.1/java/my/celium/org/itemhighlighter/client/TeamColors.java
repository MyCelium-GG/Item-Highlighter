package my.celium.org.itemhighlighter.client;

import net.minecraft.ChatFormatting;
import net.minecraft.world.scores.PlayerTeam;

/**
 * Sets a team's color from a legacy color code ('0'-'f'). The team color API
 * differs between Minecraft lines; this is the 26.1 copy (one per line in
 * common/src/mc).
 */
final class TeamColors {
    private TeamColors() {
    }

    // Before 26.2 teams are colored with a ChatFormatting.
    static void set(PlayerTeam team, char legacyCode) {
        ChatFormatting color = ChatFormatting.getByCode(legacyCode);
        team.setColor(color != null && color.isColor() ? color : ChatFormatting.WHITE);
    }
}
