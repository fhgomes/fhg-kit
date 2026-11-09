/**
 * One export pass from a star's own table to an AI usage ledger: {@link
 * io.github.fhgomes.fhgkit.llm.ledger.export.LedgerExporter#runOnce()} reads rows after a cursor, maps them to
 * {@link io.github.fhgomes.fhgkit.llm.AiUsageEvent}s, sends them with a {@link
 * io.github.fhgomes.fhgkit.llm.ledger.LedgerClient} and moves the cursor.
 *
 * <p>The application plugs in the three things only it knows: an {@link
 * io.github.fhgomes.fhgkit.llm.ledger.export.ExportSource} over its table, an {@link
 * io.github.fhgomes.fhgkit.llm.ledger.export.ExportCursorStore} and an {@link
 * io.github.fhgomes.fhgkit.llm.ledger.export.ExportRowMapper}. There is no scheduler, no SQL and no framework here:
 * the application decides when to call {@code runOnce()} (a flag, a timer, a lock).
 */
@NullMarked
package io.github.fhgomes.fhgkit.llm.ledger.export;

import org.jspecify.annotations.NullMarked;
