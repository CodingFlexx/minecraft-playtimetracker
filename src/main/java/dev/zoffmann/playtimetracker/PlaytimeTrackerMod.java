package dev.zoffmann.playtimetracker;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;

public class PlaytimeTrackerMod implements ModInitializer {
    public static final String MOD_ID = "playtimetracker";

    private static final int AFK_SAMPLE_TICKS = 20;      // 1 s
    private static final int PLAYTIME_REFRESH_TICKS = 1200; // 60 s

    private final AfkTracker afk = new AfkTracker();
    private final TabDisplayManager display = new TabDisplayManager();
    private int tickCounter = 0;

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTED.register(display::ensureAfkTeam);

        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
            ServerPlayer p = handler.player;
            BlockPos bp = p.blockPosition();
            afk.reset(p.getUUID(), bp.getX(), bp.getY(), bp.getZ());
            display.refresh(server, p, false);
        });

        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> {
            ServerPlayer p = handler.player;
            afk.remove(p.getUUID());
            display.clear(server, p);
        });

        ServerTickEvents.END_SERVER_TICK.register(server -> {
            tickCounter++;
            boolean sample = tickCounter % AFK_SAMPLE_TICKS == 0;
            boolean fullRefresh = tickCounter % PLAYTIME_REFRESH_TICKS == 0;
            if (!sample && !fullRefresh) return;

            for (ServerPlayer p : server.getPlayerList().getPlayers()) {
                boolean changed = false;
                if (sample) {
                    BlockPos bp = p.blockPosition();
                    changed = afk.tick(p.getUUID(), bp.getX(), bp.getY(), bp.getZ());
                }
                boolean isAfk = afk.isAfk(p.getUUID());
                if (changed) {
                    display.applyAfkTeam(server, p, isAfk);
                }
                if (changed || fullRefresh) {
                    display.refresh(server, p, isAfk);
                }
            }
        });
    }
}
