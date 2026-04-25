package com.teamarcadia.arcadiatweaks.neoforge.gametest;

import com.teamarcadia.arcadiatweaks.ArcadiaTweaks;
import com.teamarcadia.arcadiatweaks.common.config.ArcadiaConfig;
import net.darkhax.botanypots.common.api.data.recipes.crop.Crop;
import net.darkhax.botanypots.common.api.data.recipes.soil.Soil;
import net.darkhax.botanypots.common.impl.block.entity.BotanyPotBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

/**
 * GameTests for the BotanyPots optimization module.
 *
 * Validates that the S1 matches() memoization Mixin does not break observable
 * BotanyPots behavior. Tests run on the empty_platform structure (3x3x3 stone
 * floor, air above) - the pot is placed at relative pos (1, 1, 1).
 *
 * Run with: ./gradlew runGameTestServer
 *
 * The runGameTestServer task spins up a headless server, runs every registered
 * test, and exits with a non-zero code if any test fails - usable from CI.
 */
@GameTestHolder(ArcadiaTweaks.MOD_ID)
@PrefixGameTestTemplate(false)
public final class BotanyPotsGameTests {

    // BotanyPots registers per-color block variants ("<color>_botany_pot");
    // terracotta is the canonical "plain" variant (used as the creative tab icon).
    private static final ResourceLocation BOTANY_POT_ID =
            ResourceLocation.fromNamespaceAndPath("botanypots", "terracotta_botany_pot");

    private static final BlockPos POT_POS = new BlockPos(1, 1, 1);

    // Slot indices on AbstractBotanyPotBlockEntity (kept in sync with BotanyPots).
    private static final int SOIL_SLOT = 0;
    private static final int SEED_SLOT = 1;

    private BotanyPotsGameTests() {}

    /**
     * Smoke test - validates that the GameTest pipeline itself is wired up
     * (mod-bus listener registered, structure resolved, server boot OK).
     */
    @GameTest(template = "empty_platform")
    public static void smoke(GameTestHelper helper) {
        helper.succeed();
    }

