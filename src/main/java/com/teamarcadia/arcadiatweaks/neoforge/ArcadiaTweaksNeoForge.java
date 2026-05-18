package com.teamarcadia.arcadiatweaks.neoforge;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import com.teamarcadia.arcadiatweaks.common.module.ArcadiaModule;
import com.teamarcadia.arcadiatweaks.common.module.ModuleRegistry;
import com.teamarcadia.arcadiatweaks.neoforge.admin.ArcadiaAdminCommand;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.RegisterGameTestsEvent;
import net.neoforged.neoforge.event.server.ServerStartingEvent;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

@Mod(ArcadiaTweaks.MOD_ID)
public final class ArcadiaTweaksNeoForge {

    public ArcadiaTweaksNeoForge(IEventBus modBus, ModContainer container) {
        container.registerConfig(ModConfig.Type.COMMON, ArcadiaConfig.SPEC);

        // Module bootstrap is deferred to FMLCommonSetupEvent: config values
        // cannot be read during @Mod construction (the TOML file is parsed
        // asynchronously after registerConfig returns).

        modBus.addListener(this::onCommonSetup);
        registerGameTestsIfPresent(modBus);
        NeoForge.EVENT_BUS.addListener(this::onServerStarting);
        NeoForge.EVENT_BUS.addListener(ArcadiaAdminCommand::register);

        ArcadiaTweaks.LOGGER.info("ArcadiaTweaks loaded.");
    }

    private static void registerGameTestsIfPresent(IEventBus modBus) {
        try {
            final Class<?> gameTests = Class.forName(
                    "com.teamarcadia.arcadiatweaks.neoforge.gametest.ArcadiaTweaksGameTests",
                    false,
                    ArcadiaTweaksNeoForge.class.getClassLoader()
            );
            final Method register = gameTests.getMethod("register", RegisterGameTestsEvent.class);
            modBus.addListener((RegisterGameTestsEvent event) -> invokeGameTestRegister(register, event));
        } catch (ClassNotFoundException ignored) {
            // GameTests are compiled for dev runs but excluded from production jars.
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Invalid ArcadiaTweaks GameTest registration method", e);
        }
    }

    private static void invokeGameTestRegister(Method register, RegisterGameTestsEvent event) {
        try {
            register.invoke(null, event);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException("Unable to access ArcadiaTweaks GameTest registration method", e);
        } catch (InvocationTargetException e) {
            final Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("ArcadiaTweaks GameTest registration failed", cause);
        }
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
}
