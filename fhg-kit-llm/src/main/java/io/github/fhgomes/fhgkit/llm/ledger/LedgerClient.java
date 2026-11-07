package io.github.fhgomes.fhgkit.llm.ledger;

import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

/**
 * Sends {@link AiUsageEvent}s to an AI usage ledger ({@code POST api/integration/v1/usage-events}, bearer API key).
 *
 * <p>One call, one request: <b>no retry inside</b>. The caller owns the cursor over its event source, sends a batch,
 * and moves the cursor only when {@link #send} returns; after a {@link LedgerClientException} it sends the same
 * batch again later. The ledger de-duplicates by event id, so a repeated batch is reported as
 * {@link SendResult#duplicates()} rather than stored twice.
 *
 * <p>Thread-safe; create one per ledger and reuse it. The API key is only ever put in the {@code Authorization}
 * header, never in a message, a log line or {@code toString()}.
 */
public final class LedgerClient {

    /** The ledger's batch limit; larger batches are refused locally with {@link IllegalArgumentException}. */
    public static final int MAX_BATCH_SIZE = 500;

    /** Ingest path, resolved against {@link LedgerClientConfig#baseUri()}. */
    static final String INGEST_PATH = "api/integration/v1/usage-events";

    private final URI endpoint;
    private final String authorization;
    private final LedgerClientConfig config;
    private final HttpClient http;

    /** Creates a client for the given ledger. */
    public LedgerClient(LedgerClientConfig config) {
        this.config = Objects.requireNonNull(config, "config");
        this.endpoint = config.baseUri().resolve(INGEST_PATH);
        this.authorization = "Bearer " + config.apiKey();
        this.http = HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .followRedirects(HttpClient.Redirect.NEVER)
                .connectTimeout(config.timeout())
                .build();
    }

    /**
     * Sends one batch.
     *
     * @param events between 1 and {@value #MAX_BATCH_SIZE} events; an empty list sends nothing and returns a result
     *               with {@code httpStatus} 0
     * @return what the ledger accepted, ignored as duplicates, and rejected
     * @throws IllegalArgumentException when the batch has more than {@value #MAX_BATCH_SIZE} events
     * @throws LedgerClientException    on a transport error, a timeout, a non-2xx answer (the status is in the
     *                                  message and in {@link LedgerClientException#httpStatus()}) or an unreadable
     *                                  2xx body
     */
    public SendResult send(List<AiUsageEvent> events) {
        Objects.requireNonNull(events, "events");
        if (events.size() > MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "batch has " + events.size() + " events, the maximum is " + MAX_BATCH_SIZE);
        }
        if (events.isEmpty()) {
            return SendResult.nothingSent();
        }
        HttpRequest request = HttpRequest.newBuilder(endpoint)
                .timeout(config.timeout())
                .header("Authorization", authorization)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(UsageEventJson.write(events), StandardCharsets.UTF_8))
                .build();
        HttpResponse<String> response = execute(request);
        int status = response.statusCode();
        if (status < 200 || status > 299) {
            throw new LedgerClientException("ledger answered HTTP " + status, status);
        }
        try {
            return UsageEventJson.parseResponse(status, response.body());
        } catch (IllegalArgumentException ex) {
            throw new LedgerClientException("ledger answered HTTP " + status + " with an unreadable body", ex);
        }
    }

    private HttpResponse<String> execute(HttpRequest request) {
        try {
            return http.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (HttpTimeoutException ex) {
            throw new LedgerClientException("ledger request timed out after " + config.timeout(), ex);
        } catch (IOException ex) {
            throw new LedgerClientException("ledger request failed: " + ex.getClass().getSimpleName(), ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new LedgerClientException("ledger request interrupted", ex);
        }
    }

    @Override
    public String toString() {
        return "LedgerClient[" + endpoint + "]";
    }
}
