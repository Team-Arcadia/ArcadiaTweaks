package com.teamarcadia.arcadiatweaks.common.module;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.modules.botany.BotanyModule;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ModuleRegistry {

    private static final List<ArcadiaModule> ALL = new ArrayList<>();
    private static final List<ArcadiaModule> ACTIVE = new ArrayList<>();
    private static boolean bootstrapped = false;

    private ModuleRegistry() {}

    public static synchronized void bootstrap() {
        if (bootstrapped) return;
        bootstrapped = true;

        ALL.add(new BotanyModule());

        for (ArcadiaModule m : ALL) {
            if (m.enabledByConfig()) {
                ACTIVE.add(m);
                m.onRegister();
                ArcadiaTweaks.LOGGER.info("Module enabled: {}", m.id());
            } else {
                ArcadiaTweaks.LOGGER.info("Module disabled by config: {}", m.id());
            }
        }
    }

    public static List<ArcadiaModule> active() {
        return Collections.unmodifiableList(ACTIVE);
    }

    public static List<ArcadiaModule> all() {
        return Collections.unmodifiableList(ALL);
    }
}
