package dev.zoffmann.playtimetracker.forge;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AfkTeamsTest {
    @Test
    void movementAndLogoutRestoreThePreviousTeamWithoutRemovingOtherMembers() {
        Scoreboard scoreboard = new Scoreboard();
        PlayerTeam original = scoreboard.addPlayerTeam("blue");
        scoreboard.addPlayerToTeam("Alex", original);
        scoreboard.addPlayerToTeam("Sam", original);
        AfkTeams teams = new AfkTeams();
        teams.enter(scoreboard, "Alex");
        teams.enter(scoreboard, "Alex"); // must not overwrite the saved original
        assertEquals(AfkTeams.TEAM_NAME, scoreboard.getPlayersTeam("Alex").getName());
        assertEquals(" [AFK]", scoreboard.getPlayerTeam(AfkTeams.TEAM_NAME).getPlayerSuffix().getString());
        assertSame(original, scoreboard.getPlayersTeam("Sam"));
        teams.leave(scoreboard, "Alex");
        assertSame(original, scoreboard.getPlayersTeam("Alex"));
        teams.leave(scoreboard, "Alex"); // no throw and no removal from restored team
        assertSame(original, scoreboard.getPlayersTeam("Alex"));
    }

    @Test
    void playersWithoutATeamReturnToNoTeamAndDeletedTeamsAreNotRecreated() {
        Scoreboard scoreboard = new Scoreboard();
        AfkTeams teams = new AfkTeams();
        teams.enter(scoreboard, "Alex");
        teams.leave(scoreboard, "Alex");
        assertNull(scoreboard.getPlayersTeam("Alex"));
        PlayerTeam old = scoreboard.addPlayerTeam("old");
        scoreboard.addPlayerToTeam("Alex", old);
        teams.enter(scoreboard, "Alex");
        scoreboard.removePlayerTeam(old);
        teams.leave(scoreboard, "Alex");
        assertNull(scoreboard.getPlayersTeam("Alex"));
        assertNull(scoreboard.getPlayerTeam("old"));
    }

    @Test
    void teamChangesByAnotherModWhileAfkArePreserved() {
        Scoreboard scoreboard = new Scoreboard();
        scoreboard.addPlayerToTeam("Alex", scoreboard.addPlayerTeam("old"));
        AfkTeams teams = new AfkTeams();
        teams.enter(scoreboard, "Alex");
        PlayerTeam reassigned = scoreboard.addPlayerTeam("new");
        scoreboard.addPlayerToTeam("Alex", reassigned);
        teams.leave(scoreboard, "Alex");
        assertSame(reassigned, scoreboard.getPlayersTeam("Alex"));
    }

    @Test
    void savedMembershipsRecoverAfterRestartIncludingOfflinePlayers() {
        Scoreboard scoreboard = new Scoreboard();
        PlayerTeam original = scoreboard.addPlayerTeam("blue");
        scoreboard.addPlayerToTeam("Alex", original);
        AfkTeams before = new AfkTeams();
        before.enter(scoreboard, "Alex");
        before.enter(scoreboard, "Sam");
        AfkTeams after = AfkTeams.load(before.save(new CompoundTag()));
        after.recover(scoreboard);
        assertSame(original, scoreboard.getPlayersTeam("Alex"));
        assertNull(scoreboard.getPlayersTeam("Sam"));
        assertTrue(scoreboard.getPlayerTeam(AfkTeams.TEAM_NAME).getPlayers().isEmpty());
        assertTrue(after.save(new CompoundTag()).getCompound("previousTeams").isEmpty());
    }
}
