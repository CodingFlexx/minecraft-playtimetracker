package dev.zoffmann.playtimetracker.forge;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.TextComponent;
import net.minecraft.world.level.saveddata.SavedData;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;

/** Own only the AFK team; remember displaced memberships across world saves. */
public final class AfkTeams extends SavedData {
    public static final String TEAM_NAME = "ptt-afk";
    public static final String DATA_NAME = "playtimetracker_afk";
    private final Map<String, String> previousTeams = new HashMap<>();

    public static AfkTeams load(CompoundTag tag) {
        AfkTeams result = new AfkTeams();
        CompoundTag previous = tag.getCompound("previousTeams");
        for (String name : previous.getAllKeys()) result.previousTeams.put(name, previous.getString(name));
        return result;
    }

    @Override
    public CompoundTag save(CompoundTag tag) {
        CompoundTag previous = new CompoundTag();
        previousTeams.forEach(previous::putString);
        tag.put("previousTeams", previous);
        return tag;
    }

    public void ensureTeam(Scoreboard scoreboard) {
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team == null) team = scoreboard.addPlayerTeam(TEAM_NAME);
        team.setPlayerSuffix(new TextComponent(" [AFK]"));
    }

    public void enter(Scoreboard scoreboard, String player) {
        ensureTeam(scoreboard);
        PlayerTeam current = scoreboard.getPlayersTeam(player);
        if (current != null && TEAM_NAME.equals(current.getName())) return;
        previousTeams.put(player, current == null ? "" : current.getName());
        setDirty();
        scoreboard.addPlayerToTeam(player, scoreboard.getPlayerTeam(TEAM_NAME));
    }

    public void leave(Scoreboard scoreboard, String player) {
        PlayerTeam current = scoreboard.getPlayersTeam(player);
        String previous = previousTeams.remove(player);
        if (previous != null) setDirty();
        // A team assigned by an admin/another mod while AFK takes precedence.
        if (current == null || !TEAM_NAME.equals(current.getName())) return;
        scoreboard.removePlayerFromTeam(player, current);
        if (previous != null && !previous.isEmpty()) {
            PlayerTeam restore = scoreboard.getPlayerTeam(previous);
            if (restore != null && !TEAM_NAME.equals(restore.getName())) scoreboard.addPlayerToTeam(player, restore);
        }
    }

    /** Nobody should remain AFK after a restart; also recover offline members. */
    public void recover(Scoreboard scoreboard) {
        for (String player : new ArrayList<>(previousTeams.keySet())) leave(scoreboard, player);
        PlayerTeam team = scoreboard.getPlayerTeam(TEAM_NAME);
        if (team != null) {
            for (String player : new ArrayList<>(team.getPlayers())) leave(scoreboard, player);
        }
        ensureTeam(scoreboard);
    }
}
