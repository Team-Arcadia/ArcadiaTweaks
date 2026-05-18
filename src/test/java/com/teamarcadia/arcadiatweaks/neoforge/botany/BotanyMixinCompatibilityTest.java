package com.teamarcadia.arcadiatweaks.neoforge.botany;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

final class BotanyMixinCompatibilityTest {

    @AfterEach
    void resetCompatibilityFlag() {
        BotanyMixinCompatibility.setTickPotCoalescingCompatible(true);
    }

    @Test
    void coalescingCompatibilityCanBeDisabledAndRestored() {
        BotanyMixinCompatibility.setTickPotCoalescingCompatible(false);
        assertFalse(BotanyMixinCompatibility.isTickPotCoalescingCompatible());

        BotanyMixinCompatibility.setTickPotCoalescingCompatible(true);
        assertTrue(BotanyMixinCompatibility.isTickPotCoalescingCompatible());
    }
}
