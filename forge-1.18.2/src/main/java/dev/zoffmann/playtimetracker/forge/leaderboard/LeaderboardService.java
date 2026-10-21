package dev.zoffmann.playtimetracker.forge.leaderboard;

import com.google.gson.JsonParser;
import com.mojang.logging.LogUtils;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Predicate;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraft.world.level.storage.LevelResource;

public final class LeaderboardService implements AutoCloseable {
    public static final int MAX_PLAYERS = 4096;
    private final MinecraftServer server;
    private final Predicate<UUID> afk;
    private final ExecutorService reader = Executors.newSingleThreadExecutor(task -> {
        Thread thread = new Thread(task, "playtimetracker-statistics");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<UUID, Long> requests = new HashMap<>();
    private Map<UUID, PlayerStats> offline = Map.of();
    private boolean scanning;
    private boolean loaded;
    private volatile boolean closed;
    private int knownPlayers;

    public LeaderboardService(MinecraftServer server, Predicate<UUID> afk) {
        this.server = server;
        this.afk = afk;
        rescan();
    }

    public void rescan() {
        if (scanning || closed) return;
        scanning = true;
        Path directory = server.getWorldPath(LevelResource.PLAYER_STATS_DIR);
        reader.execute(() -> {
            Map<UUID, PlayerStats> result = new HashMap<>();
            int count = 0;
            int skipped = 0;
            if (Files.isDirectory(directory)) {
                try (var files = Files.list(directory)) {
                    var iterator = files.filter(p -> p.getFileName().toString().endsWith(".json")).sorted().iterator();
                    while (!closed && iterator.hasNext()) {
                        Path file = iterator.next();
                        UUID id;
                        try { id = UUID.fromString(file.getFileName().toString().replace(".json", "")); }
                        catch (IllegalArgumentException exception) { continue; }
                        count++;
                        if (result.size() >= MAX_PLAYERS) continue;
                        try {
                            if (Files.size(file) > 4 * 1024 * 1024) { skipped++; continue; }
                            try (Reader json = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
                                result.put(id, PlayerStats.fromJson(id, "Player " + id.toString().substring(0, 8),
                                        JsonParser.parseReader(json).getAsJsonObject()));
                            }
                        } catch (Exception exception) { skipped++; }
                    }
                } catch (Exception exception) {
                    LogUtils.getLogger().warn("Playtime leaderboard could not scan saved statistics ({}).", exception.getClass().getSimpleName());
                }
            }
            int total = count;
            int invalid = skipped;
            if (closed) return;
            server.execute(() -> {
                if (closed) return;
                // Profile-cache access stays on the server thread; no name lookup HTTP requests.
                result.replaceAll((id, stats) -> server.getProfileCache().get(id)
                        .map(profile -> stats.named(profile.getName())).orElse(stats));
                offline = Map.copyOf(result);
                knownPlayers = total;
                loaded = true;
                scanning = false;
                if (invalid > 0) LogUtils.getLogger().warn("Playtime leaderboard skipped {} unreadable statistics files.", invalid);
            });
        });
    }

    public boolean allowRequest(ServerPlayer player) {
        long now = System.nanoTime();
        Long last = requests.get(player.getUUID());
        if (last != null && now - last < 1_000_000_000L) return false;
        requests.put(player.getUUID(), now);
        return true;
    }

    public void logout(ServerPlayer player) {
        requests.remove(player.getUUID());
        // Keep the final live counters immediately; disk stats may save after the logout event.
        Map<UUID, PlayerStats> update = new HashMap<>(offline);
        PlayerStats live = live(player);
        update.put(live.id(), new PlayerStats(live.id(), live.name(), live.playTicks(), live.deaths(), live.mobKills(),
                live.minedBlocks(), live.distanceCm(), live.jumps(), false, false));
        offline = Map.copyOf(update);
    }

    public Snapshot snapshot() {
        Map<UUID, PlayerStats> all = new HashMap<>(offline);
        for (ServerPlayer player : server.getPlayerList().getPlayers()) all.put(player.getUUID(), live(player));
        List<PlayerStats> sorted = Metric.PLAYTIME.sorted(new ArrayList<>(all.values()), "", false);
        return new Snapshot(sorted.stream().limit(MAX_PLAYERS).toList(), loaded, Math.max(knownPlayers, all.size()));
    }

    private PlayerStats live(ServerPlayer player) {
        StatsCounter stats = player.getStats();
        long mined = 0;
        for (var block : Registry.BLOCK) mined += Math.max(0, stats.getValue(Stats.BLOCK_MINED.get(block)));
        long distance = 0;
        for (String key : new String[]{"walk_one_cm", "sprint_one_cm", "crouch_one_cm", "swim_one_cm",
                "walk_on_water_one_cm", "walk_under_water_one_cm", "fly_one_cm", "aviate_one_cm",
                "boat_one_cm", "minecart_one_cm", "horse_one_cm", "pig_one_cm", "strider_one_cm"}) {
            distance += Math.max(0, stats.getValue(Stats.CUSTOM.get(new ResourceLocation("minecraft:" + key))));
        }
        return new PlayerStats(player.getUUID(), player.getGameProfile().getName(),
                stats.getValue(Stats.CUSTOM.get(Stats.PLAY_TIME)), stats.getValue(Stats.CUSTOM.get(Stats.DEATHS)),
                stats.getValue(Stats.CUSTOM.get(Stats.MOB_KILLS)), mined, distance,
                stats.getValue(Stats.CUSTOM.get(Stats.JUMP)), true, afk.test(player.getUUID()));
    }

    @Override public void close() { closed = true; reader.shutdownNow(); requests.clear(); }

    public record Snapshot(List<PlayerStats> players, boolean loaded, int knownPlayers) {
        public Snapshot { players = List.copyOf(players); }
    }
}
