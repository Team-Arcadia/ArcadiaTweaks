package com.teamarcadia.arcadiatweaks.common.config;

import com.teamarcadia.arcadiatweaks.common.modules.botany.BotanyConfig;
import net.neoforged.neoforge.common.ModConfigSpec;

public final class ArcadiaConfig {

    public static final ModConfigSpec.Builder BUILDER = new ModConfigSpec.Builder();

    public static final ModConfigSpec.BooleanValue MODULE_BOTANY_ENABLED;

    public static final BotanyConfig BOTANY;

    public static final ModConfigSpec SPEC;

    static {
        BUILDER.comment("ArcadiaTweaks - master module toggles").push("modules");
        MODULE_BOTANY_ENABLED = BUILDER
                .comment("Enable the BotanyPots optimization module.")
                .define("botany", true);
        BUILDER.pop();

        BOTANY = new BotanyConfig(BUILDER);

        SPEC = BUILDER.build();
    }

    private ArcadiaConfig() {}
}
