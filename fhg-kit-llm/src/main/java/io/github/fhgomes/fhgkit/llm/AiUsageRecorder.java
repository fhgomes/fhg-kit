package io.github.fhgomes.fhgkit.llm;

/**
 * Port: receives every {@link AiUsageEvent} and does something durable with it (append to a ledger table, emit a
 * metric, log a line).
 *
 * <p>Implementations should be fast or asynchronous, and should not throw; when they might, wrap them with
 * {@link AiUsageRecorders#safe(AiUsageRecorder)} so accounting can never break the model call.
 */
@FunctionalInterface
public interface AiUsageRecorder {

    /**
     * Records one event.
     *
     * @param event the event; never null
     */
    void record(AiUsageEvent event);
}
