package io.github.fhgomes.fhgkit.llm.ledger.export;

import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import java.util.Optional;

/**
 * Turns one application row into an {@link AiUsageEvent}. The event id must be stable per row (the ledger
 * de-duplicates by it), see {@link ExportEventIds}.
 *
 * @param <R> the application's row type
 */
@FunctionalInterface
public interface ExportRowMapper<R> {

    /**
     * @return the event, or empty when the row has no usable shape. An {@link IllegalArgumentException} (for example
     *         from the {@code AiUsageEvent} builder) is treated the same way; either way the row is counted as
     *         skipped and the cursor moves past it, otherwise one bad row would block the export forever.
     */
    Optional<AiUsageEvent> map(R row);
}
