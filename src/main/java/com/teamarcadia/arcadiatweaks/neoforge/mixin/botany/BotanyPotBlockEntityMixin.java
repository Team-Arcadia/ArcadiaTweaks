package com.teamarcadia.arcadiatweaks.neoforge.mixin.botany;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.llamalad7.mixinextras.injector.wrapoperation.WrapOperation;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.neoforge.botany.ArcadiaPotState;
import net.darkhax.bookshelf.common.api.function.ReloadableCache;
import net.darkhax.bookshelf.common.api.util.IGameplayHelper;
import net.darkhax.botanypots.common.api.context.BotanyPotContext;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Botany module Mixins on BotanyPotBlockEntity.
 *
 * S1 (matches() memoization) - lines: getOrInvalidateSoil / getOrInvalidateCrop wraps.
 * Caches the result of the matches() check until the slot stack, the BlockState, or
 * the underlying RecipeHolder identity changes. Slot mutations route through
 * onSoilChanged/onSeedChanged -> reset(), so we hook reset() HEAD as the single
 * source of truth for invalidation. A bounded TTL (safety_revalidate_period_ticks)
 * is a worst-case ceiling for stale state.
 *
 * S3 (hopper export backoff) - wraps the getBlockState(below) lookup at the head
 * of the export branch and returns AIR while in backoff so the existing isAir()
 * check skips the slot scan entirely. Failure tracking lives in a per-tick
 * counter pair, updated by wrapping inventoryInsert; once a tick attempted at
 * least one insert and zero succeeded, the next call gets put in backoff with
 * exponential schedule (min, 2*min, 4*min ... capped at max). A successful
 * insert resets the schedule. The backoff is also dropped on reset() because
 * downstream state may have changed by the time the pot was emptied.
 */
@Mixin(BotanyPotBlockEntity.class)
public abstract class BotanyPotBlockEntityMixin implements ArcadiaPotState {

    @Shadow @Final private ReloadableCache<RecipeHolder<Soil>> soil;
    @Shadow @Final private ReloadableCache<RecipeHolder<Crop>> crop;

    // getSoilItem/getSeedItem live on the abstract superclass; @Shadow can only
    // bind to methods declared on the target class itself, so we cast through
    // AbstractBotanyPotBlockEntity at the call site instead.

    // S1 state
    @Unique private RecipeHolder<Soil> arcadia$lastSoilHolder;
    @Unique private RecipeHolder<Crop> arcadia$lastCropHolder;
    @Unique private BlockState         arcadia$lastSoilState;
    @Unique private BlockState         arcadia$lastCropState;
    @Unique private int                arcadia$soilTtl;
    @Unique private int                arcadia$cropTtl;

    // S3 state - exposed via ArcadiaPotState so static handlers can read/write
    // them through (ArcadiaPotState) pot. See the interface for rationale.
    @Unique private int                arcadia$hopperBackoffRemaining;
    @Unique private int                arcadia$consecutiveExportFailures;
    @Unique private int                arcadia$tickInsertAttempts;
    @Unique private int                arcadia$tickInsertSuccesses;

    // A1 state - cached result of Helpers.getRequiredGrowthTicks.
    @Unique private int                arcadia$cachedRequiredTicks;
    @Unique private int                arcadia$requiredTicksRemaining;

    @Override @Unique public int  arcadia$getHopperBackoff() { return arcadia$hopperBackoffRemaining; }
    @Override @Unique public void arcadia$setHopperBackoff(int value) { arcadia$hopperBackoffRemaining = value; }

    @Override @Unique public int  arcadia$getConsecutiveFailures() { return arcadia$consecutiveExportFailures; }
    @Override @Unique public void arcadia$setConsecutiveFailures(int value) { arcadia$consecutiveExportFailures = value; }

    @Override @Unique public int  arcadia$getTickInsertAttempts() { return arcadia$tickInsertAttempts; }
    @Override @Unique public void arcadia$setTickInsertAttempts(int value) { arcadia$tickInsertAttempts = value; }
    @Override @Unique public void arcadia$incrementTickInsertAttempts() { arcadia$tickInsertAttempts++; }

    @Override @Unique public int  arcadia$getTickInsertSuccesses() { return arcadia$tickInsertSuccesses; }
    @Override @Unique public void arcadia$setTickInsertSuccesses(int value) { arcadia$tickInsertSuccesses = value; }
    @Override @Unique public void arcadia$incrementTickInsertSuccesses() { arcadia$tickInsertSuccesses++; }

    @Override @Unique public int  arcadia$getCachedRequiredTicks() { return arcadia$cachedRequiredTicks; }
    @Override @Unique public void arcadia$setCachedRequiredTicks(int value) { arcadia$cachedRequiredTicks = value; }

    @Override @Unique public int  arcadia$getRequiredTicksRemaining() { return arcadia$requiredTicksRemaining; }
    @Override @Unique public void arcadia$setRequiredTicksRemaining(int value) { arcadia$requiredTicksRemaining = value; }
    @Override @Unique public void arcadia$decrementRequiredTicksRemaining() { arcadia$requiredTicksRemaining--; }

