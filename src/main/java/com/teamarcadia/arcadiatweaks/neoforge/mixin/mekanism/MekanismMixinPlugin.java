package com.teamarcadia.arcadiatweaks.neoforge.mixin.mekanism;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.neoforge.mixin.ArcadiaMixinTargetInspector;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MekanismMixinPlugin implements IMixinConfigPlugin {

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
            case "DynamicNetworkMixin" -> ArcadiaMixinTargetInspector.hasMethod(targetClassName, "acceptorChanged",
                    "(Lmekanism/common/content/network/transmitter/Transmitter;Lnet/minecraft/core/Direction;)V");
            case "EnergyNetworkMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "energyContainer",
                    "Lmekanism/common/capabilities/energy/VariableCapacityEnergyContainer;")
                    && ArcadiaMixinTargetInspector.hasField(targetClassName, "prevTransferAmount", "J")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onUpdate", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onContentsChanged", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "tickEmit", "(J)J");
            case "FluidNetworkMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "fluidTank",
                    "Lmekanism/common/capabilities/fluid/VariableCapacityFluidTank;")
                    && ArcadiaMixinTargetInspector.hasField(targetClassName, "prevTransferAmount", "I")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onUpdate", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onContentsChanged", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "tickEmit", "(Lnet/neoforged/neoforge/fluids/FluidStack;)I");
            case "ChemicalNetworkMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "chemicalTank",
                    "Lmekanism/api/chemical/IChemicalTank;")
                    && ArcadiaMixinTargetInspector.hasField(targetClassName, "prevTransferAmount", "J")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onUpdate", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onContentsChanged", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "tickEmit", "(Lmekanism/api/chemical/ChemicalStack;)J");
            case "LogisticalTransporterBaseMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "needsSync",
                    "Lit/unimi/dsi/fastutil/ints/Int2ObjectMap;")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onUpdateServer", "()V")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "recalculate",
                    "(ILmekanism/common/content/transporter/TransporterStack;J)Z")
                    && ArcadiaMixinTargetInspector.hasMethodCall(targetClassName, "onUpdateServer", "()V",
                    targetClassName, "recalculate", "(ILmekanism/common/content/transporter/TransporterStack;J)Z")
                    && ArcadiaMixinTargetInspector.hasMethod("mekanism.common.content.transporter.TransporterStack", "hasPath", "()Z")
                    && ArcadiaMixinTargetInspector.hasMethod("mekanism.common.content.transporter.TransporterStack", "getPathType",
                    "()Lmekanism/common/content/transporter/TransporterStack$Path;")
                    && ArcadiaMixinTargetInspector.hasMethod("mekanism.common.content.transporter.TransporterStack$Path", "noTarget", "()Z");
            case "TransporterStackMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "initiatedPath", "Z")
                    && ArcadiaMixinTargetInspector.hasField(targetClassName, "itemStack", "Lnet/minecraft/world/item/ItemStack;")
                    && ArcadiaMixinTargetInspector.hasField(targetClassName, "originalLocation", "J")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "hasPath", "()Z")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "getPathType",
                    "()Lmekanism/common/content/transporter/TransporterStack$Path;")
                    && ArcadiaMixinTargetInspector.hasMethod("mekanism.common.content.transporter.TransporterStack$Path", "noTarget", "()Z");
            case "UniversalCableMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "buffer",
                    "Lmekanism/common/capabilities/energy/BasicEnergyContainer;")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "pullFromAcceptors", "()V")
                    && ArcadiaMixinTargetInspector.hasMethodInHierarchy(targetClassName, "hasTransmitterNetwork", "()Z")
                    && ArcadiaMixinTargetInspector.hasMethodInHierarchy(targetClassName, "getTransmitterNetwork",
                    "()Lmekanism/common/lib/transmitter/DynamicNetwork;");
            case "MechanicalPipeMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "buffer",
                    "Lmekanism/common/capabilities/fluid/BasicFluidTank;")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "pullFromAcceptors", "()V")
                    && ArcadiaMixinTargetInspector.hasMethodInHierarchy(targetClassName, "hasTransmitterNetwork", "()Z")
                    && ArcadiaMixinTargetInspector.hasMethodInHierarchy(targetClassName, "getTransmitterNetwork",
                    "()Lmekanism/common/lib/transmitter/DynamicNetwork;");
            case "PressurizedTubeMixin" -> ArcadiaMixinTargetInspector.hasField(targetClassName, "chemicalTank",
                    "Lmekanism/api/chemical/IChemicalTank;")
                    && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "pullFromAcceptors", "()V")
                    && ArcadiaMixinTargetInspector.hasMethodInHierarchy(targetClassName, "hasTransmitterNetwork", "()Z")
                    && ArcadiaMixinTargetInspector.hasMethodInHierarchy(targetClassName, "getTransmitterNetwork",
                    "()Lmekanism/common/lib/transmitter/DynamicNetwork;");
            case "TransmitterBakedModelMixin" -> ArcadiaMixinTargetInspector.hasMethod(targetClassName, "getQuads",
                    "(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;Lnet/minecraft/util/RandomSource;Lnet/neoforged/neoforge/client/model/data/ModelData;Lnet/minecraft/client/renderer/RenderType;)Ljava/util/List;")
                    && ArcadiaMixinTargetInspector.hasClass("mekanism.client.model.data.TransmitterModelData")
                    && ArcadiaMixinTargetInspector.hasField("mekanism.common.tile.transmitter.TileEntityTransmitter",
                    "TRANSMITTER_PROPERTY", "Lnet/neoforged/neoforge/client/model/data/ModelProperty;");
            default -> true;
        };

        if (!compatible && LOGGED_SKIPS.add(simpleMixinName)) {
            ArcadiaTweaks.LOGGER.warn("[mekanism] Skipping {}: target structure changed in {}.", simpleMixinName, targetClassName);
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
}
