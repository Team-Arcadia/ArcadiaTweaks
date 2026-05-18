package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoff;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismPullBackoffState;
import mekanism.api.chemical.IChemicalTank;
import mekanism.common.content.network.ChemicalNetwork;
import mekanism.common.content.network.transmitter.PressurizedTube;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PressurizedTube.class)
public abstract class PressurizedTubeMixin implements ArcadiaMekanismPullBackoffState {

    @Shadow @Final public IChemicalTank chemicalTank;

    @Unique private int arcadia$pullCooldown;
    @Unique private int arcadia$nextPullBackoff = 1;
    @Unique private long arcadia$storedBeforePull;
    @Unique private boolean arcadia$trackedPull;

    @Inject(method = "pullFromAcceptors", at = @At("HEAD"), cancellable = true)
    private void arcadia$maybeSkipPull(CallbackInfo ci) {
        if (!arcadia$enabled()) {
            arcadia$resetPullBackoff();
            return;
        }
        if (arcadia$pullCooldown > 0) {
            arcadia$pullCooldown--;
            ci.cancel();
            return;
        }
        arcadia$storedBeforePull = arcadia$stored();
        arcadia$trackedPull = true;
    }

    @Inject(method = "pullFromAcceptors", at = @At("RETURN"))
    private void arcadia$updatePullBackoff(CallbackInfo ci) {
        if (!arcadia$trackedPull) {
            return;
        }
        arcadia$trackedPull = false;
        if (!arcadia$enabled() || arcadia$stored() > arcadia$storedBeforePull) {
            arcadia$resetPullBackoff();
            return;
        }
        arcadia$schedulePullBackoff();
    }

    @Unique
    @Override
    public int arcadia$getPullCooldown() {
        return arcadia$pullCooldown;
    }

    @Unique
    @Override
    public int arcadia$getNextPullBackoff() {
        return arcadia$nextPullBackoff;
    }

    @Unique
    private long arcadia$stored() {
        long stored = chemicalTank.getStored();
        final PressurizedTube tube = (PressurizedTube) (Object) this;
        if (tube.hasTransmitterNetwork()) {
            stored += ((ChemicalNetwork) tube.getTransmitterNetwork()).chemicalTank.getStored();
        }
        return stored;
    }

    @Unique
    private void arcadia$resetPullBackoff() {
        arcadia$pullCooldown = 0;
        arcadia$nextPullBackoff = 1;
    }

    @Unique
    private void arcadia$schedulePullBackoff() {
        final int maxTicks = ArcadiaConfig.MEKANISM.transmitterPullBackoffMaxTicks.get();
        arcadia$pullCooldown = ArcadiaMekanismBackoff.nextDelay(arcadia$nextPullBackoff, maxTicks);
        arcadia$nextPullBackoff = ArcadiaMekanismBackoff.growDelay(arcadia$nextPullBackoff, maxTicks);
    }

    @Unique
    private static boolean arcadia$enabled() {
        return ArcadiaConfig.MODULE_MEKANISM_ENABLED.get()
                && ArcadiaConfig.MEKANISM.transmitterPullBackoff.get();
    }
}
