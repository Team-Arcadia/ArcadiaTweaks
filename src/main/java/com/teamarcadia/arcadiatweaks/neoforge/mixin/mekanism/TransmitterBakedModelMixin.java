package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.google.common.cache.LoadingCache;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import mekanism.client.model.data.TransmitterModelData;
import mekanism.client.render.obj.TransmitterBakedModel;
import mekanism.common.tile.transmitter.TileEntityTransmitter;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.client.model.data.ModelData;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;

@Mixin(TransmitterBakedModel.class)
public abstract class TransmitterBakedModelMixin {

    @Unique private boolean arcadia$buildingSafeRenderQuads;

    @Shadow @Final private LoadingCache<?, List<BakedQuad>> cache;

    @Shadow
    public abstract List<BakedQuad> getQuads(BlockState state, Direction side, RandomSource rand, ModelData extraData, RenderType renderType);

    @Inject(method = "getQuads(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;",
            at = @At("HEAD"),
            cancellable = true)
    private void arcadia$renderColoredTransporterGlassInCutout(
            BlockState state,
            Direction side,
            RandomSource rand,
            ModelData extraData,
            RenderType renderType,
            CallbackInfoReturnable<List<BakedQuad>> cir
    ) {
        arcadia$guardModelCache();
        if (arcadia$buildingSafeRenderQuads || !arcadia$enabled() || side != null) {
            return;
        }
        final TransmitterModelData data = extraData.get(TileEntityTransmitter.TRANSMITTER_PROPERTY);
        if (data == null || !data.getHasColor()) {
            return;
        }
        if (renderType == RenderType.cutout()) {
            final List<BakedQuad> baseQuads;
            final List<BakedQuad> colorQuads;
            arcadia$buildingSafeRenderQuads = true;
            try {
                baseQuads = getQuads(state, side, rand, extraData, renderType);
                colorQuads = getQuads(state, side, rand, extraData, RenderType.translucent());
            } finally {
                arcadia$buildingSafeRenderQuads = false;
            }
            if (!colorQuads.isEmpty()) {
                final List<BakedQuad> combined = new ArrayList<>(baseQuads.size() + colorQuads.size());
                combined.addAll(baseQuads);
                combined.addAll(colorQuads);
                cir.setReturnValue(combined);
            }
        } else if (renderType == RenderType.translucent()) {
            cir.setReturnValue(List.of());
        }
    }

    @Unique
    private static boolean arcadia$enabled() {
        return ArcadiaConfig.MODULE_MEKANISM_ENABLED.get()
                && ArcadiaConfig.MEKANISM.clientLogisticalTransporterSafeRendering.get();
    }

    @Unique
    private void arcadia$guardModelCache() {
        if (!arcadia$enabled()) {
            return;
        }
        final int maxEntries = ArcadiaConfig.MEKANISM.clientTransmitterModelCacheMaxEntries.get();
        if (cache.size() > maxEntries) {
            cache.invalidateAll();
            cache.cleanUp();
        }
    }
}
