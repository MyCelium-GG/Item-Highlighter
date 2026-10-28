package my.celium.org.itemhighlighter.client;

import java.util.Optional;

import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.TeamColor;

/**
 * Sets a team's color from a legacy color code ('0'-'f'). The team color API
 * differs between Minecraft lines; this is the 26.2 copy (one per line in
 * common/src/mc).
 */
final class TeamColors {
    private TeamColors() {
    }

    static void set(PlayerTeam team, char legacyCode) {
        team.setColor(Optional.of(switch (legacyCode) {
            case '0' -> TeamColor.BLACK;
            case '1' -> TeamColor.DARK_BLUE;
            case '2' -> TeamColor.DARK_GREEN;
            case '3' -> TeamColor.DARK_AQUA;
            case '4' -> TeamColor.DARK_RED;
            case '5' -> TeamColor.DARK_PURPLE;
            case '6' -> TeamColor.GOLD;
            case '7' -> TeamColor.GRAY;
            case '8' -> TeamColor.DARK_GRAY;
            case '9' -> TeamColor.BLUE;
            case 'a' -> TeamColor.GREEN;
            case 'b' -> TeamColor.AQUA;
            case 'c' -> TeamColor.RED;
            case 'd' -> TeamColor.LIGHT_PURPLE;
            case 'e' -> TeamColor.YELLOW;
            default -> TeamColor.WHITE;
        }));
    }
}
