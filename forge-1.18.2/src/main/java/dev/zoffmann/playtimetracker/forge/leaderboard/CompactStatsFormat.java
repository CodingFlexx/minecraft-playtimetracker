package dev.zoffmann.playtimetracker.forge.leaderboard;

import java.text.NumberFormat;
import java.util.Locale;

/** Short inventory values; full counters remain available in tooltips. */
public final class CompactStatsFormat {
    public static String format(Metric metric, PlayerStats player, Locale locale) {
        long value = metric.value(player);
        if (metric == Metric.PLAYTIME) return String.format(Locale.ROOT, "%dh%02dm", value / 72000, value / 1200 % 60);
        if (metric == Metric.DISTANCE) return String.format(locale, "%.1f km", value / 100000.0);
        if (value < 1_000_000) return NumberFormat.getIntegerInstance(locale).format(value);
        String[] units = locale.getLanguage().equals("de")
                ? new String[]{"Mio", "Mrd", "Bio", "Brd", "Trl"} : new String[]{"M", "B", "T", "Q", "Qi"};
        double scaled = value / 1_000_000.0;
        int unit = 0;
        while (scaled >= 1000 && unit + 1 < units.length) { scaled /= 1000; unit++; }
        return String.format(locale, "%.1f %s", scaled, units[unit]);
    }
    private CompactStatsFormat() {}
}
