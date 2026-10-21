package dev.zoffmann.playtimetracker.forge;

import dev.zoffmann.playtimetracker.PlaytimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Team;
import net.minecraftforge.event.entity.player.PlayerEvent;

public final class TabDisplayManager {
    private final Map<UUID, Boolean> afk = new HashMap<>();

    public void refresh(ServerPlayer player, boolean isAfk) {
        afk.put(player.getUUID(), isAfk);
        player.refreshTabListName();
    }

    public static Component decorate(Component base, long playtimeTicks, boolean isAfk) {
        return new TextComponent("").append(base.copy())
                .append(new TextComponent(PlaytimeFormatter.hoursLabel(playtimeTicks) + (isAfk ? " [AFK]" : "")));
    }

    public void format(PlayerEvent.TabListNameFormat event) {
        if (!(event.getPlayer() instanceof ServerPlayer player)) return;
        Component base = event.getDisplayName();
        if (base == null) {
            // The managed AFK team's suffix is added below, after the hour label.
            Team team = player.getTeam();
            base = team != null && AfkTeams.TEAM_NAME.equals(team.getName())
                    ? player.getName() : PlayerTeam.formatNameForTeam(team, player.getName());
        }
        long ticks = player.getStats().getValue(Stats.CUSTOM.get(Stats.PLAY_TIME));
        event.setDisplayName(decorate(base, ticks, afk.getOrDefault(player.getUUID(), false)));
    }

    public void forget(UUID player) { afk.remove(player); }
    public void clear() { afk.clear(); }
}
