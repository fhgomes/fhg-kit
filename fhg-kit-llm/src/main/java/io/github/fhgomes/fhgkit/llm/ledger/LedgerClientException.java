package io.github.fhgomes.fhgkit.llm.ledger;

import org.jspecify.annotations.Nullable;

/**
 * A send to the ledger failed: transport error, timeout, a non-2xx answer or an unreadable 2xx body.
 *
 * <p>The message carries the HTTP status when there is one and never the API key or the response body. A timeout
 * keeps the original {@link java.net.http.HttpTimeoutException} as cause, so a caller can still tell it apart.
 */
public class LedgerClientException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final int httpStatus;

    /** A failure without an HTTP answer (status {@code 0}). */
    public LedgerClientException(String message, @Nullable Throwable cause) {
        super(message, cause);
        this.httpStatus = 0;
    }

    /** A failure on an HTTP answer with the given status. */
    public LedgerClientException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    /** The HTTP status the ledger answered with, or {@code 0} when there was no usable answer. */
    public int httpStatus() {
        return httpStatus;
    }
}
