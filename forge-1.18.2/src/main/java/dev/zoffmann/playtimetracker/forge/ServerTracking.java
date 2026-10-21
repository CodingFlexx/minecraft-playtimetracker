package dev.zoffmann.playtimetracker.forge;

import com.mojang.logging.LogUtils;
import dev.zoffmann.playtimetracker.AfkTracker;
import net.minecraft.core.BlockPos;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.event.server.ServerStartedEvent;
import net.minecraftforge.event.server.ServerStoppingEvent;
import net.minecraftforge.eventbus.api.EventPriority;
import net.minecraftforge.eventbus.api.SubscribeEvent;

public abstract class ServerTracking {
    public static final String MOD_ID = "playtimetracker";
    private AfkTracker afk = new AfkTracker();
    private final TabDisplayManager display = new TabDisplayManager();
    private AfkTeams teams;
    private MinecraftServer server;
    private int tickCounter;
    @SubscribeEvent
    public void started(ServerStartedEvent event) {
        server = event.getServer();
        afk = new AfkTracker();
        display.clear();
        tickCounter = 0;
        teams = server.overworld().getDataStorage().computeIfAbsent(AfkTeams::load, AfkTeams::new, AfkTeams.DATA_NAME);
        teams.recover(server.getScoreboard());
        onStarted(server);
        LogUtils.getLogger().info("Playtime + AFK Tracker active (Forge 1.18.2).");
    }

    @SubscribeEvent
    public void stopping(ServerStoppingEvent event) {
        onStopping();
        if (teams != null) teams.recover(event.getServer().getScoreboard());
        afk = new AfkTracker();
        display.clear();
        server = null;
        teams = null;
    }

    @SubscribeEvent
    public void login(PlayerEvent.PlayerLoggedInEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && server != null) {
            teams.leave(server.getScoreboard(), player.getScoreboardName());
            reset(player);
        }
    }

    @SubscribeEvent
    public void logout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && server != null) {
            onLogout(player);
            teams.leave(server.getScoreboard(), player.getScoreboardName());
            afk.remove(player.getUUID());
            display.forget(player.getUUID());
        }
    }

    @SubscribeEvent
    public void respawn(PlayerEvent.PlayerRespawnEvent event) {
        if (event.getPlayer() instanceof ServerPlayer player && server != null) {
            teams.leave(server.getScoreboard(), player.getScoreboardName());
            reset(player);
        }
    }

    private void reset(ServerPlayer player) {
        BlockPos position = player.blockPosition();
        afk.reset(player.getUUID(), position.getX(), position.getY(), position.getZ());
        display.refresh(player, false);
    }

    @SubscribeEvent(priority = EventPriority.LOW)
    public void tabName(PlayerEvent.TabListNameFormat event) { display.format(event); }

    @SubscribeEvent
    public void tick(TickEvent.ServerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || server == null) return;
        tickCounter = (tickCounter + 1) % 1200;
        if (tickCounter == 0) onMinute();
        if (tickCounter % 20 != 0) return;
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            BlockPos position = player.blockPosition();
            boolean changed = afk.tick(player.getUUID(), position.getX(), position.getY(), position.getZ());
            boolean isAfk = afk.isAfk(player.getUUID());
            if (changed) {
                if (isAfk) teams.enter(server.getScoreboard(), player.getScoreboardName());
                else teams.leave(server.getScoreboard(), player.getScoreboardName());
            }
            if (changed || tickCounter == 0) display.refresh(player, isAfk);
        }
    }
    protected boolean isAfk(java.util.UUID player) { return afk.isAfk(player); }
    protected void onStarted(MinecraftServer server) {}
    protected void onStopping() {}
    protected void onLogout(ServerPlayer player) {}
    protected void onMinute() {}
}
