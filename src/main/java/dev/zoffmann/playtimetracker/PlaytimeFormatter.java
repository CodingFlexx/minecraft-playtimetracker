package dev.zoffmann.playtimetracker;

public final class PlaytimeFormatter {
    private PlaytimeFormatter() {}

    /** play_time is stored in game ticks (20 per second). Floors to whole hours. */
    public static String hoursLabel(long playTimeTicks) {
        long hours = playTimeTicks / 20L / 3600L;
        return " [" + hours + "h]";
    }
}
