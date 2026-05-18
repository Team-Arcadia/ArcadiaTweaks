package com.teamarcadia.arcadiatweaks.common.modules.refinedstorage;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaModule;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaPatchVariant;

public final class RefinedStorageModule implements ArcadiaModule {

    @Override
    public String id() {
        return "refinedstorage";
    }

    @Override
    public boolean enabledByConfig() {
        return ArcadiaPatchVariant.hasRefinedStorage() && ArcadiaConfig.MODULE_REFINED_STORAGE_ENABLED.get();
    }

    @Override
    public void onServerStarting() {
        ArcadiaTweaks.LOGGER.info(
                "[refinedstorage] Active strategies: S1={}(N={})",
                ArcadiaConfig.REFINED_STORAGE.activenessCheckCoalescing.get(),
                ArcadiaConfig.REFINED_STORAGE.activenessCheckInterval.get()
        );
    }
}
