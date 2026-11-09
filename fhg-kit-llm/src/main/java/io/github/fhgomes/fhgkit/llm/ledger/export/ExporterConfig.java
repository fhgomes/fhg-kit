package io.github.fhgomes.fhgkit.llm.ledger.export;

import io.github.fhgomes.fhgkit.llm.ledger.LedgerClient;
import java.time.Clock;
import java.time.Duration;
import java.util.Objects;

/**
 * Knobs of one export pass.
 *
 * @param batchSize        rows (and events) per request, 1 to {@value LedgerClient#MAX_BATCH_SIZE}
 * @param maxBatchesPerRun requests per pass, at least 1; a larger backlog is worked off over several passes
 * @param settle           rows younger than this are held back (their transaction may not have committed); not negative
 * @param clock            the time source for the settle window
 */
public record ExporterConfig(int batchSize, int maxBatchesPerRun, Duration settle, Clock clock) {

    /** Default rows per request. */
    public static final int DEFAULT_BATCH_SIZE = 200;
    /** Default requests per pass. */
    public static final int DEFAULT_MAX_BATCHES_PER_RUN = 20;
    /** Default settle window. */
    public static final Duration DEFAULT_SETTLE = Duration.ofMinutes(1);

    /**
     * @throws IllegalArgumentException when a number is out of range
     * @throws NullPointerException     when {@code settle} or {@code clock} is null
     */
    public ExporterConfig {
        if (batchSize < 1 || batchSize > LedgerClient.MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "batchSize must be between 1 and " + LedgerClient.MAX_BATCH_SIZE + ", got " + batchSize);
        }
        if (maxBatchesPerRun < 1) {
            throw new IllegalArgumentException("maxBatchesPerRun must be at least 1, got " + maxBatchesPerRun);
        }
        Objects.requireNonNull(settle, "settle");
        if (settle.isNegative()) {
            throw new IllegalArgumentException("settle must not be negative, got " + settle);
        }
        Objects.requireNonNull(clock, "clock");
    }

    /** 200 rows per request, 20 requests per pass, 1 minute settle window, the system UTC clock. */
    public static ExporterConfig defaults() {
        return new ExporterConfig(DEFAULT_BATCH_SIZE, DEFAULT_MAX_BATCHES_PER_RUN, DEFAULT_SETTLE, Clock.systemUTC());
    }

    /** A copy with another batch size. */
    public ExporterConfig withBatchSize(int batchSize) {
        return new ExporterConfig(batchSize, maxBatchesPerRun, settle, clock);
    }

    /** A copy with another request cap per pass. */
    public ExporterConfig withMaxBatchesPerRun(int maxBatchesPerRun) {
        return new ExporterConfig(batchSize, maxBatchesPerRun, settle, clock);
    }

    /** A copy with another settle window. */
    public ExporterConfig withSettle(Duration settle) {
        return new ExporterConfig(batchSize, maxBatchesPerRun, settle, clock);
    }

    /** A copy with another clock. */
    public ExporterConfig withClock(Clock clock) {
        return new ExporterConfig(batchSize, maxBatchesPerRun, settle, clock);
    }
}
