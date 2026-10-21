package dev.zoffmann.playtimetracker.forge.leaderboard;

import com.google.gson.JsonObject;
import com.mojang.authlib.GameProfile;
import java.util.List;
import java.util.UUID;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestAssertException;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.stats.Stats;
import net.minecraft.stats.StatsCounter;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Runs inside Forge so the identity-based custom-stat registry is fully initialized. */
@GameTestHolder("playtimetracker")
@PrefixGameTestTemplate(false)
public final class LiveDistanceGameTest {
    private static void check(boolean condition, String message) {
        if (!condition) throw new GameTestAssertException(message);
    }

    private static List<ResourceLocation> movementStats() {
        return List.of(Stats.WALK_ONE_CM, Stats.SPRINT_ONE_CM, Stats.CROUCH_ONE_CM, Stats.SWIM_ONE_CM,
                Stats.WALK_ON_WATER_ONE_CM, Stats.WALK_UNDER_WATER_ONE_CM, Stats.FLY_ONE_CM, Stats.AVIATE_ONE_CM,
                Stats.BOAT_ONE_CM, Stats.MINECART_ONE_CM, Stats.HORSE_ONE_CM, Stats.PIG_ONE_CM, Stats.STRIDER_ONE_CM);
    }

    @GameTest(template = "empty")
    public static void emptyCounters(GameTestHelper helper) {
        check(LeaderboardService.distanceCm(new StatsCounter()) == 0,
                "Empty live counters must read zero without creating unregistered stats");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void allMovementCounters(GameTestHelper helper) {
        StatsCounter live = new StatsCounter();
        JsonObject custom = new JsonObject();
        long expected = 0;
        for (ResourceLocation id : movementStats()) {
            int value = Integer.MAX_VALUE - custom.size();
            live.setValue(null, Stats.CUSTOM.get(id), value);
            custom.addProperty(id.toString(), value);
            expected += value;
        }
        for (ResourceLocation excluded : List.of(Stats.FALL_ONE_CM, Stats.CLIMB_ONE_CM)) {
            live.setValue(null, Stats.CUSTOM.get(excluded), 1000);
            custom.addProperty(excluded.toString(), 1000);
        }
        JsonObject stats = new JsonObject();
        stats.add("minecraft:custom", custom);
        JsonObject root = new JsonObject();
        root.add("stats", stats);
        long actual = LeaderboardService.distanceCm(live);
        check(actual == expected, "Sum must include all 13 movement counters without int overflow");
        check(actual == PlayerStats.fromJson(UUID.randomUUID(), "Alex", root).distanceCm(),
                "Live and saved statistics must count the same movements, excluding falling and climbing");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void negativeCounters(GameTestHelper helper) {
        StatsCounter live = new StatsCounter();
        live.setValue(null, Stats.CUSTOM.get(Stats.WALK_ONE_CM), -20);
        live.setValue(null, Stats.CUSTOM.get(Stats.STRIDER_ONE_CM), 300);
        check(LeaderboardService.distanceCm(live) == 300, "Negative counters must not reduce distance");
        helper.succeed();
    }

    @GameTest(template = "empty")
    public static void logoutPreservesFinalLiveCounters(GameTestHelper helper) {
        var server = helper.getLevel().getServer();
        UUID id = UUID.randomUUID();
        ServerPlayer player = new ServerPlayer(server, helper.getLevel(), new GameProfile(id, "DistanceTest"));
        player.getStats().setValue(player, Stats.CUSTOM.get(Stats.WALK_ONE_CM), 123);
        player.getStats().setValue(player, Stats.CUSTOM.get(Stats.STRIDER_ONE_CM), 456);
        try (LeaderboardService service = new LeaderboardService(server, uuid -> true)) {
            service.logout(player);
            PlayerStats result = service.snapshot().players().stream().filter(p -> p.id().equals(id)).findFirst().orElseThrow();
            check(result.distanceCm() == 579, "Logout must retain the final live movement counters");
            check(!result.online() && !result.afk(), "Logged-out players must be offline without AFK");
        }
        helper.succeed();
    }
}
