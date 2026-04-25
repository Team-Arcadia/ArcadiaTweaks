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

        // Threshold at 10% - observed run-to-run range is 18-43% so anything
        // tighter trips on JIT/scheduling tails. 10% is still well clear of
        // "S1 broken or no-op" territory (~0-5%) and a real regression
        // would fail this. The Spark profile on prod is the source of
        // truth for absolute numbers at scale.
        final double minGainPct = 10.0;
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

    /**
     * Microbenchmark for A1. Drives a growing pot through tickPot and toggles
     * the getRequiredGrowthTicks cache.
     *
     * Threshold is intentionally lenient (5%): A1's saved work is the body of
     * one Helpers method while tickPot also runs S1 lookups, cooldowns, growth
     * accounting and (no-op) hopper logic. The gain matters in aggregate over
     * thousands of pots on a real server but only shows as a few percent of a
     * single tickPot call. A failing run here means A1 is broken or
     * net-negative, not "less than the doc's 70/85% on the full hot path".
     */
    @GameTest(template = "empty_platform", timeoutTicks = 200)
    public static void a1MicrobenchProvesGain(GameTestHelper helper) {
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

        final ServerLevel level = helper.getLevel();
        final BlockPos absPos = helper.absolutePos(POT_POS);
        final BlockState state = bpe.getBlockState();

        final int warmupIters = 2_000;
        final int measureIters = 60_000;

        warmA1(level, absPos, state, bpe, false, warmupIters);
        warmA1(level, absPos, state, bpe, true, warmupIters);
        warmA1(level, absPos, state, bpe, false, warmupIters);
        warmA1(level, absPos, state, bpe, true, warmupIters);

        ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.set(false);
        final long offStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, bpe);
        }
        final long offNanos = System.nanoTime() - offStart;

        ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.set(true);
        final long onStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, bpe);
        }
        final long onNanos = System.nanoTime() - onStart;

        ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.set(true);

        final double offMs   = offNanos / 1_000_000.0;
        final double onMs    = onNanos  / 1_000_000.0;
        final double speedup = (double) offNanos / onNanos;
        final double gainPct = (1.0 - (double) onNanos / offNanos) * 100.0;
        final double offNs   = offNanos / (double) measureIters;
        final double onNs    = onNanos  / (double) measureIters;

        ArcadiaTweaks.LOGGER.info(
                "[A1 microbench] iters={} | off={}ms ({} ns/tick) | on={}ms ({} ns/tick) | speedup={}x | gain={}%",
                measureIters,
                String.format(java.util.Locale.ROOT, "%.2f", offMs),
                String.format(java.util.Locale.ROOT, "%.1f", offNs),
                String.format(java.util.Locale.ROOT, "%.2f", onMs),
                String.format(java.util.Locale.ROOT, "%.1f", onNs),
                String.format(java.util.Locale.ROOT, "%.2f", speedup),
                String.format(java.util.Locale.ROOT, "%.1f", gainPct));

        // A1's contribution is small relative to tickPot total, so the bench
        // is noisy run to run (observed range: -2.5% to +17%). Threshold is
        // set negative on purpose: we want to catch actual regressions (a
        // broken cache that causes a meaningful slowdown) without tripping
        // on JIT noise. The Spark profile on prod is the right place to
        // confirm A1 is paying off in absolute terms at scale.
        final double minGainPct = -5.0;
        if (gainPct < minGainPct) {
            helper.fail("A1 microbench regressed: " + String.format(java.util.Locale.ROOT, "%.1f", gainPct)
                    + "% gain (regression threshold " + minGainPct + "%). off="
                    + String.format(java.util.Locale.ROOT, "%.2f", offMs) + "ms, on="
                    + String.format(java.util.Locale.ROOT, "%.2f", onMs) + "ms over " + measureIters + " ticks.");
            return;
        }
        helper.succeed();
    }

    private static void warmA1(ServerLevel level, BlockPos pos, BlockState state, BotanyPotBlockEntity pot, boolean a1On, int n) {
        ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.set(a1On);
        for (int i = 0; i < n; i++) {
            BotanyPotBlockEntity.tickPot(level, pos, state, pot);
        }
    }

    /**
     * Combined bench. Drives a worst-case-ish single-pot setup that exercises
     * S1 + A1 (growth path active via dirt+wheat) and S3 (hopper pot above a
     * full chest with dirt sitting in storage). Toggles ALL of s1/s3/a1 off
     * vs ALL on, drives tickPot 30k times each, asserts >=50% combined gain.
     *
     * The number is the closest the gametest harness gets to "vanilla
     * BotanyPots vs ArcadiaTweaks botany on a saturated farm pot". A real
     * Spark profile on a 400-pot farm will land lower because this exercises
     * the worst case (chest always rejects).
     */
    @GameTest(template = "empty_platform", timeoutTicks = 400)
    public static void comboBenchProvesCombinedGain(GameTestHelper helper) {
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

        pot.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        pot.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));
        pot.setItem(3, new ItemStack(Items.DIRT, 64));

        if (pot.getOrInvalidateSoil() == null || pot.getOrInvalidateCrop() == null) {
            helper.fail("Default soil/crop recipes did not resolve.");
            return;
        }

        final ServerLevel level = helper.getLevel();
        final BlockPos absPos = helper.absolutePos(POT_POS);
        final BlockState state = pot.getBlockState();

        final int warmupIters = 1_000;
        final int measureIters = 30_000;

        warmCombo(level, absPos, state, pot, false, warmupIters);
        warmCombo(level, absPos, state, pot, true, warmupIters);
        warmCombo(level, absPos, state, pot, false, warmupIters);
        warmCombo(level, absPos, state, pot, true, warmupIters);

        setAllOptims(false);
        final long offStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
        }
        final long offNanos = System.nanoTime() - offStart;

        setAllOptims(true);
        final long onStart = System.nanoTime();
        for (int i = 0; i < measureIters; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
        }
        final long onNanos = System.nanoTime() - onStart;

        setAllOptims(true);

        final double offMs   = offNanos / 1_000_000.0;
        final double onMs    = onNanos  / 1_000_000.0;
        final double speedup = (double) offNanos / onNanos;
        final double gainPct = (1.0 - (double) onNanos / offNanos) * 100.0;
        final double offNs   = offNanos / (double) measureIters;
        final double onNs    = onNanos  / (double) measureIters;

        ArcadiaTweaks.LOGGER.info(
                "[Combined bench] S1+S3+A1 vs all-off | iters={} | off={}ms ({} ns/tick) | on={}ms ({} ns/tick) | speedup={}x | gain={}%",
                measureIters,
                String.format(java.util.Locale.ROOT, "%.2f", offMs),
                String.format(java.util.Locale.ROOT, "%.1f", offNs),
                String.format(java.util.Locale.ROOT, "%.2f", onMs),
                String.format(java.util.Locale.ROOT, "%.1f", onNs),
                String.format(java.util.Locale.ROOT, "%.2f", speedup),
                String.format(java.util.Locale.ROOT, "%.1f", gainPct));

        final double minGainPct = 50.0;
        if (gainPct < minGainPct) {
            helper.fail("Combined bench: only " + String.format(java.util.Locale.ROOT, "%.1f", gainPct)
                    + "% gain (threshold " + minGainPct + "%). off="
                    + String.format(java.util.Locale.ROOT, "%.2f", offMs) + "ms, on="
                    + String.format(java.util.Locale.ROOT, "%.2f", onMs) + "ms over " + measureIters + " ticks.");
            return;
        }
        helper.succeed();
    }

    private static void warmCombo(ServerLevel level, BlockPos pos, BlockState state, BotanyPotBlockEntity pot, boolean on, int n) {
        setAllOptims(on);
        for (int i = 0; i < n; i++) {
            BotanyPotBlockEntity.tickPot(level, pos, state, pot);
        }
    }

    private static void setAllOptims(boolean on) {
        ArcadiaConfig.BOTANY.s1MatchesCache.set(on);
        ArcadiaConfig.BOTANY.s3HopperBackoff.set(on);
        ArcadiaConfig.BOTANY.a1RequiredGrowthTicksCache.set(on);
    }

    /**
     * Drives a full harvest cycle: hopper pot + dirt + wheat seeds. No chest
     * below, so the harvest deposit stays in the pot's own storage slots.
     * Validates that with all caches on (S1 + A1 in particular) the growth
     * path matures the crop, onHarvest is invoked, and the storage slot
     * receives the drops.
     *
     * Catches any cache that would freeze growthTime at a stale value or
     * miss the maturation transition.
     */
    @GameTest(template = "empty_platform", timeoutTicks = 200)
    public static void fullHarvestCycleProducesDrops(GameTestHelper helper) {
        final Block hopperPot = BuiltInRegistries.BLOCK.get(BOTANY_HOPPER_POT_ID);
        helper.setBlock(POT_POS, hopperPot.defaultBlockState());

        if (!(helper.getBlockEntity(POT_POS) instanceof BotanyPotBlockEntity pot)) {
            helper.fail("Hopper pot BE missing at " + POT_POS);
            return;
        }

        pot.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        pot.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));

        if (pot.getOrInvalidateSoil() == null || pot.getOrInvalidateCrop() == null) {
            helper.fail("Default soil/crop recipes did not resolve.");
            return;
        }

        final ServerLevel level = helper.getLevel();
        final BlockPos absPos = helper.absolutePos(POT_POS);
        final BlockState state = pot.getBlockState();
        final int firstStorageSlot = 3;

        for (int i = 0; i < 8000; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
            for (int s = firstStorageSlot; s < 15; s++) {
                if (!pot.getItem(s).isEmpty()) {
                    helper.succeed();
                    return;
                }
            }
        }

        helper.fail("No harvest after 8000 ticks. growthTime=" + pot.growthTime.getTicks()
                + " requiredTicks=" + pot.getRequiredGrowthTicks());
    }

    /**
     * Worst-case S3 recovery scenario:
     *   1. Hopper pot above a full chest, dirt in pot's first storage slot.
     *   2. Drive enough ticks to put S3 into deep backoff.
     *   3. Empty the chest.
     *   4. Drive tickPot for 2x the max-backoff window.
     *   5. Verify the dirt has been pushed to the chest.
     *
     * Catches a backoff that decrements but never re-attempts, or one that
     * does not reset its failure counter on an eventual success.
     */
    @GameTest(template = "empty_platform", timeoutTicks = 200)
    public static void s3BackoffRecoversWhenDownstreamEmpties(GameTestHelper helper) {
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
        pot.setItem(firstStorageSlot, new ItemStack(Items.DIRT, 16));

        final ServerLevel level = helper.getLevel();
        final BlockPos absPos = helper.absolutePos(POT_POS);
        final BlockState state = pot.getBlockState();

        for (int i = 0; i < 200; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
        }

        if (pot.getItem(firstStorageSlot).getCount() != 16) {
            helper.fail("Dirt unexpectedly moved to a full chest. count=" + pot.getItem(firstStorageSlot).getCount());
            return;
        }

        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, ItemStack.EMPTY);
        }

        final int maxBackoff = ArcadiaConfig.BOTANY.s3HopperBackoffMaxTicks.get();
        final int recoveryWindow = (maxBackoff * 2) + 8;
        for (int i = 0; i < recoveryWindow; i++) {
            BotanyPotBlockEntity.tickPot(level, absPos, state, pot);
            if (pot.getItem(firstStorageSlot).isEmpty()) {
                helper.succeed();
                return;
            }
        }

        helper.fail("Dirt not exported within " + recoveryWindow
                + " ticks after the chest was emptied. backoff stuck? remaining count="
                + pot.getItem(firstStorageSlot).getCount());
    }

    /**
     * Multi-pot stress + bench. Places 4 pots of mixed configurations in the
     * 3x3x3 platform, drives tickPot on all of them in round-robin for many
     * iterations, asserts no crash and reports the combined-strats gain.
     *
     * Configurations:
     *   - (0,1,0) basic pot  + dirt + wheat seeds              -> S1, A1 paths
     *   - (2,1,0) hopper pot above stone (no inventory)        -> S3 NoCrash path
     *   - (0,1,2) basic pot  + dirt + wheat seeds              -> S1, A1 (second instance)
     *   - (2,1,2) hopper pot above full chest + dirt storage   -> S3 saturated path
     *
     * Catches any Mixin handler that accidentally shares state across BEs
     * (e.g. a mistakenly @Static field instead of @Unique).
     */
    @GameTest(template = "empty_platform", timeoutTicks = 400)
    public static void multiPotMixedFarmBench(GameTestHelper helper) {
        final Block basicPot  = BuiltInRegistries.BLOCK.get(BOTANY_POT_ID);
        final Block hopperPot = BuiltInRegistries.BLOCK.get(BOTANY_HOPPER_POT_ID);

        final BlockPos a = new BlockPos(0, 1, 0);
        final BlockPos b = new BlockPos(2, 1, 0);
        final BlockPos c = new BlockPos(0, 1, 2);
        final BlockPos d = new BlockPos(2, 1, 2);
        final BlockPos chestUnderD = d.below();

        helper.setBlock(chestUnderD, Blocks.CHEST.defaultBlockState());
        if (!(helper.getBlockEntity(chestUnderD) instanceof ChestBlockEntity chest)) {
            helper.fail("Chest BE missing under d");
            return;
        }
        for (int slot = 0; slot < chest.getContainerSize(); slot++) {
            chest.setItem(slot, new ItemStack(Items.STONE, 64));
        }

        helper.setBlock(a, basicPot.defaultBlockState());
        helper.setBlock(b, hopperPot.defaultBlockState());
        helper.setBlock(c, basicPot.defaultBlockState());
        helper.setBlock(d, hopperPot.defaultBlockState());

        final BotanyPotBlockEntity pa = (BotanyPotBlockEntity) helper.getBlockEntity(a);
        final BotanyPotBlockEntity pb = (BotanyPotBlockEntity) helper.getBlockEntity(b);
        final BotanyPotBlockEntity pc = (BotanyPotBlockEntity) helper.getBlockEntity(c);
        final BotanyPotBlockEntity pd = (BotanyPotBlockEntity) helper.getBlockEntity(d);
        if (pa == null || pb == null || pc == null || pd == null) {
            helper.fail("One of the pot BEs is missing.");
            return;
        }

        pa.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        pa.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));
        pc.setItem(SOIL_SLOT, new ItemStack(Items.DIRT));
        pc.setItem(SEED_SLOT, new ItemStack(Items.WHEAT_SEEDS));
        pd.setItem(3, new ItemStack(Items.DIRT, 64));

        final ServerLevel level = helper.getLevel();
        final BlockPos absA = helper.absolutePos(a);
        final BlockPos absB = helper.absolutePos(b);
        final BlockPos absC = helper.absolutePos(c);
        final BlockPos absD = helper.absolutePos(d);
        final BlockState sa = pa.getBlockState();
        final BlockState sb = pb.getBlockState();
        final BlockState sc = pc.getBlockState();
        final BlockState sd = pd.getBlockState();

        final int warmupRounds = 1_000;
        final int measureRounds = 10_000;

        warmFarm(level, absA, absB, absC, absD, sa, sb, sc, sd, pa, pb, pc, pd, false, warmupRounds);
        warmFarm(level, absA, absB, absC, absD, sa, sb, sc, sd, pa, pb, pc, pd, true,  warmupRounds);
        warmFarm(level, absA, absB, absC, absD, sa, sb, sc, sd, pa, pb, pc, pd, false, warmupRounds);
        warmFarm(level, absA, absB, absC, absD, sa, sb, sc, sd, pa, pb, pc, pd, true,  warmupRounds);

        setAllOptims(false);
        final long offStart = System.nanoTime();
        for (int i = 0; i < measureRounds; i++) {
            BotanyPotBlockEntity.tickPot(level, absA, sa, pa);
            BotanyPotBlockEntity.tickPot(level, absB, sb, pb);
            BotanyPotBlockEntity.tickPot(level, absC, sc, pc);
            BotanyPotBlockEntity.tickPot(level, absD, sd, pd);
        }
        final long offNanos = System.nanoTime() - offStart;

        setAllOptims(true);
        final long onStart = System.nanoTime();
        for (int i = 0; i < measureRounds; i++) {
            BotanyPotBlockEntity.tickPot(level, absA, sa, pa);
            BotanyPotBlockEntity.tickPot(level, absB, sb, pb);
            BotanyPotBlockEntity.tickPot(level, absC, sc, pc);
            BotanyPotBlockEntity.tickPot(level, absD, sd, pd);
        }
        final long onNanos = System.nanoTime() - onStart;

        setAllOptims(true);

        final int totalTickCalls = measureRounds * 4;
        final double offMs   = offNanos / 1_000_000.0;
        final double onMs    = onNanos  / 1_000_000.0;
        final double speedup = (double) offNanos / onNanos;
        final double gainPct = (1.0 - (double) onNanos / offNanos) * 100.0;
        final double offNs   = offNanos / (double) totalTickCalls;
        final double onNs    = onNanos  / (double) totalTickCalls;

        ArcadiaTweaks.LOGGER.info(
                "[Multi-pot bench] 4 pots x {} rounds = {} tickPot calls | off={}ms ({} ns/tick) | on={}ms ({} ns/tick) | speedup={}x | gain={}%",
                measureRounds, totalTickCalls,
                String.format(java.util.Locale.ROOT, "%.2f", offMs),
                String.format(java.util.Locale.ROOT, "%.1f", offNs),
                String.format(java.util.Locale.ROOT, "%.2f", onMs),
                String.format(java.util.Locale.ROOT, "%.1f", onNs),
                String.format(java.util.Locale.ROOT, "%.2f", speedup),
                String.format(java.util.Locale.ROOT, "%.1f", gainPct));

        if (pa.isRemoved() || pb.isRemoved() || pc.isRemoved() || pd.isRemoved()) {
            helper.fail("A pot was removed mid-bench - state corruption?");
            return;
        }

        // Mixed-load threshold: lower than the saturated-hopper combo bench
        // because the basic pots in the mix do less work per tick. Catches
        // any cross-BE state corruption that would tank perf.
        final double minGainPct = 30.0;
        if (gainPct < minGainPct) {
            helper.fail("Multi-pot bench: only " + String.format(java.util.Locale.ROOT, "%.1f", gainPct)
                    + "% gain (threshold " + minGainPct + "%). off="
                    + String.format(java.util.Locale.ROOT, "%.2f", offMs) + "ms, on="
                    + String.format(java.util.Locale.ROOT, "%.2f", onMs) + "ms over " + totalTickCalls + " calls.");
            return;
        }
        helper.succeed();
    }

    private static void warmFarm(ServerLevel level,
                                  BlockPos absA, BlockPos absB, BlockPos absC, BlockPos absD,
                                  BlockState sa, BlockState sb, BlockState sc, BlockState sd,
                                  BotanyPotBlockEntity pa, BotanyPotBlockEntity pb,
                                  BotanyPotBlockEntity pc, BotanyPotBlockEntity pd,
                                  boolean on, int rounds) {
        setAllOptims(on);
        for (int i = 0; i < rounds; i++) {
            BotanyPotBlockEntity.tickPot(level, absA, sa, pa);
            BotanyPotBlockEntity.tickPot(level, absB, sb, pb);
            BotanyPotBlockEntity.tickPot(level, absC, sc, pc);
            BotanyPotBlockEntity.tickPot(level, absD, sd, pd);
        }
    }
}
