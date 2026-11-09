package io.github.fhgomes.fhgkit.llm.ledger.export;

/** Where the application keeps the export cursor (typically a one-row table). */
public interface ExportCursorStore {

    /** The stored cursor, or {@link ExportCursor#START} when nothing was ever exported. */
    ExportCursor load();

    /** Persists the cursor; called only after the ledger answered 2xx (or refused the whole batch for good). */
    void save(ExportCursor cursor);
}
