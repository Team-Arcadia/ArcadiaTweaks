package com.teamarcadia.arcadiatweaks.common.module;

public interface ArcadiaModule {

    String id();

    boolean enabledByConfig();

    default void onRegister() {}

    default void onCommonSetup() {}

    default void onServerStarting() {}
}
