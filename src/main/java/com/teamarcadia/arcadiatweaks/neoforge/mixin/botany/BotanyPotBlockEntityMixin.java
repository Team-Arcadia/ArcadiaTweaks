package com.teamarcadia.arcadiatweaks.neoforge.mixin.botany;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import net.darkhax.bookshelf.common.api.function.ReloadableCache;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.block.entity.AbstractBotanyPotBlockEntity;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * S1 - Memoize the result of getOrInvalidateSoil / getOrInvalidateCrop while the
 * inputs that could change the matches() outcome are stable.
 *
 * Cache invariants: same RecipeHolder identity (caught when the recipe cache reloads),
 * same BlockState (catches potType / pot_predicate-based invalidation), non-empty stack.
 *
 * Slot mutations route through onSoilChanged/onSeedChanged which already call reset();
 * we hook reset() HEAD as the single source of truth for cache invalidation.
 *
 * A bounded TTL (safety_revalidate_period_ticks) gives a worst-case bound to any stale
 * state if an addon mutates internal state through an unforeseen path.
 */
@Mixin(BotanyPotBlockEntity.class)
public abstract class BotanyPotBlockEntityMixin {

    @Shadow @Final private ReloadableCache<RecipeHolder<Soil>> soil;
    @Shadow @Final private ReloadableCache<RecipeHolder<Crop>> crop;

    // getSoilItem/getSeedItem live on the abstract superclass; @Shadow can only
    // bind to methods declared on the target class itself, so we cast through
    // AbstractBotanyPotBlockEntity at the call site instead.

    @Unique private RecipeHolder<Soil> arcadia$lastSoilHolder;
    @Unique private RecipeHolder<Crop> arcadia$lastCropHolder;
    @Unique private BlockState         arcadia$lastSoilState;
    @Unique private BlockState         arcadia$lastCropState;
    @Unique private int                arcadia$soilTtl;
    @Unique private int                arcadia$cropTtl;

    @Inject(method = "reset", at = @At("HEAD"))
    private void arcadia$invalidateMatchCaches(CallbackInfo ci) {
        arcadia$lastSoilHolder = null;
        arcadia$lastCropHolder = null;
        arcadia$lastSoilState = null;
        arcadia$lastCropState = null;
        arcadia$soilTtl = 0;
        arcadia$cropTtl = 0;
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
