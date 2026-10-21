package dev.zoffmann.playtimetracker.forge.leaderboard;

import com.google.gson.JsonParser;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import net.minecraft.network.FriendlyByteBuf;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LeaderboardTest {
    private static PlayerStats player(String name, long ticks, boolean online) {
        return new PlayerStats(UUID.randomUUID(), name, ticks, 7, 23, 456, 123456, 89, online, false);
    }

    @Test void readsExistingVanillaStatsAndAggregatesModdedBlocksAndTravel() {
        var root = JsonParser.parseString("""
                {"DataVersion":2975,"stats":{
                  "minecraft:custom":{"minecraft:play_time":133200,"minecraft:deaths":4,
                    "minecraft:mob_kills":12,"minecraft:walk_one_cm":250,"minecraft:sprint_one_cm":750,
                    "minecraft:fall_one_cm":2000,"minecraft:jump":56},
                  "minecraft:mined":{"minecraft:stone":100,"mod:ore":7},
                  "minecraft:crafted":{"minecraft:stone":999}}}
                """).getAsJsonObject();
        PlayerStats p = PlayerStats.fromJson(UUID.randomUUID(), "Alex", root);
        assertEquals(133200, p.playTicks());
        assertEquals(4, p.deaths());
        assertEquals(12, p.mobKills());
        assertEquals(107, p.minedBlocks());
        assertEquals(1000, p.distanceCm(), "Falling is not counted as travelled distance");
        assertEquals(56, p.jumps());
        assertFalse(p.online());
    }

    @Test void absentOrInvalidCountersDoNotBreakTheWholePlayerRecord() {
        PlayerStats empty = PlayerStats.fromJson(UUID.randomUUID(), "Alex", JsonParser.parseString("{}").getAsJsonObject());
        assertEquals(0, empty.playTicks());
        assertEquals(0, empty.minedBlocks());
        var malformed = JsonParser.parseString("""
                {"stats":{"minecraft:custom":{"minecraft:deaths":"broken","minecraft:play_time":-10},
                  "minecraft:mined":{"minecraft:stone":8,"mod:ore":null}}}
                """).getAsJsonObject();
        PlayerStats result = PlayerStats.fromJson(UUID.randomUUID(), "Alex", malformed);
        assertEquals(0, result.playTicks());
        assertEquals(0, result.deaths());
        assertEquals(8, result.minedBlocks());
    }

    @Test void sortsDescendingWithStableNameTiesAndSupportsSearchAndOnlineFilter() {
        PlayerStats alex = player("Alex", 72000, true);
        PlayerStats sam = player("Sam", 144000, false);
        PlayerStats bob = player("Bob", 72000, false);
        List<PlayerStats> all = List.of(bob, sam, alex);
        assertEquals(List.of(sam, alex, bob), Metric.PLAYTIME.sorted(all, "", false));
        assertEquals(List.of(alex), Metric.PLAYTIME.sorted(all, " AL ", false));
        assertEquals(List.of(alex), Metric.PLAYTIME.sorted(all, "", true));
        assertEquals(all, List.of(bob, sam, alex), "Sorting must not modify the server snapshot");
    }

    @Test void displaysPreciseMinutesAndReadableDistances() {
        PlayerStats alex = player("Alex", 133200, true);
        assertEquals("1h 51m", Metric.PLAYTIME.format(alex));
        assertEquals("1.23 km", Metric.DISTANCE.format(alex));
        assertEquals("456", Metric.BLOCKS.format(alex));
    }

    @Test void networkRoundTripPreservesOnlineAfkAndLargeCounters() {
        PlayerStats afk = new PlayerStats(UUID.randomUUID(), "Alex", 144000, 8, 90,
                8_000_000_000L, 9_000_000_000L, 12, true, true);
        var packet = new TrackerNetwork.Response(new LeaderboardService.Snapshot(List.of(afk, player("Offline", 72000, false)), true, 2), true);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TrackerNetwork.encode(packet, buffer);
            assertEquals(packet, TrackerNetwork.decode(buffer));
            assertEquals(0, buffer.readableBytes());
        } finally { buffer.release(); }
    }

    @Test void rejectsOversizedPacketDeclarationsBeforeAllocatingPlayerRows() {
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            buffer.writeBoolean(true).writeBoolean(true);
            buffer.writeVarInt(1000000).writeVarInt(1000000);
            assertThrows(IllegalArgumentException.class, () -> TrackerNetwork.decode(buffer));
        } finally { buffer.release(); }
    }

    @Test void maximumSnapshotFitsInOneForgeNetworkPayload() {
        List<PlayerStats> players = new ArrayList<>();
        for (int i = 0; i < LeaderboardService.MAX_PLAYERS; i++) players.add(player("Player" + i, Integer.MAX_VALUE, false));
        var packet = new TrackerNetwork.Response(new LeaderboardService.Snapshot(players, true, players.size()), false);
        FriendlyByteBuf buffer = new FriendlyByteBuf(Unpooled.buffer());
        try {
            TrackerNetwork.encode(packet, buffer);
            assertTrue(buffer.readableBytes() < 1024 * 1024);
            assertEquals(packet, TrackerNetwork.decode(buffer));
        } finally { buffer.release(); }
    }
}
