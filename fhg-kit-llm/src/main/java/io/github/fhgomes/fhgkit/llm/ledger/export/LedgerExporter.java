package io.github.fhgomes.fhgkit.llm.ledger.export;

import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import io.github.fhgomes.fhgkit.llm.ledger.LedgerClient;
import io.github.fhgomes.fhgkit.llm.ledger.LedgerClientException;
import io.github.fhgomes.fhgkit.llm.ledger.SendResult;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * One export pass from an application's own table to an AI usage ledger: the policy proven in the first two stars,
 * written once.
 *
 * <p>{@link #runOnce()} loads the cursor, then up to {@link ExporterConfig#maxBatchesPerRun()} times: reads the next
 * batch of settled rows (oldest first), maps them to events, sends them with the {@link LedgerClient} and saves the
 * cursor at the last row of the batch.
 * <ul>
 *   <li><b>2xx</b>: the cursor moves. Duplicates and per-event rejections inside the answer are counted and not
 *       resent.</li>
 *   <li><b>400, 413, 422</b> (the ledger refused the whole batch for good): the batch is skipped and the cursor moves,
 *       otherwise the export would be stuck forever.</li>
 *   <li><b>401, 403</b>: the pass stops with {@link StopReason#AUTH}; the cursor stays.</li>
 *   <li><b>anything else</b> (5xx, 429, timeout, network): the pass stops with {@link StopReason#UNAVAILABLE}; the
 *       cursor stays and the next pass resends the same batch (the ledger de-duplicates by event id).</li>
 *   <li>a row the mapper cannot map is counted in {@code skippedRows} and passed over.</li>
 * </ul>
 *
 * <p>A send failure never throws: it is reported in {@link ExportRunResult#stoppedBy()}. A failure of the
 * {@link ExportSource} or the {@link ExportCursorStore} propagates to the caller (who logs it); the cursor was then not
 * moved past anything unsaved, so the next pass is safe. The log lines carry counts, statuses and row ids, never a
 * key or row content.
 *
 * <p>There is no scheduler: the application calls {@code runOnce()} when it wants (a timer, a feature flag, a lock).
 * Do not run two passes of the same export concurrently.
 *
 * @param <R> the application's row type
 */
public final class LedgerExporter<R> {

    private static final Logger log = LoggerFactory.getLogger(LedgerExporter.class);

    private final LedgerClient client;
    private final ExportSource<R> source;
    private final ExportCursorStore store;
    private final ExportRowMapper<R> mapper;
    private final ExporterConfig config;

    /**
     * @param client the ledger client (one per ledger, reused)
     * @param source the application's rows
     * @param store  where the cursor lives
     * @param mapper row to event
     * @param config batch size, cap, settle window and clock
     */
    public LedgerExporter(LedgerClient client, ExportSource<R> source, ExportCursorStore store,
            ExportRowMapper<R> mapper, ExporterConfig config) {
        this.client = Objects.requireNonNull(client, "client");
        this.source = Objects.requireNonNull(source, "source");
        this.store = Objects.requireNonNull(store, "store");
        this.mapper = Objects.requireNonNull(mapper, "mapper");
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Runs one pass. Idempotent: the cursor is the state.
     *
     * @return what the pass did and why it ended
     */
    public ExportRunResult runOnce() {
        Instant settledBefore = config.clock().instant().minus(config.settle());
        ExportCursor cursor = store.load();
        Totals totals = new Totals();
        for (int batch = 0; batch < config.maxBatchesPerRun(); batch++) {
            List<R> rows = source.after(cursor, settledBefore, config.batchSize());
            if (rows.isEmpty()) {
                return totals.result(StopReason.DRAINED);
            }
            List<AiUsageEvent> events = toEvents(rows, totals);
            if (!events.isEmpty()) {
                StopReason stop = send(events, totals);
                if (stop != null) {
                    return totals.result(stop);
                }
            }
            cursor = source.cursorOf(rows.get(rows.size() - 1));
            store.save(cursor);
            if (rows.size() < config.batchSize()) {
                return totals.result(StopReason.DRAINED);
            }
        }
        return totals.result(StopReason.MAX_BATCHES);
    }

    /** Sends one batch; returns the reason to stop the pass, or {@code null} to carry on (2xx or batch skipped). */
    private StopReason send(List<AiUsageEvent> events, Totals totals) {
        try {
            SendResult result = client.send(events);
            totals.sent += events.size();
            totals.duplicates += result.duplicates();
            totals.rejected += result.rejected().size();
            if (!result.rejected().isEmpty()) {
                log.warn("ai ledger rejected {} of {} events (first reason {}), not resent",
                        result.rejected().size(), events.size(), result.rejected().get(0).reason());
            }
            return null;
        } catch (LedgerClientException e) {
            int status = e.httpStatus();
            if (status == 400 || status == 413 || status == 422) {
                totals.skippedBatches++;
                log.warn("ai ledger refused a batch of {} events with HTTP {}, skipped", events.size(), status);
                return null;
            }
            if (status == 401 || status == 403) {
                log.warn("ai ledger export stopped, cursor kept: HTTP {} (check the API key)", status);
                return StopReason.AUTH;
            }
            log.warn("ai ledger export stopped, cursor kept: {} status={}", e.getClass().getSimpleName(), status);
            return StopReason.UNAVAILABLE;
        }
    }

    private List<AiUsageEvent> toEvents(List<R> rows, Totals totals) {
        List<AiUsageEvent> events = new ArrayList<>(rows.size());
        for (R row : rows) {
            Optional<AiUsageEvent> event;
            try {
                event = mapper.map(row);
            } catch (IllegalArgumentException e) {
                event = Optional.empty();
            }
            if (event.isPresent()) {
                events.add(event.get());
            } else {
                totals.skippedRows++;
                log.warn("ai ledger export skipped row {}: not mappable", source.cursorOf(row).id());
            }
        }
        return events;
    }

    /** Mutable counters of one pass. */
    private static final class Totals {
        int sent;
        int duplicates;
        int rejected;
        int skippedRows;
        int skippedBatches;

        ExportRunResult result(StopReason stoppedBy) {
            return new ExportRunResult(sent, duplicates, rejected, skippedRows, skippedBatches, stoppedBy);
        }
    }
}
