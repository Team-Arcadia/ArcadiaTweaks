package com.teamarcadia.arcadiatweaks.neoforge.mekanism;

public final class ArcadiaMekanismBackoff {

    private ArcadiaMekanismBackoff() {}

    public static int nextDelay(int currentNextDelay, int maxTicks) {
        return Math.max(1, Math.min(currentNextDelay, Math.max(1, maxTicks)));
    }

    public static int growDelay(int currentDelay, int maxTicks) {
        final int next = Math.max(1, currentDelay) * 2;
        return Math.min(next, Math.max(1, maxTicks));
    }
}
