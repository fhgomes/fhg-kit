package io.github.fhgomes.fhgkit.llm.ledger;

import java.net.URI;
import java.time.Duration;
import java.util.Objects;

/**
 * Where and how a {@link LedgerClient} talks to the ledger.
 *
 * <p>{@link #toString()} never prints the API key.
 *
 * @param baseUri the ledger's base URI, absolute {@code http} or {@code https}, whose path <b>must end with
 *                {@code '/'}</b> (for example {@code https://ledger.example.invalid/ai-ledger/}); the ingest path is
 *                resolved against it, so a base without the trailing slash would silently lose its last segment
 * @param apiKey  the application key sent as a bearer token; visible ASCII only (no whitespace, control or non-ASCII
 *                characters)
 * @param timeout bounds both connecting and waiting for the response; must be positive
 */
public record LedgerClientConfig(URI baseUri, String apiKey, Duration timeout) {

    /**
     * Validates the configuration. The messages never contain the key.
     *
     * @throws NullPointerException     when a part is null
     * @throws IllegalArgumentException when the base URI is not an absolute http(s) URI ending in {@code '/'}, the
     *                                  key is blank or not visible ASCII, or the timeout is
     *                                  not positive
     */
    public LedgerClientConfig {
        Objects.requireNonNull(baseUri, "baseUri");
        Objects.requireNonNull(apiKey, "apiKey");
        Objects.requireNonNull(timeout, "timeout");
        String scheme = baseUri.getScheme();
        if (scheme == null || !(scheme.equalsIgnoreCase("http") || scheme.equalsIgnoreCase("https"))
                || baseUri.getHost() == null) {
            throw new IllegalArgumentException("baseUri must be an absolute http or https URI");
        }
        String path = baseUri.getPath();
        if (path == null || !path.endsWith("/")) {
            throw new IllegalArgumentException("baseUri path must end with '/'");
        }
        // Visible ASCII only: the JDK rejects anything else in a header value, and that exception would quote the key.
        if (apiKey.isEmpty() || apiKey.chars().anyMatch(c -> c < 0x21 || c > 0x7e)) {
            throw new IllegalArgumentException("apiKey must be non-empty visible ASCII (no whitespace, control or non-ASCII characters)");
        }
        if (timeout.isZero() || timeout.isNegative()) {
            throw new IllegalArgumentException("timeout must be positive, got " + timeout);
        }
    }

    @Override
    public String toString() {
        return "LedgerClientConfig[baseUri=" + baseUri + ", apiKey=***, timeout=" + timeout + "]";
    }
}
