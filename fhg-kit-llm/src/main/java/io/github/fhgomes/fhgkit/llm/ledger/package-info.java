/**
 * Client for an AI usage ledger service: sends {@link io.github.fhgomes.fhgkit.llm.AiUsageEvent}s over HTTPS/HTTP
 * with the JDK's {@code java.net.http} client.
 *
 * <p>Framework-free like the rest of the module: no JSON library, no retry policy. The caller keeps a cursor over
 * its own event source and advances it only after {@link io.github.fhgomes.fhgkit.llm.ledger.LedgerClient#send}
 * returns, so a failed send is retried by sending the same batch again (the ledger de-duplicates by event id).
 */
@NullMarked
package io.github.fhgomes.fhgkit.llm.ledger;

import org.jspecify.annotations.NullMarked;
