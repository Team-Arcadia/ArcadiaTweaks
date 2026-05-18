package com.teamarcadia.arcadiatweaks.neoforge.support;

public final class ArcadiaTickCoalescing {

    private ArcadiaTickCoalescing() {}

    public static Decision advance(int phase, int interval) {
        final int safeInterval = Math.max(1, interval);
        if (safeInterval <= 1) {
            return new Decision(true, 0);
        }
        final int nextPhase = Math.floorMod(phase + 1, safeInterval);
        return new Decision(nextPhase == 0, nextPhase);
    }

    public record Decision(boolean shouldRun, int nextPhase) {}
}
