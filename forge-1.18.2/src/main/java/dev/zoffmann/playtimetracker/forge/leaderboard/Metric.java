package dev.zoffmann.playtimetracker.forge.leaderboard;

import java.text.NumberFormat;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.ToLongFunction;

public enum Metric {
    PLAYTIME("playtime", PlayerStats::playTicks),
    DEATHS("deaths", PlayerStats::deaths),
    MOB_KILLS("mob_kills", PlayerStats::mobKills),
    BLOCKS("blocks", PlayerStats::minedBlocks),
    DISTANCE("distance", PlayerStats::distanceCm),
    JUMPS("jumps", PlayerStats::jumps);

    private final String key;
    private final ToLongFunction<PlayerStats> value;
    Metric(String key, ToLongFunction<PlayerStats> value) { this.key = key; this.value = value; }
    public String translationKey() { return "playtimetracker.metric." + key; }
    public long value(PlayerStats player) { return value.applyAsLong(player); }

    public String format(PlayerStats player) {
        long number = value(player);
        if (this == PLAYTIME) return number / 72000 + "h " + (number / 1200 % 60) + "m";
        if (this == DISTANCE) return String.format(Locale.ROOT, "%.2f km", number / 100000.0);
        return NumberFormat.getIntegerInstance(Locale.ROOT).format(number);
    }

    public List<PlayerStats> sorted(List<PlayerStats> players, String search, boolean onlyOnline) {
        String query = search.strip().toLowerCase(Locale.ROOT);
        return players.stream().filter(p -> !onlyOnline || p.online())
                .filter(p -> p.name().toLowerCase(Locale.ROOT).contains(query))
                .sorted(Comparator.comparingLong(this::value).reversed()
                        .thenComparing(PlayerStats::name, String.CASE_INSENSITIVE_ORDER)
                        .thenComparing(PlayerStats::id))
                .toList();
    }
}
