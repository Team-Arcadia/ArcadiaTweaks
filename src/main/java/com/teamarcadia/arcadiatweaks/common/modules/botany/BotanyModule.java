package com.teamarcadia.arcadiatweaks.common.modules.botany;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaModule;

public final class BotanyModule implements ArcadiaModule {

    @Override
    public String id() {
        return "botany";
    }

    @Override
    public boolean enabledByConfig() {
        return ArcadiaConfig.MODULE_BOTANY_ENABLED.get();
    }

    @Override
    public void onServerStarting() {
        ArcadiaTweaks.LOGGER.info(
                "[botany] Active strategies: S1={} S2={}(N={}) S3={} A1={} A2={}",
                ArcadiaConfig.BOTANY.s1MatchesCache.get(),
                ArcadiaConfig.BOTANY.s2TickCoalescing.get(),
                ArcadiaConfig.BOTANY.s2CoalesceN.get(),
                ArcadiaConfig.BOTANY.s3HopperBackoff.get(),
                ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.get(),
                ArcadiaConfig.BOTANY.a2LightFlagDowngrade.get()
        );
    }
}
