package com.teamarcadia.arcadiatweaks.neoforge.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.refinedmods.refinedstorage.common.networking.NetworkTransmitterBlockEntity;
import com.refinedmods.refinedstorage.common.networking.NetworkTransmitterBlockEntityTicker;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.refinedstorage.ArcadiaRefinedStorageNodeState;
import com.teamarcadia.arcadiatweaks.neoforge.support.ArcadiaIntervalGate;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * S1b - Coalesce network transmitter block-state refresh.
 *
 * RS2 already rate-limits actual state writes to one per second, but still
 * calculates transmitter state every tick. That state lookup checks the graph
 * component for the remote receiver. Spark reports this specialized ticker
 * separately from the common network-node ticker.
 */
@Mixin(NetworkTransmitterBlockEntityTicker.class)
public abstract class NetworkTransmitterBlockEntityTickerMixin {

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/common/networking/NetworkTransmitterBlockEntity;updateStateInLevel(Lnet/minecraft/world/level/block/state/BlockState;)V"))
    private void arcadia$coalesceTransmitterStateCheck(
            NetworkTransmitterBlockEntity blockEntity,
            BlockState state,
            Operation<Void> original) {
        if (arcadia$shouldCheckTransmitterState(blockEntity)) {
            original.call(blockEntity, state);
        }
    }

    @Unique
    private static boolean arcadia$shouldCheckTransmitterState(NetworkTransmitterBlockEntity blockEntity) {
        if (!ArcadiaConfig.MODULE_REFINED_STORAGE_ENABLED.get()
                || !ArcadiaConfig.REFINED_STORAGE.activenessCheckCoalescing.get()) {
            return true;
        }
        final int interval = Math.max(1, ArcadiaConfig.REFINED_STORAGE.activenessCheckInterval.get());
        if (interval <= 1) {
            return true;
        }
        final ArcadiaRefinedStorageNodeState state = (ArcadiaRefinedStorageNodeState) blockEntity;
        final ArcadiaIntervalGate.Decision decision = ArcadiaIntervalGate.next(
                state.arcadia$getTransmitterStateCheckCooldown(),
                interval
        );
        state.arcadia$setTransmitterStateCheckCooldown(decision.nextCooldown());
        return decision.shouldRun();
    }
}
