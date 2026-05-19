package dev.zoffmann.playtimetracker;

import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class AfkTrackerTest {
    private final UUID id = UUID.randomUUID();

    @Test void firstTickInitializesAndIsNotAfk() {
        AfkTracker t = new AfkTracker();
        assertFalse(t.tick(id, 0, 64, 0));
        assertFalse(t.isAfk(id));
    }

    @Test void becomesAfkExactlyAtThreshold() {
        AfkTracker t = new AfkTracker();
        t.tick(id, 0, 64, 0); // init
        boolean changed = false;
        for (int s = 1; s <= AfkTracker.AFK_THRESHOLD_SECONDS; s++) {
            changed = t.tick(id, 0, 64, 0);
        }
        assertTrue(changed, "AFK state should flip on the 300th idle second");
        assertTrue(t.isAfk(id));
    }

    @Test void notAfkOneSecondBeforeThreshold() {
        AfkTracker t = new AfkTracker();
        t.tick(id, 0, 64, 0);
        for (int s = 1; s <= AfkTracker.AFK_THRESHOLD_SECONDS - 1; s++) {
            assertFalse(t.tick(id, 0, 64, 0));
        }
        assertFalse(t.isAfk(id));
    }

    @Test void movementResetsAndClearsAfkWithStateChange() {
        AfkTracker t = new AfkTracker();
        t.tick(id, 0, 64, 0);
        for (int s = 1; s <= AfkTracker.AFK_THRESHOLD_SECONDS; s++) t.tick(id, 0, 64, 0);
        assertTrue(t.isAfk(id));
        assertTrue(t.tick(id, 1, 64, 0), "moving while AFK flips state back");
        assertFalse(t.isAfk(id));
    }

    @Test void removeForgetsPlayer() {
        AfkTracker t = new AfkTracker();
        t.tick(id, 0, 64, 0);
        t.remove(id);
        assertFalse(t.isAfk(id));
    }
}
