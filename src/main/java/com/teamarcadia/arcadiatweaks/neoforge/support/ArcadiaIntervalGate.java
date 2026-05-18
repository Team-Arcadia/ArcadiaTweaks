package com.teamarcadia.arcadiatweaks.neoforge.support;

public final class ArcadiaIntervalGate {

    private ArcadiaIntervalGate() {}

    public static Decision next(int cooldown, int interval) {
        final int safeInterval = Math.max(1, interval);
        if (safeInterval <= 1) {
            return new Decision(true, 0);
        }
        if (cooldown <= 0) {
            return new Decision(true, safeInterval - 1);
        }
        return new Decision(false, cooldown - 1);
    }

    public record Decision(boolean shouldRun, int nextCooldown) {}
}
