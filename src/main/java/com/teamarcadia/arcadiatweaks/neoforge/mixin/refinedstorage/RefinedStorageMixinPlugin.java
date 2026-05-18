package com.teamarcadia.arcadiatweaks.neoforge.mixin.refinedstorage;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.neoforge.mixin.ArcadiaMixinTargetInspector;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class RefinedStorageMixinPlugin implements IMixinConfigPlugin {

    private static final String BASE_NODE = "com.refinedmods.refinedstorage.common.support.network.AbstractBaseNetworkNodeContainerBlockEntity";
    private static final String TRANSMITTER = "com.refinedmods.refinedstorage.common.networking.NetworkTransmitterBlockEntity";
    private static final Set<String> LOGGED_SKIPS = new HashSet<>();

    @Override
    public void onLoad(String mixinPackage) {
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public boolean shouldApplyMixin(String targetClassName, String mixinClassName) {
        final String simpleMixinName = mixinClassName.substring(mixinClassName.lastIndexOf('.') + 1);
        final boolean compatible = switch (simpleMixinName) {
            case "AbstractBaseNetworkNodeContainerBlockEntityMixin" -> ArcadiaMixinTargetInspector.hasMethod(targetClassName, "updateActiveness",
                    "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/properties/BooleanProperty;)V");
            case "NetworkNodeBlockEntityTickerMixin" -> baseNodeCompatible()
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "tick",
                    "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/refinedmods/refinedstorage/common/support/network/AbstractBaseNetworkNodeContainerBlockEntity;)V");
            case "NetworkTransmitterBlockEntityTickerMixin" -> baseNodeCompatible()
                    && ArcadiaMixinTargetInspector.hasMethod(TRANSMITTER, "updateStateInLevel",
                    "(Lnet/minecraft/world/level/block/state/BlockState;)V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "tick",
                    "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lcom/refinedmods/refinedstorage/common/networking/NetworkTransmitterBlockEntity;)V");
            default -> true;
        };
        if (!compatible && LOGGED_SKIPS.add(simpleMixinName)) {
            ArcadiaTweaks.LOGGER.warn("[refinedstorage] Skipping {}: target structure changed in {}.", simpleMixinName, targetClassName);
        }
        return compatible;
    }

    @Override
    public void acceptTargets(Set<String> myTargets, Set<String> otherTargets) {
    }

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    @Override
    public void postApply(String targetClassName, ClassNode targetClass, String mixinClassName, IMixinInfo mixinInfo) {
    }

    private static boolean baseNodeCompatible() {
        return ArcadiaMixinTargetInspector.hasMethod(BASE_NODE, "updateActiveness",
                "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/properties/BooleanProperty;)V");
    }
}
