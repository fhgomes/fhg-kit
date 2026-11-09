package io.github.fhgomes.fhgkit.llm.ledger.export;

import java.util.Objects;

/**
 * What one export pass did.
 *
 * @param sent           events in batches the ledger answered 2xx for; {@code duplicates} and {@code rejected} are
 *                       subsets of it (accepted = {@code sent - duplicates - rejected})
 * @param duplicates     events the ledger already had
 * @param rejected       events the ledger refused one by one (not resent)
 * @param skippedRows    rows the mapper could not turn into an event (passed over)
 * @param skippedBatches batches the ledger refused as a whole with 400, 413 or 422 (passed over, never resent)
 * @param stoppedBy      why the pass ended
 */
public record ExportRunResult(
        int sent, int duplicates, int rejected, int skippedRows, int skippedBatches, StopReason stoppedBy) {

    /**
     * @throws IllegalArgumentException when a count is negative
     * @throws NullPointerException     when {@code stoppedBy} is null
     */
    public ExportRunResult {
        if (sent < 0 || duplicates < 0 || rejected < 0 || skippedRows < 0 || skippedBatches < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        Objects.requireNonNull(stoppedBy, "stoppedBy");
    }

    /** {@code true} when the pass ended because of a failure the operator should look at. */
    public boolean failed() {
        return stoppedBy == StopReason.AUTH || stoppedBy == StopReason.UNAVAILABLE;
    }
}
