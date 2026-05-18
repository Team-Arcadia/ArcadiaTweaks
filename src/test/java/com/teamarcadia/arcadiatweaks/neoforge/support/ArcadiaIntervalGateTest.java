package com.teamarcadia.arcadiatweaks.neoforge.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArcadiaIntervalGateTest {

    @Test
    void intervalOneAlwaysRuns() {
        final ArcadiaIntervalGate.Decision decision = ArcadiaIntervalGate.next(99, 1);

        assertTrue(decision.shouldRun());
        assertEquals(0, decision.nextCooldown());
    }

    @Test
    void intervalFourRunsOnceThenSkipsThreeTicks() {
        int cooldown = 0;
        final boolean[] pattern = new boolean[8];

        for (int i = 0; i < pattern.length; i++) {
            final ArcadiaIntervalGate.Decision decision = ArcadiaIntervalGate.next(cooldown, 4);
            pattern[i] = decision.shouldRun();
            cooldown = decision.nextCooldown();
        }

        assertArrayEquals(new boolean[] {true, false, false, false, true, false, false, false}, pattern);
    }
}
