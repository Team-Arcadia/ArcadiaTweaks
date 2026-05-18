package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoff;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismBackoffState;
import mekanism.common.capabilities.fluid.VariableCapacityFluidTank;
import mekanism.common.content.network.FluidNetwork;
import net.neoforged.neoforge.fluids.FluidStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(FluidNetwork.class)
public abstract class FluidNetworkMixin implements ArcadiaMekanismBackoffState {

    @Shadow @Final public VariableCapacityFluidTank fluidTank;
    @Shadow private int prevTransferAmount;

    @Unique private int arcadia$emitCooldown;
    @Unique private int arcadia$nextEmitBackoff = 1;
    @Unique private boolean arcadia$skippedEmit;

    @WrapOperation(method = "onUpdate",
            at = @At(value = "INVOKE", target = "Lmekanism/common/content/network/FluidNetwork;tickEmit(Lnet/neoforged/neoforge/fluids/FluidStack;)I"))
    private int arcadia$backoffEmptyEmit(FluidNetwork network, FluidStack fluidToSend, Operation<Integer> original) {
        if (!arcadia$enabled()) {
            arcadia$resetBackoff();
            return original.call(network, fluidToSend);
        }
        if (arcadia$emitCooldown > 0) {
            arcadia$emitCooldown--;
            arcadia$skippedEmit = true;
            return 0;
        }
        return original.call(network, fluidToSend);
    }

    @Inject(method = "onUpdate", at = @At("RETURN"))
    private void arcadia$updateBackoff(CallbackInfo ci) {
        if (!arcadia$enabled() || fluidTank.isEmpty() || prevTransferAmount > 0) {
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
