package com.teamarcadia.arcadiatweaks.neoforge.mixin.refinedstorage;

import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.refinedmods.refinedstorage.common.detector.DetectorBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity;
import com.refinedmods.refinedstorage.common.support.network.NetworkNodeBlockEntityTicker;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.refinedstorage.ArcadiaRefinedStorageNodeState;
import com.teamarcadia.arcadiatweaks.neoforge.support.ArcadiaIntervalGate;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

/**
 * S1 - Coalesce RS2 activeness recalculation.
 *
 * In Refined Storage 2.0.5 every network-node block entity runs:
 *   updateActiveness(state, activenessProperty);
 *   doWork();
 * every server tick. Most expensive node work is already throttled by RS or
 * guarded by AbstractNetworkNode.isActive(), but updateActiveness still checks
 * level-loaded state, redstone neighbor power and network energy every tick.
 *
 * This Mixin leaves doWork cadence intact and only skips the activeness
 * recalculation between configured intervals. Each block entity stores its own
 * cooldown, so dense networks are not biased by stable chunk iteration order.
 */
@Mixin(NetworkNodeBlockEntityTicker.class)
public abstract class NetworkNodeBlockEntityTickerMixin {

    @WrapOperation(method = "tick",
            at = @At(value = "INVOKE",
                    target = "Lcom/refinedmods/refinedstorage/common/support/network/AbstractBaseNetworkNodeContainerBlockEntity;updateActiveness(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/properties/BooleanProperty;)V"))
    private void arcadia$coalesceActivenessCheck(
            AbstractBaseNetworkNodeContainerBlockEntity<?> blockEntity,
            BlockState state,
            BooleanProperty activenessProperty,
            Operation<Void> original) {
        if (arcadia$shouldCheckActiveness(blockEntity)) {
            original.call(blockEntity, state, activenessProperty);
        }
    }

    @Unique
    private static boolean arcadia$shouldCheckActiveness(AbstractBaseNetworkNodeContainerBlockEntity<?> blockEntity) {
        if (blockEntity instanceof DetectorBlockEntity) {
            return true;
        }
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
                state.arcadia$getActivenessCheckCooldown(),
                interval
        );
        state.arcadia$setActivenessCheckCooldown(decision.nextCooldown());
        return decision.shouldRun();
    }
}
