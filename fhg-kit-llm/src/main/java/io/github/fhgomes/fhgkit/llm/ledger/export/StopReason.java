package io.github.fhgomes.fhgkit.llm.ledger.export;

/** Why one export pass ended. */
public enum StopReason {

    /** The source had nothing more to send (a batch came back short or empty). */
    DRAINED,

    /** {@link ExporterConfig#maxBatchesPerRun()} was reached with rows still waiting; the next pass continues. */
    MAX_BATCHES,

    /** The ledger answered 401 or 403 (wrong or revoked key). The cursor stays; fix the key. */
    AUTH,

    /** A network error, timeout, 5xx or any other unexpected answer. The cursor stays; the next pass resends. */
    UNAVAILABLE
}
