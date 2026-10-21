package dev.zoffmann.playtimetracker.forge.leaderboard;

import com.google.gson.JsonObject;
import java.util.UUID;

/** Public game statistics only; no inventory, position, IP or private player data. */
public record PlayerStats(UUID id, String name, long playTicks, long deaths, long mobKills,
                          long minedBlocks, long distanceCm, long jumps, boolean online, boolean afk) {
    public PlayerStats {
        playTicks = Math.max(0, playTicks);
        deaths = Math.max(0, deaths);
        mobKills = Math.max(0, mobKills);
        minedBlocks = Math.max(0, minedBlocks);
        distanceCm = Math.max(0, distanceCm);
        jumps = Math.max(0, jumps);
    }

    public static PlayerStats fromJson(UUID id, String name, JsonObject root) {
        JsonObject stats = object(root, "stats");
        JsonObject custom = object(stats, "minecraft:custom");
        long distance = 0;
        for (String stat : new String[]{"walk_one_cm", "sprint_one_cm", "crouch_one_cm", "swim_one_cm",
                "walk_on_water_one_cm", "walk_under_water_one_cm", "fly_one_cm", "aviate_one_cm",
                "boat_one_cm", "minecart_one_cm", "horse_one_cm", "pig_one_cm", "strider_one_cm"}) {
            distance += number(custom, "minecraft:" + stat);
        }
        long mined = 0;
        JsonObject blocks = object(stats, "minecraft:mined");
        for (String block : blocks.keySet()) mined += number(blocks, block);
        return new PlayerStats(id, name, number(custom, "minecraft:play_time"), number(custom, "minecraft:deaths"),
                number(custom, "minecraft:mob_kills"), mined, distance, number(custom, "minecraft:jump"), false, false);
    }

    private static JsonObject object(JsonObject parent, String name) {
        return parent.has(name) && parent.get(name).isJsonObject() ? parent.getAsJsonObject(name) : new JsonObject();
    }

    private static long number(JsonObject parent, String name) {
        try { return Math.max(0, Math.min(Integer.MAX_VALUE, parent.get(name).getAsLong())); }
        catch (RuntimeException exception) { return 0; }
    }

    public PlayerStats named(String name) {
        return new PlayerStats(id, name, playTicks, deaths, mobKills, minedBlocks, distanceCm, jumps, online, afk);
    }
}
