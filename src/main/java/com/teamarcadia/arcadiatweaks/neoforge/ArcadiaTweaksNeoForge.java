package com.teamarcadia.arcadiatweaks.neoforge;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaModule;
import com.teamarcadia.arcadiatweaks.common.module.ModuleRegistry;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.ModList;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

@Mod(ArcadiaTweaks.MOD_ID)
public final class ArcadiaTweaksNeoForge {

    public ArcadiaTweaksNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ArcadiaConfig.SPEC);

        // Module bootstrap is deferred to FMLCommonSetupEvent: config values
        // cannot be read during @Mod construction (the TOML file is parsed
        // asynchronously after registerConfig returns).

        modBus.addListener(this::onCommonSetup);
        modBus.addListener(this::onRegisterGameTests);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);

        ArcadiaTweaks.LOGGER.info("ArcadiaTweaks loaded.");
    }

    private void onCommonSetup(FMLCommonSetupEvent event) {
        ModuleRegistry.bootstrap();
        for (ArcadiaModule m : ModuleRegistry.active()) {
            m.onCommonSetup();
        }
        ArcadiaTweaks.LOGGER.info("Common setup done - {} module(s) active.",
                ModuleRegistry.active().size());
    }

    private void onServerStarting(ServerStartingEvent event) {
        for (ArcadiaModule m : ModuleRegistry.active()) {
            m.onServerStarting();
        }
    }

    /**
     * Legacy annotation-based gametest registration. NeoForge 1.21 has moved
     * to a JSON/datapack-driven system (data/&lt;modid&gt;/test_instance/*.json
     * + DeferredRegister&lt;TEST_FUNCTION&gt;); this listener still works as a
     * fallback for @GameTestHolder classes but the test runner cannot resolve
     * structures without the new pipeline. See README "Run automated tests".
     */
    private void onRegisterGameTests(RegisterGameTestsEvent event) {
        if (!ModList.get().isLoaded("botanypots")) {
            ArcadiaTweaks.LOGGER.info("BotanyPots not loaded - skipping botany gametest registration.");
            return;
        }
        try {
            Class<?> testHolder = Class.forName(
                    "com.teamarcadia.arcadiatweaks.neoforge.gametest.BotanyPotsGameTests");
            event.register(testHolder);
            ArcadiaTweaks.LOGGER.info("Registered legacy gametest holder: {}", testHolder.getSimpleName());
        } catch (ClassNotFoundException e) {
            ArcadiaTweaks.LOGGER.warn("Could not load BotanyPots gametests: {}", e.getMessage());
        }
    }
}
