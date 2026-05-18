package com.teamarcadia.arcadiatweaks.neoforge.mekanism;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

final class ArcadiaMekanismBackoffTest {

    @Test
    void nextDelayNeverReturnsLessThanOne() {
        assertEquals(1, ArcadiaMekanismBackoff.nextDelay(0, 20));
        assertEquals(1, ArcadiaMekanismBackoff.nextDelay(-5, 20));
        assertEquals(1, ArcadiaMekanismBackoff.nextDelay(0, 0));
    }

    @Test
    void nextDelayCapsToConfiguredMaximum() {
        assertEquals(5, ArcadiaMekanismBackoff.nextDelay(5, 20));
        assertEquals(20, ArcadiaMekanismBackoff.nextDelay(40, 20));
        assertEquals(1, ArcadiaMekanismBackoff.nextDelay(40, -1));
    }

    @Test
    void growDelayDoublesAndCaps() {
        assertEquals(2, ArcadiaMekanismBackoff.growDelay(1, 20));
        assertEquals(8, ArcadiaMekanismBackoff.growDelay(4, 20));
        assertEquals(20, ArcadiaMekanismBackoff.growDelay(16, 20));
        assertEquals(1, ArcadiaMekanismBackoff.growDelay(-5, 1));
    }
}
