package dev.zoffmann.playtimetracker;

import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundPlayerInfoUpdatePacket;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public final class TabDisplayManager {
    private static final String AFK_TEAM = "ptt-afk";

    /** Read by the Mixin to override the tab-list name. */
    private static final Map<UUID, Component> DISPLAY = new ConcurrentHashMap<>();

    /** Scoreboard name of the team a player was on before going AFK. */
    private final Map<UUID, String> previousTeam = new ConcurrentHashMap<>();

    public static Component getDisplayName(UUID id) {
        return DISPLAY.get(id);
    }

    public void ensureAfkTeam(MinecraftServer server) {
        Scoreboard sb = server.getScoreboard();
        PlayerTeam team = sb.getPlayerTeam(AFK_TEAM);
        if (team == null) {
            team = sb.addPlayerTeam(AFK_TEAM);
        }
        team.setPlayerSuffix(Component.literal(" [AFK]"));
    }

    public void refresh(MinecraftServer server, ServerPlayer player, boolean afk) {
        long ticks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        String label = PlaytimeFormatter.hoursLabel(ticks);
        Component name = Component.literal(player.getGameProfile().name() + label
                + (afk ? " [AFK]" : ""));
        DISPLAY.put(player.getUUID(), name);
        ClientboundPlayerInfoUpdatePacket packet = new ClientboundPlayerInfoUpdatePacket(
                EnumSet.of(ClientboundPlayerInfoUpdatePacket.Action.UPDATE_DISPLAY_NAME),
                List.of(player));
        server.getPlayerList().broadcastAll(packet);
    }

    public void applyAfkTeam(MinecraftServer server, ServerPlayer player, boolean afk) {
        Scoreboard sb = server.getScoreboard();
        PlayerTeam afkTeam = sb.getPlayerTeam(AFK_TEAM);
        if (afkTeam == null) return;
        String name = player.getScoreboardName();
        UUID id = player.getUUID();
        if (afk) {
            PlayerTeam current = sb.getPlayersTeam(name);
            if (current != null && !current.getName().equals(AFK_TEAM)) {
                previousTeam.put(id, current.getName());
            }
            sb.addPlayerToTeam(name, afkTeam);
        } else {
            sb.removePlayerFromTeam(name, afkTeam);
            String prev = previousTeam.remove(id);
            if (prev != null) {
                PlayerTeam prevTeam = sb.getPlayerTeam(prev);
                if (prevTeam != null) sb.addPlayerToTeam(name, prevTeam);
            }
        }
    }

    public void clear(MinecraftServer server, ServerPlayer player) {
        UUID id = player.getUUID();
        DISPLAY.remove(id);
        previousTeam.remove(id);
        Scoreboard sb = server.getScoreboard();
        PlayerTeam afkTeam = sb.getPlayerTeam(AFK_TEAM);
        if (afkTeam != null) {
            String name = player.getScoreboardName();
            if (afkTeam.equals(sb.getPlayersTeam(name))) {
                sb.removePlayerFromTeam(name, afkTeam);
            }
        }
    }
}
