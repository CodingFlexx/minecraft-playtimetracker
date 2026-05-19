package dev.zoffmann.playtimetracker;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

class PlaytimeFormatterTest {
    private static long ticks(long seconds) { return seconds * 20L; }

    @Test void zeroPlaytime() {
        assertEquals(" [0h]", PlaytimeFormatter.hoursLabel(0L));
    }

    @Test void justUnderOneHourFloorsToZero() {
        assertEquals(" [0h]", PlaytimeFormatter.hoursLabel(ticks(3599)));
    }

    @Test void exactlyOneHour() {
        assertEquals(" [1h]", PlaytimeFormatter.hoursLabel(ticks(3600)));
    }

    @Test void justUnderTwoHoursFloorsToOne() {
        assertEquals(" [1h]", PlaytimeFormatter.hoursLabel(ticks(7199)));
    }

    @Test void largeValue() {
        assertEquals(" [5000h]", PlaytimeFormatter.hoursLabel(ticks(5000L * 3600L)));
    }
}
