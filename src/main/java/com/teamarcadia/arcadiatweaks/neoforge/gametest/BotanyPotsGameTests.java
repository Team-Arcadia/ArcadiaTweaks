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
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
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
    private static final ResourceLocation BOTANY_HOPPER_POT_ID =
            ResourceLocation.fromNamespaceAndPath("botanypots", "terracotta_hopper_botany_pot");

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

    /**
     * Functional check for S3: a hopper pot above empty space (the floor stone)
     * must not crash and must not lose items, with S3 on or off. The export
     * branch reads the block below; with S3 active and no progress recorded,
     * the backoff schedule kicks in. Either way the item stays in the pot.
     */
    @GameTest(template = "empty_platform", timeoutTicks = 100)
    public static void s3HopperBackoffNoCrash(GameTestHelper helper) {
        final Block hopperPot = BuiltInRegistries.BLOCK.get(BOTANY_HOPPER_POT_ID);
        helper.setBlock(POT_POS, hopperPot.defaultBlockState());

        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity bpe)) {
            helper.fail("Block at " + POT_POS + " is not a BotanyPotBlockEntity (hopper variant).");
            return;
        }

        final int firstStorageSlot = 3;
        bpe.setItem(firstStorageSlot, new ItemStack(Items.DIRT, 32));

        helper.runAfterDelay(50, () -> {
            if (bpe.isRemoved()) {
                helper.fail("Hopper pot got removed during the test.");
                return;
            }
            if (bpe.getItem(firstStorageSlot).isEmpty()) {
                helper.fail("Item disappeared - export should not move anything when there is no inventory below.");
                return;
            }
            helper.succeed();
        });
    }

    /**
     * Microbenchmark for S3. Builds the worst case for a hopper pot:
     *   - chest below the pot, all 27 slots full of an item the pot will not
     *     try to insert (so every inventoryInsert call fails),
     *   - a stack of dirt in the pot's first storage slot.
     * Then drives BotanyPotBlockEntity.tickPot directly in a tight loop with
     * S3 disabled vs enabled. Asserts >= 30% gain on the cumulative tick time.
     *
     * S3 ON path: the first attempt fails, the wrap schedules a backoff of
     * s3_hopper_backoff_min_ticks; subsequent ticks short-circuit at the
     * getBlockState wrap (returns AIR -> isAir() check skips the slot scan).
     */
    @GameTest(template = "empty_platform", timeoutTicks = 400)
    public static void s3MicrobenchProvesGain(GameTestHelper helper) {
        final Block hopperPot = BuiltInRegistries.BLOCK.get(BOTANY_HOPPER_POT_ID);
        final BlockPos chestRelPos = POT_POS.below();

        helper.setBlock(chestRelPos, Blocks.CHEST.defaultBlockState());
        if (!(helper.getBlockEntity(chestRelPos) instanceof ChestBlockEntity chest)) {
            helper.fail("Chest BE missing at " + chestRelPos);
            return;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }

        helper.setBlock(POT_POS, hopperPot.defaultBlockState());
        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity pot)) {
            helper.fail("Hopper pot BE missing at " + POT_POS);
            return;
        }
        final int firstStorageSlot = 3;
        pot.setItem(firstStorageSlot, new ItemStack(Items.DIRT, 64));

        final ServerLevel level = helper.getLevel();
        final BlockPos absPos = helper.absolutePos(POT_POS);
        final BlockState state = pot.getBlockState();

        final int warmupIters = 1_000;
        final int measureIters = 30_000;

        warmS3(level, absPos, state, pot, false, warmupIters);
        warmS3(level, absPos, state, pot, true, warmupIters);
        warmS3(level, absPos, state, pot, false, warmupIters);
        warmS3(level, absPos, state, pot, true, warmupIters);

        ArcadiaConfig.BOTANY.s3HopperBackoff.set(false);
        final long offStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
        }
        final long offNanos = System.nanoTime() - offStart;

        ArcadiaConfig.BOTANY.s3HopperBackoff.set(true);
        final long onStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
        }
        final long onNanos = System.nanoTime() - onStart;

        ArcadiaConfig.BOTANY.s3HopperBackoff.set(true);

        final double offMs   = offNanos / 1_000_000.0;
        final double onMs    = onNanos  / 1_000_000.0;
        final double speedup = (double) offNanos / onNanos;
        final double gainPct = (1.0 - (double) onNanos / offNanos) * 100.0;
        final double offNs   = offNanos / (double) measureIters;
        final double onNs    = onNanos  / (double) measureIters;

        ArcadiaTweaks.LOGGER.info(
                "[S3 microbench] iters={} | off={}ms ({} ns/tick) | on={}ms ({} ns/tick) | speedup={}x | gain={}%",
                measureIters,
                String.format(java.util.Locale.ROOT, "%.2f", offMs),
                String.format(java.util.Locale.ROOT, "%.1f", offNs),
                String.format(java.util.Locale.ROOT, "%.2f", onMs),
                String.format(java.util.Locale.ROOT, "%.1f", onNs),
                String.format(java.util.Locale.ROOT, "%.2f", speedup),
                String.format(java.util.Locale.ROOT, "%.1f", gainPct));

        final double minGainPct = 30.0;
        if (gainPct < minGainPct) {
            helper.fail("S3 microbench: only " + String.format(java.util.Locale.ROOT, "%.1f", gainPct)
                    + "% gain (threshold " + minGainPct + "%). off="
                    + String.format(java.util.Locale.ROOT, "%.2f", offMs) + "ms, on="
                    + String.format(java.util.Locale.ROOT, "%.2f", onMs) + "ms over " + measureIters + " ticks.");
            return;
        }
        helper.succeed();
    }

    private static void warmS3(ServerLevel level, BlockPos pos, BlockState state, BotanyPotBlockEntity pot, boolean s3On, int n) {
        ArcadiaConfig.BOTANY.s3HopperBackoff.set(s3On);
        for (int i = 0; i < n; i++) {
            BotanyPotBlockEntity.tickPot(level, pos, state, pot);
        }
    }
}
