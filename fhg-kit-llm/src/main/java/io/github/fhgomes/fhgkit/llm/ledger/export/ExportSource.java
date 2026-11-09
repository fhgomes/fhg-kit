package io.github.fhgomes.fhgkit.llm.ledger.export;

import java.time.Instant;
import java.util.List;

/**
 * The application's rows to export (its outbox: the table it already writes once per model call).
 *
 * @param <R> the application's row type
 */
public interface ExportSource<R> {

    /**
     * Rows strictly after {@code cursor} in {@code (createdAt, id)} order, oldest first, created before
     * {@code settledBefore} (a row whose transaction is still open can commit with a creation time behind the
     * cursor, so the youngest rows are held back), at most {@code limit}.
     */
    List<R> after(ExportCursor cursor, Instant settledBefore, int limit);

    /** The cursor positioned on {@code row}. */
    ExportCursor cursorOf(R row);
}
