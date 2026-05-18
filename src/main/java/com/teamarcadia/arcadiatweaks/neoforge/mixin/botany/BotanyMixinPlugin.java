package com.teamarcadia.arcadiatweaks.neoforge.mixin.botany;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.neoforge.botany.BotanyMixinCompatibility;
import com.teamarcadia.arcadiatweaks.neoforge.mixin.ArcadiaMixinTargetInspector;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class BotanyMixinPlugin implements IMixinConfigPlugin {

    private static final Set<String> LOGGED_SKIPS = new HashSet<>();
    private static final String TICK_POT_DESCRIPTOR =
            "(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/darkhax/botanypots/common/impl/block/entity/BotanyPotBlockEntity;)V";
    private static final String TICK_ACCUMULATOR = "net/darkhax/bookshelf/common/api/util/TickAccumulator";
    private static final String TICK_ACCUMULATOR_LEVEL_DESCRIPTOR = "(Lnet/minecraft/world/level/Level;)V";

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
            case "AbstractBotanyPotBlockEntityMixin" -> ArcadiaMixinTargetInspector.hasMethod(targetClassName, "markUpdated", "()V");
            case "BotanyPotBlockEntityMixin" -> isBotanyPotBlockEntityCompatible(targetClassName);
            default -> true;
        };
        if (!compatible && LOGGED_SKIPS.add(simpleMixinName)) {
            ArcadiaTweaks.LOGGER.warn("[botany] Skipping {}: target structure changed in {}.", simpleMixinName, targetClassName);
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

    private static boolean isBotanyPotBlockEntityCompatible(String targetClassName) {
        final boolean baseCompatible = ArcadiaMixinTargetInspector.hasField(targetClassName, "soil",
                "Lnet/darkhax/bookshelf/common/api/function/ReloadableCache;")
                && ArcadiaMixinTargetInspector.hasField(targetClassName, "crop",
                "Lnet/darkhax/bookshelf/common/api/function/ReloadableCache;")
                && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "reset", "()V")
                && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "onToolChanged", "(Lnet/minecraft/world/item/ItemStack;)V")
                && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "getOrInvalidateSoil",
                "()Lnet/darkhax/botanypots/common/api/data/recipes/soil/Soil;")
                && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "getOrInvalidateCrop",
                "()Lnet/darkhax/botanypots/common/api/data/recipes/crop/Crop;")
                && ArcadiaMixinTargetInspector.hasMethod(targetClassName, "tickPot", TICK_POT_DESCRIPTOR);
        if (!baseCompatible) {
            BotanyMixinCompatibility.setTickPotCoalescingCompatible(false);
            return false;
        }

        final boolean coalescingCompatible = ArcadiaMixinTargetInspector.hasMethodCall(
                targetClassName,
                "tickPot",
                TICK_POT_DESCRIPTOR,
                TICK_ACCUMULATOR,
                "tickDown",
                TICK_ACCUMULATOR_LEVEL_DESCRIPTOR
        ) && ArcadiaMixinTargetInspector.hasMethodCall(
                targetClassName,
                "tickPot",
                TICK_POT_DESCRIPTOR,
                TICK_ACCUMULATOR,
                "tickUp",
                TICK_ACCUMULATOR_LEVEL_DESCRIPTOR
        );
        BotanyMixinCompatibility.setTickPotCoalescingCompatible(coalescingCompatible);
        if (!coalescingCompatible && LOGGED_SKIPS.add("BotanyPotBlockEntityMixin.S2")) {
            ArcadiaTweaks.LOGGER.warn(
                    "[botany] Disabling BotanyPotBlockEntityMixin S2 tick coalescing: tickPot no longer calls TickAccumulator.tickDown/tickUp with the expected descriptors."
            );
        }
        return true;
    }
}
