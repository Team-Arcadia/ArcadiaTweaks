package com.teamarcadia.arcadiatweaks.neoforge.botany;

public final class BotanyMixinCompatibility {

    private static volatile boolean tickPotCoalescingCompatible = true;

    private BotanyMixinCompatibility() {}

    public static boolean isTickPotCoalescingCompatible() {
        return tickPotCoalescingCompatible;
    }

    public static void setTickPotCoalescingCompatible(boolean compatible) {
        tickPotCoalescingCompatible = compatible;
    }
}
