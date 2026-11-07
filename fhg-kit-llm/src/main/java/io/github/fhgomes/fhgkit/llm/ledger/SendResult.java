package io.github.fhgomes.fhgkit.llm.ledger;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

/**
 * What the ledger said about one batch.
 *
 * @param httpStatus the HTTP status of the answer ({@code 202} on success); {@code 0} when nothing was sent
 *                   because the batch was empty
 * @param accepted   events stored by this call
 * @param duplicates events the ledger already had (same id), ignored
 * @param rejected   events the ledger refused, each with its reason; resending them unchanged is pointless
 */
public record SendResult(int httpStatus, int accepted, int duplicates, List<Rejected> rejected) {

    /** One event the ledger refused. */
    public record Rejected(UUID id, String reason) {

        /** @throws NullPointerException when a part is null */
        public Rejected {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(reason, "reason");
        }
    }

    /**
     * Validates the counts and copies {@code rejected} into an unmodifiable list.
     *
     * @throws IllegalArgumentException when a count is negative
     */
    public SendResult {
        if (accepted < 0 || duplicates < 0) {
            throw new IllegalArgumentException("counts must not be negative");
        }
        rejected = List.copyOf(Objects.requireNonNull(rejected, "rejected"));
    }

    /** The result of sending nothing. */
    static SendResult nothingSent() {
        return new SendResult(0, 0, 0, List.of());
    }
}
