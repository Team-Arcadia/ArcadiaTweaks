package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.mekanism.ArcadiaMekanismIdlePathBackoffState;
import it.unimi.dsi.fastutil.ints.Int2ObjectMap;
import mekanism.common.content.network.transmitter.LogisticalTransporterBase;
import mekanism.common.content.transporter.TransporterStack;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(LogisticalTransporterBase.class)
public abstract class LogisticalTransporterBaseMixin {

    @Shadow @Final protected Int2ObjectMap<TransporterStack> needsSync;

    @WrapOperation(method = "onUpdateServer",
            at = @At(value = "INVOKE", target = "Lmekanism/common/content/network/transmitter/LogisticalTransporterBase;recalculate(ILmekanism/common/content/transporter/TransporterStack;J)Z"))
    private boolean arcadia$backoffIdleRecalculate(
            LogisticalTransporterBase transporter,
            int id,
            TransporterStack stack,
            long originalLocation,
            Operation<Boolean> original
    ) {
        if (!(stack instanceof ArcadiaMekanismIdlePathBackoffState state) || !arcadia$enabled()) {
            arcadia$resetState(stack);
            return original.call(transporter, id, stack, originalLocation);
        }
        if (!arcadia$isIdleBackoffCandidate(stack)) {
            state.arcadia$resetIdlePathBackoff();
            return original.call(transporter, id, stack, originalLocation);
        }
        if (state.arcadia$consumeIdlePathCooldown()) {
            needsSync.put(id, stack);
            if (originalLocation != Long.MAX_VALUE) {
                stack.originalLocation = originalLocation;
            }
            return true;
        }

        final boolean result = original.call(transporter, id, stack, originalLocation);
        if (result && arcadia$isIdleBackoffCandidate(stack)) {
            state.arcadia$scheduleIdlePathBackoff(ArcadiaConfig.MEKANISM.logisticalTransporterIdleBackoffMaxTicks.get());
        } else {
            state.arcadia$resetIdlePathBackoff();
        }
        return result;
    }

    @Unique
    private static boolean arcadia$isIdleBackoffCandidate(TransporterStack stack) {
        return stack.initiatedPath
                && !stack.itemStack.isEmpty()
                && stack.hasPath()
                && stack.getPathType().noTarget();
    }

    @Unique
    private static void arcadia$resetState(TransporterStack stack) {
        if (stack instanceof ArcadiaMekanismIdlePathBackoffState state) {
            state.arcadia$resetIdlePathBackoff();
        }
    }

    @Unique
    private static boolean arcadia$enabled() {
        return ArcadiaConfig.MODULE_MEKANISM_ENABLED.get()
                && ArcadiaConfig.MEKANISM.logisticalTransporterIdleBackoff.get();
    }
}
