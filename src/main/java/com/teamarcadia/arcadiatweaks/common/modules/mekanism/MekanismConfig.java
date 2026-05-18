package com.teamarcadia.arcadiatweaks.common.modules.mekanism;

import net.neoforged.neoforge.common.ModConfigSpec;

public final class MekanismConfig {

    public final ModConfigSpec.BooleanValue transmitterPullBackoff;
    public final ModConfigSpec.IntValue transmitterPullBackoffMaxTicks;
    public final ModConfigSpec.BooleanValue networkEmitBackoff;
    public final ModConfigSpec.IntValue networkEmitBackoffMaxTicks;
    public final ModConfigSpec.BooleanValue logisticalTransporterIdleBackoff;
    public final ModConfigSpec.IntValue logisticalTransporterIdleBackoffMaxTicks;
    public final ModConfigSpec.BooleanValue clientLogisticalTransporterSafeRendering;
    public final ModConfigSpec.IntValue clientTransmitterModelCacheMaxEntries;

    public MekanismConfig(ModConfigSpec.Builder b) {
        b.comment(
                "Mekanism optimizations. Conservative cable/network throttles only;",
                "machine recipe ticks and heat simulation are intentionally left untouched."
        ).push("mekanism");

        transmitterPullBackoff = b
                .comment(
                        "M1 - Back off transmitter pull scans when a cable/tube/pipe repeatedly pulls nothing.",
                        "Applies to Universal Cable, Mechanical Pipe and Pressurized Tube pullFromAcceptors().",
                        "Successful pulls reset the backoff immediately. Worst-case delay after a source becomes available is the max value."
                )
                .define("transmitter_pull_backoff_enabled", true);

        transmitterPullBackoffMaxTicks = b
                .comment("M1 - Maximum transmitter pull backoff in ticks.")
                .defineInRange("transmitter_pull_backoff_max_ticks", 20, 1, 200);

        networkEmitBackoff = b
                .comment(
                        "M2 - Back off network emit scans when energy/fluid/chemical networks repeatedly emit nothing.",
                        "Heat networks are not affected."
                )
                .define("network_emit_backoff_enabled", true);

        networkEmitBackoffMaxTicks = b
                .comment("M2 - Maximum network emit backoff in ticks.")
                .defineInRange("network_emit_backoff_max_ticks", 20, 1, 200);

        logisticalTransporterIdleBackoff = b
                .comment(
                        "M3 - Back off repeated idle path recalculations for Logistical Transporters.",
                        "Targets the TransporterStack.calculateIdle/getIdlePath hot path seen in Spark.",
                        "Only applies after Mekanism already produced a no-target idle path; destination and home paths are not throttled."
                )
                .define("logistical_transporter_idle_backoff_enabled", true);

        logisticalTransporterIdleBackoffMaxTicks = b
                .comment("M3 - Maximum idle path recalculation backoff in ticks.")
                .defineInRange("logistical_transporter_idle_backoff_max_ticks", 20, 1, 200);

        clientLogisticalTransporterSafeRendering = b
                .comment(
                        "M4 - Client-side safe rendering for colored Logistical Transporters.",
                        "When enabled on a client, ArcadiaTweaks renders Mekanism's colored Logistical Transporter layer in the cutout pass instead of the translucent pass.",
                        "Server-side transporter colors are kept for item routing, and the color remains visible client-side.",
                        "This avoids the client render path suspected of causing progressive FPS/memory degradation around many colored transporters."
                )
                .define("client_logistical_transporter_safe_rendering_enabled", true);

        clientTransmitterModelCacheMaxEntries = b
                .comment(
                        "M4 - Client-side guardrail for Mekanism transmitter model cache entries.",
                        "If Mekanism's transmitter baked-model cache grows above this value, ArcadiaTweaks clears it and lets it rebuild.",
                        "This is a defensive leak guard; normal colored transporter scenes should stay far below the default."
                )
                .defineInRange("client_transmitter_model_cache_max_entries", 4096, 256, 65536);

        b.pop();
    }
}
