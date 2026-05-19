package dev.zoffmann.playtimetracker;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class AfkTracker {
    public static final int AFK_THRESHOLD_SECONDS = 300;

    private static final class State {
        int x, y, z;
        int idleSeconds;
        boolean afk;
        boolean initialized;
    }

    private final Map<UUID, State> states = new HashMap<>();

    /** Call once per second per online player. @return true if AFK status changed. */
    public boolean tick(UUID id, int x, int y, int z) {
        State s = states.computeIfAbsent(id, k -> new State());
        if (!s.initialized) {
            s.x = x; s.y = y; s.z = z;
            s.idleSeconds = 0; s.afk = false; s.initialized = true;
            return false;
        }
        if (s.x == x && s.y == y && s.z == z) {
            s.idleSeconds++;
        } else {
            s.x = x; s.y = y; s.z = z;
            s.idleSeconds = 0;
        }
        boolean nowAfk = s.idleSeconds >= AFK_THRESHOLD_SECONDS;
        boolean changed = nowAfk != s.afk;
        s.afk = nowAfk;
        return changed;
    }

    public boolean isAfk(UUID id) {
        State s = states.get(id);
        return s != null && s.afk;
    }

    /** Reset baseline to a known position (used on join). */
    public void reset(UUID id, int x, int y, int z) {
        State s = new State();
        s.x = x; s.y = y; s.z = z; s.initialized = true;
        states.put(id, s);
    }

    public void remove(UUID id) { states.remove(id); }
}
