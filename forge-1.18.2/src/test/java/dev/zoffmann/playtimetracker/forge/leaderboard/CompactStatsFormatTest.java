package dev.zoffmann.playtimetracker.forge.leaderboard;

import java.util.Locale;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CompactStatsFormatTest {
    private static PlayerStats player(long ticks, long blocks, long distance) {
        return new PlayerStats(new UUID(0, 1), "Alex", ticks, 0, 0, blocks, distance, 0, true, false);
    }

    @Test void minuteAndHourBoundariesRemainFlooredAndZeroPadded() {
        assertEquals("0h00m", CompactStatsFormat.format(Metric.PLAYTIME, player(1199, 0, 0), Locale.GERMANY));
        assertEquals("0h01m", CompactStatsFormat.format(Metric.PLAYTIME, player(1200, 0, 0), Locale.GERMANY));
        assertEquals("0h59m", CompactStatsFormat.format(Metric.PLAYTIME, player(71999, 0, 0), Locale.GERMANY));
        assertEquals("1h00m", CompactStatsFormat.format(Metric.PLAYTIME, player(72000, 0, 0), Locale.GERMANY));
        assertEquals("128h42m", CompactStatsFormat.format(Metric.PLAYTIME, player((128 * 60L + 42) * 1200, 0, 0), Locale.US));
    }

    @Test void largeLocalizedValuesFitCompactRowsWithoutChangingCounters() {
        PlayerStats counts = player(0, 8_000_000_000L, 14260000);
        assertEquals("8,0 Mrd", CompactStatsFormat.format(Metric.BLOCKS, counts, Locale.GERMANY));
        assertEquals("8.0 B", CompactStatsFormat.format(Metric.BLOCKS, counts, Locale.US));
        assertEquals("142,6 km", CompactStatsFormat.format(Metric.DISTANCE, counts, Locale.GERMANY));
        assertEquals("142.6 km", CompactStatsFormat.format(Metric.DISTANCE, counts, Locale.US));
        assertEquals(8_000_000_000L, counts.minedBlocks());
        assertEquals("8,000,000,000", Metric.BLOCKS.format(counts), "Full values remain available for the tooltip");
    }

    @Test void fullThousandsAreKeptBelowTheMillionBoundary() {
        assertEquals("999.999", CompactStatsFormat.format(Metric.BLOCKS, player(0, 999999, 0), Locale.GERMANY));
        assertEquals("1,0 Mio", CompactStatsFormat.format(Metric.BLOCKS, player(0, 1000000, 0), Locale.GERMANY));
    }
}
