package dev.zoffmann.playtimetracker.forge;

import dev.zoffmann.playtimetracker.forge.leaderboard.LeaderboardService;
import dev.zoffmann.playtimetracker.forge.leaderboard.TrackerNetwork;
import net.minecraft.commands.Commands;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegisterCommandsEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** GUI build: shared tracking plus the client/server leaderboard channel. */
@Mod(ServerTracking.MOD_ID)
public final class PlaytimeTrackerMod extends ServerTracking {
    private static LeaderboardService leaderboard;
    public static LeaderboardService leaderboard() { return leaderboard; }

    public PlaytimeTrackerMod() {
        TrackerNetwork.register();
        MinecraftForge.EVENT_BUS.register(this);
    }
    @Override protected void onStarted(MinecraftServer server) { leaderboard = new LeaderboardService(server, this::isAfk); }
    @Override protected void onStopping() {
        if (leaderboard != null) leaderboard.close();
        leaderboard = null;
    }
    @Override protected void onLogout(ServerPlayer player) { if (leaderboard != null) leaderboard.logout(player); }
    @Override protected void onMinute() { if (leaderboard != null) leaderboard.rescan(); }

    @SubscribeEvent public void commands(RegisterCommandsEvent event) {
        event.getDispatcher().register(Commands.literal("playtime").executes(context -> {
            TrackerNetwork.respond(context.getSource().getPlayerOrException(), true);
            return 1;
        }));
        event.getDispatcher().register(Commands.literal("leaderboard").executes(context -> {
            TrackerNetwork.respond(context.getSource().getPlayerOrException(), true);
            return 1;
        }));
    }
}
