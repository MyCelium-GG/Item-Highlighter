package my.celium.org.itemhighlighter.client;

import java.util.Iterator;
import java.util.Map;
import java.util.Optional;
import java.util.WeakHashMap;

import my.celium.org.itemhighlighter.HighlightColor;
import my.celium.org.itemhighlighter.HighlightKeys;
import my.celium.org.itemhighlighter.HighlightStore;
import my.celium.org.itemhighlighter.ItemHighlighterMod;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.TeamColor;

/**
 * Gives highlighted world entities (dropped items, item frames) a real
 * glowing outline in the highlight's color.
 *
 * <p>Vanilla renders a colored outline around any glowing entity, colored by
 * its scoreboard team. Every client tick this syncs that state: highlighted
 * entities are tagged glowing and joined to a client-side team in the
 * nearest palette color, everything else is left alone. Client only, same
 * per-tick pattern as vanilla nameplate syncing.
 */
public final class EntityGlowSync {
    /** Client-side teams are ours alone; the server never touches them. */
    static final String TEAM_PREFIX = "ih_";

    private static final Map<Entity, Applied> APPLIED = new WeakHashMap<>();

    private EntityGlowSync() {
    }

    private record Applied(boolean originalTag, String originalTeam, String ourTeam, int rgb) {
    }

    public static void tickClient(Minecraft minecraft) {
        if (minecraft == null) {
            return;
        }
        try {
            tickClientInner(minecraft);
        } catch (RuntimeException ignored) {
            APPLIED.clear();
        }
    }

    private static void tickClientInner(Minecraft minecraft) {
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.player == null) {
            APPLIED.clear();
            return;
        }
        if (!ItemHighlighterMod.isModEnabled()) {
            restoreAll(level);
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        for (Entity entity : level.entitiesForRendering()) {
            if (entity.isRemoved()) {
                continue;
            }
            Integer rgb = highlightFor(entity);
            if (rgb == null) {
                restoreIfApplied(level, entity);
            } else {
                syncEntity(level, entity, rgb);
            }
        }
        pruneRemoved(level);
    }

    /** Highlight color for an entity's displayed item, or {@code null}. */
    static Integer highlightFor(Entity entity) {
        ItemStack stack = displayedItem(entity);
        if (stack == null || stack.isEmpty()) {
            return null;
        }
        return HighlightStore.getColor(HighlightKeys.keyFor(stack));
    }

    /** The item an entity shows: frame contents or dropped stack. */
    static ItemStack displayedItem(Entity entity) {
        if (entity instanceof ItemFrame frame) {
            return frame.getItem();
        }
        if (entity instanceof ItemEntity item) {
            return item.getItem();
        }
        return null;
    }

    private static void syncEntity(ClientLevel level, Entity entity, int rgb) {
        Applied applied = APPLIED.get(entity);
        String wantTeam = TEAM_PREFIX + HighlightColor.toHex(rgb).substring(1);
        if (applied == null || applied.rgb() != rgb) {
            restoreIfApplied(level, entity);
            String originalTeam = entity.getTeam() == null ? null : entity.getTeam().getName();
            APPLIED.put(entity, new Applied(entity.hasGlowingTag(), originalTeam, wantTeam, rgb));
        }
        if (!entity.hasGlowingTag()) {
            entity.setGlowingTag(true);
        }
        joinTeam(level.getScoreboard(), entity, wantTeam, rgb);
    }

    private static void joinTeam(Scoreboard scoreboard, Entity entity, String teamName, int rgb) {
        if (scoreboard == null) {
            return;
        }
        PlayerTeam team = scoreboard.getPlayerTeam(teamName);
        if (team == null) {
            team = scoreboard.addPlayerTeam(teamName);
        }
        team.setColor(Optional.of(teamColorFor(rgb)));
        PlayerTeam current = entity.getTeam();
        if (current == null || !teamName.equals(current.getName())) {
            scoreboard.addPlayerToTeam(entity.getScoreboardName(), team);
        }
    }

    private static void restoreIfApplied(ClientLevel level, Entity entity) {
        Applied applied = APPLIED.remove(entity);
        if (applied != null) {
            restore(level, entity, applied);
        }
    }

    private static void restore(ClientLevel level, Entity entity, Applied applied) {
        if (entity.hasGlowingTag() != applied.originalTag()) {
            entity.setGlowingTag(applied.originalTag());
        }
        if (level == null) {
            return;
        }
        Scoreboard scoreboard = level.getScoreboard();
        if (scoreboard == null) {
            return;
        }
        // Only touch team state we created: leave entities whose team moved
        // on (server packets, other mods) strictly alone.
        PlayerTeam current = entity.getTeam();
        if (current != null && applied.ourTeam().equals(current.getName())) {
            scoreboard.removePlayerFromTeam(entity.getScoreboardName());
            if (applied.originalTeam() != null) {
                PlayerTeam original = scoreboard.getPlayerTeam(applied.originalTeam());
                if (original != null) {
                    scoreboard.addPlayerToTeam(entity.getScoreboardName(), original);
                }
            }
        }
    }

    private static void restoreAll(ClientLevel level) {
        for (Map.Entry<Entity, Applied> entry : APPLIED.entrySet()) {
            restore(level, entry.getKey(), entry.getValue());
        }
        APPLIED.clear();
    }

    private static void pruneRemoved(ClientLevel level) {
        Iterator<Entity> iterator = APPLIED.keySet().iterator();
        while (iterator.hasNext()) {
            Entity entity = iterator.next();
            if (entity.isRemoved()) {
                iterator.remove();
            }
        }
    }

    /** Nearest 16-color palette entry for a packed highlight color. */
    static TeamColor teamColorFor(int rgb) {
        return switch (HighlightColor.nearestLegacyCode(rgb)) {
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
        };
    }
}
