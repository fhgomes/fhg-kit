package io.github.fhgomes.fhgkit.llm.ledger.export;

import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.UUID;

/** Stable event ids for rows whose own id is not a UUID. */
public final class ExportEventIds {

    private ExportEventIds() {
    }

    /**
     * {@code UUID.nameUUIDFromBytes("<app>:<kind>:<rowId>")}: the same row always gets the same id, across runs and
     * restarts, so a resend is a duplicate for the ledger and never a second row.
     *
     * @param app   the application name, as the ledger knows it
     * @param kind  the kind of row, for example the table name
     * @param rowId the row's own id
     */
    public static UUID stable(String app, String kind, String rowId) {
        Objects.requireNonNull(app, "app");
        Objects.requireNonNull(kind, "kind");
        Objects.requireNonNull(rowId, "rowId");
        return UUID.nameUUIDFromBytes((app + ":" + kind + ":" + rowId).getBytes(StandardCharsets.UTF_8));
    }
}