    /**
     * Places a pot, sets a soil item, calls getOrInvalidateSoil twice in a row
     * (first miss, second cache hit) and asserts the result is identical and
     * non-null. Validates that the S1 @WrapMethod does not corrupt the lookup.
     */
    @GameTest(template = "empty_platform", timeoutTicks = 40)
    public static void s1ConsistentSoilLookup(GameTestHelper helper) {
        final Block pot = BuiltInRegistries.BLOCK.get(BOTANY_POT_ID);
        helper.setBlock(POT_POS, pot.defaultBlockState());

        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity bpe)) {
            helper.fail("Block at " + POT_POS + " is not a BotanyPotBlockEntity.");
            return;
        }

        bpe.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));

        final Soil first = bpe.getOrInvalidateSoil();
        if (first == null) {
            helper.fail("First soil lookup returned null - is minecraft:dirt a registered BotanyPots soil?");
            return;
        }
        final Soil second = bpe.getOrInvalidateSoil();
        if (first != second) {
            helper.fail("S1 cache returned a different Soil instance on second lookup (cache inconsistency).");
            return;
        }
        helper.succeed();
    }

    /**
     * Places a pot, soil and seed, lets the world tick, then asserts that
     * growthTime advanced. Sanity test that S1 has not broken the growth path.
     */
    @GameTest(template = "empty_platform", timeoutTicks = 200)
    public static void s1CropGrowthAdvances(GameTestHelper helper) {
        final Block pot = BuiltInRegistries.BLOCK.get(BOTANY_POT_ID);
        helper.setBlock(POT_POS, pot.defaultBlockState());

        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity bpe)) {
            helper.fail("Block at " + POT_POS + " is not a BotanyPotBlockEntity.");
            return;
        }

        bpe.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        bpe.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));

        helper.runAfterDelay(150, () -> {
            if (bpe.isRemoved()) {
                helper.fail("Pot was removed before the assertion.");
                return;
            }
            if (bpe.getOrInvalidateSoil() == null) {
                helper.fail("Soil lookup returned null after 150 ticks - pot lost its soil.");
                return;
            }
            if (bpe.getOrInvalidateCrop() == null) {
                helper.fail("Crop lookup returned null after 150 ticks - pot lost its crop.");
                return;
            }
            final float growth = bpe.growthTime.getTicks();
            if (growth <= 0f) {
                helper.fail("Pot did not advance growth after 150 ticks (growthTime=" + growth + ").");
                return;
            }
            helper.succeed();
        });
    }

    /**
     * Places a pot with crop A, ticks, then changes the seed to a different
     * crop. Validates that S1 properly invalidates the cached match (otherwise
     * the second lookup would still report crop A's recipe).
     */
    @GameTest(template = "empty_platform", timeoutTicks = 100)
    public static void s1CacheInvalidatesOnSeedSwap(GameTestHelper helper) {
        final Block pot = BuiltInRegistries.BLOCK.get(BOTANY_POT_ID);
        helper.setBlock(POT_POS, pot.defaultBlockState());

        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity bpe)) {
            helper.fail("Block at " + POT_POS + " is not a BotanyPotBlockEntity.");
            return;
        }

        bpe.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        bpe.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));
        final Crop wheat = bpe.getOrInvalidateCrop();
        if (wheat == null) {
            helper.fail("Wheat seeds did not resolve to a crop recipe.");
            return;
        }

        bpe.setItem(SEED_SLOT, new ItemStack(Items.CARROT));
        final Crop carrot = bpe.getOrInvalidateCrop();
        if (carrot == wheat) {
            helper.fail("S1 cache did not invalidate after seed swap (still returned the wheat crop).");
            return;
        }
        helper.succeed();
    }

    /**
     * Microbenchmark - exercises getOrInvalidateSoil/Crop in a tight loop with
     * S1 disabled then enabled, asserts the cached path is at least 30% faster
     * than the baseline. Targets the exact code S1 optimizes (the matches()
     * call) without the surrounding tickPot machinery, so the measured ratio
     * is the upper bound of the improvement on the production hot path.
     *
     * Fails the test below threshold so a regression in S1 efficiency surfaces
     * in CI rather than only on a real bench.
     */
    @GameTest(template = "empty_platform", timeoutTicks = 200)
    public static void s1MicrobenchProvesGain(GameTestHelper helper) {
        final Block pot = BuiltInRegistries.BLOCK.get(BOTANY_POT_ID);
        helper.setBlock(POT_POS, pot.defaultBlockState());

        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity bpe)) {
            helper.fail("Block at " + POT_POS + " is not a BotanyPotBlockEntity.");
            return;
        }

        bpe.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        bpe.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));

        if (bpe.getOrInvalidateSoil() == null || bpe.getOrInvalidateCrop() == null) {
            helper.fail("Default soil/crop recipes did not resolve - benchmark needs them.");
            return;
        }

        final int warmupIters = 10_000;
        final int measureIters = 200_000;

        // Warm both code paths so JIT compiles them before measurement.
        warm(bpe, false, warmupIters);
        warm(bpe, true, warmupIters);
        warm(bpe, false, warmupIters);
        warm(bpe, true, warmupIters);

        ArcadiaConfig.BOTANY.s1MatchesCache.set(false);
        long offSink = 0L;
        final long offStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            offSink ^= System.identityHashCode(bpe.getOrInvalidateSoil());
            offSink ^= System.identityHashCode(bpe.getOrInvalidateCrop());
        }
        final long offNanos = System.nanoTime() - offStart;

        ArcadiaConfig.BOTANY.s1MatchesCache.set(true);
        long onSink = 0L;
        final long onStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            onSink ^= System.identityHashCode(bpe.getOrInvalidateSoil());
            onSink ^= System.identityHashCode(bpe.getOrInvalidateCrop());
        }
        final long onNanos = System.nanoTime() - onStart;

        // Restore default - dev runs and CI rely on S1 staying on after this test.
        ArcadiaConfig.BOTANY.s1MatchesCache.set(true);

        final double offMs   = offNanos / 1_000_000.0;
        final double onMs    = onNanos  / 1_000_000.0;
        final double speedup = (double) offNanos / onNanos;
        final double gainPct = (1.0 - (double) onNanos / offNanos) * 100.0;
        final double offNs   = offNanos / (double) measureIters;
        final double onNs    = onNanos  / (double) measureIters;

        ArcadiaTweaks.LOGGER.info(
                "[S1 microbench] iters={} | off={}ms ({} ns/op) | on={}ms ({} ns/op) | speedup={}x | gain={}% (sinks {} {})",
                measureIters,
                String.format(java.util.Locale.ROOT, "%.2f", offMs),
                String.format(java.util.Locale.ROOT, "%.1f", offNs),
                String.format(java.util.Locale.ROOT, "%.2f", onMs),
                String.format(java.util.Locale.ROOT, "%.1f", onNs),
                String.format(java.util.Locale.ROOT, "%.2f", speedup),
                String.format(java.util.Locale.ROOT, "%.1f", gainPct),
                offSink, onSink);

        final double minGainPct = 30.0;
        if (gainPct < minGainPct) {
            helper.fail("S1 microbench: only " + String.format(java.util.Locale.ROOT, "%.1f", gainPct)
                    + "% gain (threshold " + minGainPct + "%). off="
                    + String.format(java.util.Locale.ROOT, "%.2f", offMs) + "ms, on="
                    + String.format(java.util.Locale.ROOT, "%.2f", onMs) + "ms over " + measureIters + " iters.");
            return;
        }
        helper.succeed();
    }

    private static void warm(BotanyPotBlockEntity bpe, boolean s1On, int n) {
        ArcadiaConfig.BOTANY.s1MatchesCache.set(s1On);
        long sink = 0L;
        for (int i = 0; i < n; i++) {
            sink ^= System.identityHashCode(bpe.getOrInvalidateSoil());
            sink ^= System.identityHashCode(bpe.getOrInvalidateCrop());
        }
        if (sink == 0xDEADBEEFL) ArcadiaTweaks.LOGGER.trace("warm sink {}", sink);
    }
}
