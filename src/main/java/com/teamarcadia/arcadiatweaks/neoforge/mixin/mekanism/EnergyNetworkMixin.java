package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoff;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoffState;
import mekanism.common.capabilities.energy.VariableCapacityEnergyContainer;
import mekanism.common.content.network.EnergyNetwork;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(EnergyNetwork.class)
public abstract class EnergyNetworkMixin implements ArcadiaMekanismBackoffState {

    @Shadow @Final public VariableCapacityEnergyContainer energyContainer;
    @Shadow private long prevTransferAmount;

    @Unique private int arcadia$emitCooldown;
    @Unique private int arcadia$nextEmitBackoff = 1;
    @Unique private boolean arcadia$skippedEmit;

    @WrapOperation(method = "onUpdate",
            at = @At(value = "INVOKE", target = "Lmekanism/common/content/network/EnergyNetwork;tickEmit(J)J"))
    private long arcadia$backoffEmptyEmit(EnergyNetwork network, long energyToSend, Operation<Long> original) {
        if (!arcadia$enabled()) {
            arcadia$resetBackoff();
            return original.call(network, energyToSend);
        }
        if (arcadia$emitCooldown > 0) {
            arcadia$emitCooldown--;
            arcadia$skippedEmit = true;
            return 0L;
        }
        return original.call(network, energyToSend);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void arcadia$updateBackoff(CallbackInfo ci) {
        if (!arcadia$enabled() || energyContainer.isEmpty() || prevTransferAmount > 0L) {
            arcadia$resetBackoff();
            return;
        }
        if (arcadia$skippedEmit) {
            arcadia$skippedEmit = false;
            return;
        }
        arcadia$scheduleBackoff();
    }

    @Inject(method = "onContentsChanged", at = @At("HEAD"))
    private void arcadia$resetBackoffOnContentsChanged(CallbackInfo ci) {
        arcadia$resetBackoff();
    }

    @Override
    public void arcadia$resetBackoff() {
        arcadia$emitCooldown = 0;
        arcadia$nextEmitBackoff = 1;
        arcadia$skippedEmit = false;
    }

    @Unique
    private void arcadia$scheduleBackoff() {
        final int maxTicks = ArcadiaConfig.MEKANISM.networkEmitBackoffMaxTicks.get();
        arcadia$emitCooldown = ArcadiaMekanismBackoff.nextDelay(arcadia$nextEmitBackoff, maxTicks);
        arcadia$nextEmitBackoff = ArcadiaMekanismBackoff.growDelay(arcadia$nextEmitBackoff, maxTicks);
    }

    @Unique
    private static boolean arcadia$enabled() {
        return ArcadiaConfig.MODULE_MEKANISM_ENABLED.get()
                && ArcadiaConfig.MEKANISM.networkEmitBackoff.get();
    }
}