    @Inject(method = "reset", at = @At("HEAD"))
    private void arcadia$invalidateMatchCaches(CallbackInfo ci) {
        arcadia$lastSoilHolder = null;
        arcadia$lastCropHolder = null;
        arcadia$lastSoilState = null;
        arcadia$lastCropState = null;
        arcadia$soilTtl = 0;
        arcadia$cropTtl = 0;
        arcadia$hopperBackoffRemaining = 0;
        arcadia$consecutiveExportFailures = 0;
        arcadia$requiredTicksRemaining = 0;
    }

    /**
     * A1 - tool changes do not flow through reset() (vanilla onToolChanged
     * only calls markUpdated()), so we hook it explicitly to drop the cached
     * required-ticks value. Tool item / its enchantments contribute to
     * Helpers.efficiencyModifier inside getRequiredGrowthTicks.
     */
    @Inject(method = "onToolChanged", at = @At("HEAD"))
    private void arcadia$invalidateA1OnTool(ItemStack newStack, CallbackInfo ci) {
        arcadia$requiredTicksRemaining = 0;
    }

    /**
     * S3 entry point - the hopper branch reads the block below via
     * {@code serverLevel.getBlockState(pot.below.get())} and skips the slot
     * scan when the block is air. By returning AIR while we are in backoff,
     * the existing branch turns into a no-op without us having to inject
     * cancellation logic mid-method.
     *
     * As a side-effect this is the natural place to reset the per-tick
     * insert counters used by {@link #arcadia$adjustHopperBackoff}.
     */
    @WrapOperation(method = "tickPot",
            at = @At(value = "INVOKE",
                     target = "Lnet/minecraft/server/level/ServerLevel;getBlockState(Lnet/minecraft/core/BlockPos;)Lnet/minecraft/world/level/block/state/BlockState;"))
    private static BlockState arcadia$gateHopperBackoff(
            ServerLevel level, BlockPos pos, Operation<BlockState> original,
            Level levelArg, BlockPos posArg, BlockState stateArg, BotanyPotBlockEntity pot) {
        if (!ArcadiaConfig.BOTANY.s3HopperBackoff.get()) {
            return original.call(level, pos);
        }
        final ArcadiaPotState state = (ArcadiaPotState) pot;
        state.arcadia$setTickInsertAttempts(0);
        state.arcadia$setTickInsertSuccesses(0);
        final int remaining = state.arcadia$getHopperBackoff();
        if (remaining > 0) {
            state.arcadia$setHopperBackoff(remaining - 1);
            return Blocks.AIR.defaultBlockState();
        }
        return original.call(level, pos);
    }

    /**
     * S3 measurement - records how many of this tick's inventoryInsert calls
     * actually moved items. A call counts as success if the returned stack
     * size is smaller than the input (the inventory accepted at least one
     * item).
     */
    @WrapOperation(method = "tickPot",
            at = @At(value = "INVOKE",
                     target = "Lnet/darkhax/bookshelf/common/api/util/IGameplayHelper;inventoryInsert(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/core/BlockPos;Lnet/minecraft/core/Direction;Lnet/minecraft/world/item/ItemStack;)Lnet/minecraft/world/item/ItemStack;"))
    private static ItemStack arcadia$trackHopperInsert(
            IGameplayHelper svc, ServerLevel level, BlockPos pos, Direction dir, ItemStack stack, Operation<ItemStack> original,
            Level levelArg, BlockPos posArg, BlockState stateArg, BotanyPotBlockEntity pot) {
        final ItemStack result = original.call(svc, level, pos, dir, stack);
        if (ArcadiaConfig.BOTANY.s3HopperBackoff.get()) {
            final ArcadiaPotState state = (ArcadiaPotState) pot;
            state.arcadia$incrementTickInsertAttempts();
            if (result.getCount() < stack.getCount()) {
                state.arcadia$incrementTickInsertSuccesses();
            }
        }
        return result;
    }

    /**
     * S3 decision - runs right after exportCooldown.reset() (the tail of the
     * hopper branch). If we attempted any insert this tick and none made
     * progress, schedule an exponential backoff. A successful insert (or
     * "no inserts attempted at all", which means slots were empty) clears
     * the failure counter without scheduling backoff.
     */
    @Inject(method = "tickPot",
            at = @At(value = "INVOKE",
                     target = "Lnet/darkhax/bookshelf/common/api/util/TickAccumulator;reset()V",
                     ordinal = 1,
                     shift = At.Shift.AFTER))
    private static void arcadia$adjustHopperBackoff(
            Level levelArg, BlockPos posArg, BlockState stateArg, BotanyPotBlockEntity pot,
            CallbackInfo ci) {
        if (!ArcadiaConfig.BOTANY.s3HopperBackoff.get()) return;
        final ArcadiaPotState state = (ArcadiaPotState) pot;
        if (state.arcadia$getHopperBackoff() > 0) return;

        final int attempts  = state.arcadia$getTickInsertAttempts();
        final int successes = state.arcadia$getTickInsertSuccesses();
        if (attempts == 0) return;

        if (successes == 0) {
            final int failures = state.arcadia$getConsecutiveFailures() + 1;
            state.arcadia$setConsecutiveFailures(failures);
            final int min = ArcadiaConfig.BOTANY.s3HopperBackoffMinTicks.get();
            final int max = ArcadiaConfig.BOTANY.s3HopperBackoffMaxTicks.get();
            final int shift = Math.min(failures - 1, 8);
            state.arcadia$setHopperBackoff(Math.min(min << shift, max));
        } else {
            state.arcadia$setConsecutiveFailures(0);
        }
    }

