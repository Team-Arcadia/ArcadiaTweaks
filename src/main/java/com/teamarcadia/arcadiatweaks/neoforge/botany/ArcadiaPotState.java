package com.teamarcadia.arcadiatweaks.neoforge.botany;

/**
 * Bridge interface that exposes ArcadiaTweaks' per-pot scratch state to static
 * Mixin handlers.
 *
 * Why this exists: {@code @Unique} fields declared on a Mixin class become real
 * fields on the target class only after the Mixin transformer runs, but the
 * Java source compiler cannot see them. Static handlers (the wraps and inject
 * targeting BotanyPotBlockEntity.tickPot, which is itself static) only receive
 * the BlockEntity through a parameter, so they cannot use the Mixin {@code this}
 * trick to read those fields.
 *
 * The Mixin class implements this interface and exposes accessors for each
 * scratch field; the transformer adds the interface to BotanyPotBlockEntity at
 * load time. Handlers then cast {@code (ArcadiaPotState) pot} to read/write.
 */
public interface ArcadiaPotState {

    int arcadia$getHopperBackoff();
    void arcadia$setHopperBackoff(int value);

    int arcadia$getConsecutiveFailures();
    void arcadia$setConsecutiveFailures(int value);

    int arcadia$getTickInsertAttempts();
    void arcadia$setTickInsertAttempts(int value);
    void arcadia$incrementTickInsertAttempts();

    int arcadia$getTickInsertSuccesses();
    void arcadia$setTickInsertSuccesses(int value);
    void arcadia$incrementTickInsertSuccesses();

    // A1 - getRequiredGrowthTicks cache.
    // requiredTicksRemaining encodes both validity and the safety-net TTL: a
    // positive value means the cached integer is still good for that many
    // calls; zero means the cache is invalid and the next call must
    // re-compute.
    int arcadia$getCachedRequiredTicks();
    void arcadia$setCachedRequiredTicks(int value);

    int arcadia$getRequiredTicksRemaining();
    void arcadia$setRequiredTicksRemaining(int value);
    void arcadia$decrementRequiredTicksRemaining();

    // S2 - tick coalescing phase counter. When the active N is > 1, only
    // 1 game tick in N runs the full tickPot body; the others are
    // cancelled at HEAD. The TickAccumulator.tickUp/tickDown wrapping
    // amplifies the per-call delta to keep effective growth/cooldown
    // speed identical to N=1.
    int arcadia$getCoalescePhase();
    void arcadia$setCoalescePhase(int value);
}
