package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoff;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismIdlePathBackoffState;
import mekanism.common.content.transporter.TransporterStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

@Mixin(TransporterStack.class)
public abstract class TransporterStackMixin implements ArcadiaMekanismIdlePathBackoffState {

    @Unique private int arcadia$idlePathCooldown;
    @Unique private int arcadia$nextIdlePathBackoff = 1;

    @Unique
    @Override
    public boolean arcadia$consumeIdlePathCooldown() {
        if (arcadia$idlePathCooldown <= 0) {
            return false;
        }
        arcadia$idlePathCooldown--;
        return true;
    }

    @Unique
    @Override
    public void arcadia$scheduleIdlePathBackoff(int maxTicks) {
        arcadia$idlePathCooldown = ArcadiaMekanismBackoff.nextDelay(arcadia$nextIdlePathBackoff, maxTicks);
        arcadia$nextIdlePathBackoff = ArcadiaMekanismBackoff.growDelay(arcadia$nextIdlePathBackoff, maxTicks);
    }

    @Unique
    @Override
    public void arcadia$resetIdlePathBackoff() {
        arcadia$idlePathCooldown = 0;
        arcadia$nextIdlePathBackoff = 1;
    }

    @Unique
    @Override
    public int arcadia$getIdlePathCooldown() {
        return arcadia$idlePathCooldown;
    }

    @Unique
    @Override
    public int arcadia$getNextIdlePathBackoff() {
        return arcadia$nextIdlePathBackoff;
    }
}
