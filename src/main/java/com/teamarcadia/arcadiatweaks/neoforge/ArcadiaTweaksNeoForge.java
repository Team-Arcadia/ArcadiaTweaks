package com.teamarcadia.arcadiatweaks.neoforge;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaModule;
import com.teamarcadia.arcadiatweaks.common.module.ModuleRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(ArcadiaTweaks.MOD_ID)
public final class ArcadiaTweaksNeoForge {

    public ArcadiaTweaksNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ArcadiaConfig.SPEC);

        ModuleRegistry.bootstrap();

        modBus.addListener(this::onCommonSetup);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        ArcadiaTweaks.LOGGER.info("ArcadiaTweaks loaded - {} module(s) active.",
                ModuleRegistry.active().size());
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        for (ArcadiaModule m : ModuleRegistry.active()) {
            m.onCommonSetup();
        }
    }

    private void onServerStarting(ServerStartingEvent event) {
        for (ArcadiaModule m : ModuleRegistry.active()) {
            m.onServerStarting();
        }
    }
}
