package com.teamarcadia.arcadiatweaks.neoforge.support;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class ArcadiaTickCoalescingTest {

    @Test
    void intervalOneAlwaysRunsAndResetsPhase() {
        final ArcadiaTickCoalescing.Decision decision = ArcadiaTickCoalescing.advance(7, 1);

        assertTrue(decision.shouldRun());
        assertEquals(0, decision.nextPhase());
    }

    @Test
    void intervalFourSkipsThreeTicksThenRunsOneTick() {
        int phase = 0;
        final boolean[] pattern = new boolean[8];

        for (int i = 0; i < pattern.length; i++) {
            final ArcadiaTickCoalescing.Decision decision = ArcadiaTickCoalescing.advance(phase, 4);
            pattern[i] = decision.shouldRun();
            phase = decision.nextPhase();
        }

        assertArrayEquals(new boolean[] {false, false, false, true, false, false, false, true}, pattern);
    }
}
