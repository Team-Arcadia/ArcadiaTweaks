package com.teamarcadia.arcadiatweaks.common.modules.refinedstorage;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class RefinedStorageConfig {

    public final ModConfigSpec.BooleanValue activenessCheckCoalescing;
    public final ModConfigSpec.IntValue activenessCheckInterval;

    public RefinedStorageConfig(ModConfigSpec.Builder b) {
        b.comment(
                "Refined Storage optimizations. Each entry is a kill-switch - set false to disable",
                "the corresponding Mixin path while keeping the rest active."
        ).push("refinedstorage");

        activenessCheckCoalescing = b
                .comment(
                        "S1 - Coalesce network-node activeness checks.",
                        "RS2 recalculates level-loaded, redstone and network-energy state every block-entity tick.",
                        "This keeps node work cadence unchanged and only runs that activeness check every N ticks.",
                        "Worst-case visible delay after redstone/energy changes is N-1 ticks."
                )
                .define("activeness_check_coalescing_enabled", true);

        activenessCheckInterval = b
                .comment("S1 - Interval in ticks for activeness recalculation. 1 disables coalescing.")
                .defineInRange("activeness_check_interval_ticks", 20, 1, 200);

        b.pop();
    }
}