    /**
     * A1 - memoize Helpers.getRequiredGrowthTicks. The result depends on
     * (cropRecipeId, soilRecipeId, harvestItem-as-tool, BlockState, config),
     * all invariant between slot changes. Slot changes route through reset()
     * (soil/seed) or onToolChanged (tool) - both invalidate the cache. The
     * safety_revalidate_period_ticks ceiling bounds drift if config is
     * reloaded mid-game without restart.
     */
    @WrapOperation(method = "tickPot",
            at = @At(value = "INVOKE",
                     target = "Lnet/darkhax/botanypots/common/impl/Helpers;getRequiredGrowthTicks(Lnet/darkhax/botanypots/common/api/context/BotanyPotContext;Lnet/minecraft/world/level/Level;Lnet/darkhax/botanypots/common/api/data/recipes/crop/Crop;Lnet/darkhax/botanypots/common/api/data/recipes/soil/Soil;)I"))
    private static int arcadia$cachedRequiredGrowthTicks(
            BotanyPotContext ctx, Level level, Crop crop, Soil soil, Operation<Integer> original,
            Level levelArg, BlockPos posArg, BlockState stateArg, BotanyPotBlockEntity pot) {
        if (!ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.get()) {
            return original.call(ctx, level, crop, soil);
        }
        final ArcadiaPotState state = (ArcadiaPotState) pot;
        if (state.arcadia$getRequiredTicksRemaining() > 0) {
            state.arcadia$decrementRequiredTicksRemaining();
            return state.arcadia$getCachedRequiredTicks();
        }
        final int result = original.call(ctx, level, crop, soil);
        state.arcadia$setCachedRequiredTicks(result);
        final int period = ArcadiaConfig.BOTANY.safetyRevalidatePeriod.get();
        state.arcadia$setRequiredTicksRemaining(period > 0 ? period : Integer.MAX_VALUE);
        return result;
    }

    @WrapMethod(method = "getOrInvalidateSoil")
    private Soil arcadia$cachedSoil(Operation<Soil> original) {
        if (!ArcadiaConfig.BOTANY.s1MatchesCache.get()) {
            return original.call();
        }
        final BotanyPotBlockEntity self = (BotanyPotBlockEntity) (Object) this;
        final Level lvl = self.getLevel();
        if (lvl == null) {
            return original.call();
        }

        if (arcadia$soilTtl > 0
                && arcadia$lastSoilHolder != null
                && self.getBlockState() == arcadia$lastSoilState
                && !((AbstractBotanyPotBlockEntity) self).getSoilItem().isEmpty()) {
            final RecipeHolder<Soil> current = this.soil.apply(lvl);
            if (current == arcadia$lastSoilHolder) {
                arcadia$soilTtl--;
                return current.value();
            }
        }

        final Soil result = original.call();
        if (result != null) {
            arcadia$lastSoilHolder = this.soil.apply(lvl);
            arcadia$lastSoilState = self.getBlockState();
            final int period = ArcadiaConfig.BOTANY.safetyRevalidatePeriod.get();
            arcadia$soilTtl = period > 0 ? period : Integer.MAX_VALUE;
        } else {
            arcadia$lastSoilHolder = null;
            arcadia$lastSoilState = null;
            arcadia$soilTtl = 0;
        }
        return result;
    }

    @WrapMethod(method = "getOrInvalidateCrop")
    private Crop arcadia$cachedCrop(Operation<Crop> original) {
        if (!ArcadiaConfig.BOTANY.s1MatchesCache.get()) {
            return original.call();
        }
        final BotanyPotBlockEntity self = (BotanyPotBlockEntity) (Object) this;
        final Level lvl = self.getLevel();
        if (lvl == null) {
            return original.call();
        }

        if (arcadia$cropTtl > 0
                && arcadia$lastCropHolder != null
                && self.getBlockState() == arcadia$lastCropState
                && !((AbstractBotanyPotBlockEntity) self).getSeedItem().isEmpty()) {
            final RecipeHolder<Crop> current = this.crop.apply(lvl);
            if (current == arcadia$lastCropHolder) {
                arcadia$cropTtl--;
                return current.value();
            }
        }

        final Crop result = original.call();
        if (result != null) {
            arcadia$lastCropHolder = this.crop.apply(lvl);
            arcadia$lastCropState = self.getBlockState();
            final int period = ArcadiaConfig.BOTANY.safetyRevalidatePeriod.get();
            arcadia$cropTtl = period > 0 ? period : Integer.MAX_VALUE;
        } else {
            arcadia$lastCropHolder = null;
            arcadia$lastCropState = null;
            arcadia$cropTtl = 0;
        }
        return result;
    }
}
