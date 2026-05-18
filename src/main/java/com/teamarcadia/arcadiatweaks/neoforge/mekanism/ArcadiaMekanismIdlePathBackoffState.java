package com.teamarcadia.arcadiatweaks.neoforge.mekanism;

public interface ArcadiaMekanismIdlePathBackoffState {

    boolean arcadia$consumeIdlePathCooldown();

    void arcadia$scheduleIdlePathBackoff(int maxTicks);

    void arcadia$resetIdlePathBackoff();

    int arcadia$getIdlePathCooldown();

    int arcadia$getNextIdlePathBackoff();
}
