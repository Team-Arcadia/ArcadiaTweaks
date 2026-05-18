package com.teamarcadia.arcadiatweaks.common.modules.mekanism;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaModule;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaPatchVariant;

public final class MekanismModule implements ArcadiaModule {

    @Override
    public String id() {
        return "mekanism";
    }

    @Override
    public boolean enabledByConfig() {
        return ArcadiaPatchVariant.hasMekanism() && ArcadiaConfig.MODULE_MEKANISM_ENABLED.get();
    }

    @Override
    public void onServerStarting() {
        ArcadiaTweaks.LOGGER.info(
                "[mekanism] Active strategies: M1={}(max={}) M2={}(max={}) M3={}(max={}) M4-client={}",
                ArcadiaConfig.MEKANISM.transmitterPullBackoff.get(),
                ArcadiaConfig.MEKANISM.transmitterPullBackoffMaxTicks.get(),
                ArcadiaConfig.MEKANISM.networkEmitBackoff.get(),
                ArcadiaConfig.MEKANISM.networkEmitBackoffMaxTicks.get(),
                ArcadiaConfig.MEKANISM.logisticalTransporterIdleBackoff.get(),
                ArcadiaConfig.MEKANISM.logisticalTransporterIdleBackoffMaxTicks.get(),
                ArcadiaConfig.MEKANISM.clientLogisticalTransporterSafeRendering.get()
        );
    }
}
