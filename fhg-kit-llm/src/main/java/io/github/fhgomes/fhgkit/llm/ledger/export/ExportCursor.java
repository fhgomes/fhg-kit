package io.github.fhgomes.fhgkit.llm.ledger.export;

import java.time.Instant;
import java.util.Objects;

/**
 * Position of the last exported row: rows strictly after {@code (createdAt, id)} in that order are still to be sent.
 * Ties on {@code createdAt} are common in batch writes, which is why the id is part of the cursor.
 *
 * @param createdAt creation time of the last exported row
 * @param id        id of the last exported row, in whatever textual form the source orders by
 */
public record ExportCursor(Instant createdAt, String id) {

    /** Before every row: nothing has been exported yet. */
    public static final ExportCursor START = new ExportCursor(Instant.EPOCH, "");

    /** @throws NullPointerException when a part is null */
    public ExportCursor {
        Objects.requireNonNull(createdAt, "createdAt");
        Objects.requireNonNull(id, "id");
    }
}
