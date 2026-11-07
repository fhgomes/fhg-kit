package io.github.fhgomes.fhgkit.llm.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.github.fhgomes.fhgkit.llm.AiCallOutcome;
import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import io.github.fhgomes.fhgkit.llm.ModelRef;
import io.github.fhgomes.fhgkit.llm.TokenUsage;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

/** Runs the client against the JDK's own HttpServer on the loopback interface; nothing leaves the machine. */
class LedgerClientTest {

    private static final String KEY = "ail_test_not-a-real-secret";
    private static final UUID ID = UUID.fromString("8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11");
    private static final String ACCEPTED_BODY = "{\"accepted\":1,\"duplicates\":0,\"rejected\":[]}";

    private final List<Seen> seen = new CopyOnWriteArrayList<>();
    private HttpServer server;

    /** What the fake ledger received. */
    private record Seen(String method, String path, String authorization, String contentType, String body) {
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    private LedgerClient clientAnswering(int status, String body) throws IOException {
        return clientWith(exchange -> reply(exchange, status, body), Duration.ofSeconds(5));
    }

    private LedgerClient clientWith(Handler handler, Duration timeout) throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                seen.add(new Seen(exchange.getRequestMethod(), exchange.getRequestURI().getPath(),
                        exchange.getRequestHeaders().getFirst("Authorization"),
                        exchange.getRequestHeaders().getFirst("Content-Type"),
                        new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8)));
                handler.handle(exchange);
            }
        });
        server.start();
        var base = URI.create("http://localhost:" + server.getAddress().getPort() + "/ai-ledger/");
        return new LedgerClient(new LedgerClientConfig(base, KEY, timeout));
    }

    @FunctionalInterface
    private interface Handler {
        void handle(HttpExchange exchange) throws IOException;
    }

    private static void reply(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length == 0 ? -1 : bytes.length);
        if (bytes.length > 0) {
            exchange.getResponseBody().write(bytes);
        }
    }

    private static AiUsageEvent event() {
        return AiUsageEvent.builder().id(ID).occurredAt(Instant.parse("2026-11-01T03:15:58.198Z"))
                .model(new ModelRef("openai-compatible", "model-a")).purpose("REVALIDATE").outcome(AiCallOutcome.OK)
                .tokens(TokenUsage.exact(10, 2)).latency(Duration.ofMillis(5)).build();
    }

    @Test
    @DisplayName("should POST the batch to <base>/api/integration/v1/usage-events with a bearer key and JSON body")
    void shouldPostToIngestPath() throws IOException {
        var client = clientAnswering(202, ACCEPTED_BODY);

        client.send(List.of(event()));

        assertThat(seen).hasSize(1);
        var request = seen.get(0);
        assertThat(request.method()).isEqualTo("POST");
        assertThat(request.path()).isEqualTo("/ai-ledger/api/integration/v1/usage-events");
        assertThat(request.authorization()).isEqualTo("Bearer " + KEY);
        assertThat(request.contentType()).isEqualTo("application/json");
        assertThat(request.body()).isEqualTo(UsageEventJson.write(List.of(event())));
        assertThat(request.body()).startsWith("{\"events\":[{\"id\":\"8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11\"");
    }

    @Test
    @DisplayName("should return the counts and rejections of a 202")
    void shouldParse202() throws IOException {
        var client = clientAnswering(202, "{\"accepted\":3,\"duplicates\":2,\"rejected\":["
                + "{\"id\":\"8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11\",\"reason\":\"INVALID_VALUE\"}]}");

        var result = client.send(List.of(event()));

        assertThat(result.httpStatus()).isEqualTo(202);
        assertThat(result.accepted()).isEqualTo(3);
        assertThat(result.duplicates()).isEqualTo(2);
        assertThat(result.rejected()).containsExactly(new SendResult.Rejected(ID, "INVALID_VALUE"));
    }

    @Test
    @DisplayName("should send the same batch twice without any retry of its own")
    void shouldNotRetryInside() throws IOException {
        var client = clientAnswering(503, "");

        assertThatThrownBy(() -> client.send(List.of(event()))).isInstanceOf(LedgerClientException.class);

        assertThat(seen).hasSize(1);
    }

    @Test
    @DisplayName("should throw with the status and without the key on 401")
    void shouldThrowOn401WithoutKey() throws IOException {
        var client = clientAnswering(401, "{\"title\":\"Unauthorized\",\"detail\":\"bad key " + KEY + "\"}");

        assertThatThrownBy(() -> client.send(List.of(event())))
                .isInstanceOfSatisfying(LedgerClientException.class, ex -> {
                    assertThat(ex.httpStatus()).isEqualTo(401);
                    assertThat(ex.getMessage()).contains("401").doesNotContain(KEY).doesNotContain("bad key");
                })
                .hasMessageNotContaining("ail_");
    }

    @Test
    @DisplayName("should throw on 413 and 500 carrying the status")
    void shouldThrowOnOtherNon2xx() throws IOException {
        var client = clientAnswering(413, "");

        assertThatThrownBy(() -> client.send(List.of(event())))
                .isInstanceOfSatisfying(LedgerClientException.class, ex -> assertThat(ex.httpStatus()).isEqualTo(413));
    }

    @Test
    @DisplayName("should not follow redirects, so the key never travels to another location")
    void shouldNotFollowRedirects() throws IOException {
        var client = clientWith(exchange -> {
            exchange.getResponseHeaders().add("Location", "http://localhost:1/elsewhere");
            exchange.sendResponseHeaders(302, -1);
        }, Duration.ofSeconds(5));

        assertThatThrownBy(() -> client.send(List.of(event())))
                .isInstanceOfSatisfying(LedgerClientException.class, ex -> assertThat(ex.httpStatus()).isEqualTo(302));
        assertThat(seen).hasSize(1);
    }

    @Test
    @DisplayName("should throw when a 2xx body is empty or unreadable, keeping the status")
    void shouldThrowOnUnreadable2xx() throws IOException {
        var client = clientAnswering(202, "<html>proxy page</html>");

        assertThatThrownBy(() -> client.send(List.of(event())))
                .isInstanceOf(LedgerClientException.class)
                .hasMessageContaining("202")
                .hasMessageContaining("unreadable")
                .hasMessageNotContaining("proxy page");
    }

    @Test
    @DisplayName("should time out when the ledger does not answer, keeping the timeout as cause")
    void shouldTimeOut() throws IOException {
        var client = clientWith(exchange -> {
            try {
                Thread.sleep(2_000);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            reply(exchange, 202, ACCEPTED_BODY);
        }, Duration.ofMillis(300));

        assertThatThrownBy(() -> client.send(List.of(event())))
                .isInstanceOf(LedgerClientException.class)
                .hasMessageContaining("timed out")
                .hasMessageNotContaining(KEY)
                .hasCauseInstanceOf(HttpTimeoutException.class);
    }

    @Test
    @DisplayName("should wrap a refused connection without leaking the key")
    void shouldWrapConnectionFailure() throws IOException {
        var client = clientAnswering(202, ACCEPTED_BODY);
        server.stop(0);
        server = null;

        assertThatThrownBy(() -> client.send(List.of(event())))
                .isInstanceOf(LedgerClientException.class)
                .hasMessageStartingWith("ledger request failed")
                .hasMessageNotContaining(KEY);
    }

    @Test
    @DisplayName("should refuse more than 500 events locally and accept exactly 500")
    void shouldEnforceBatchLimit() throws IOException {
        var client = clientAnswering(202, ACCEPTED_BODY);
        var batch = new ArrayList<AiUsageEvent>();
        for (int i = 0; i < 501; i++) {
            batch.add(event());
        }

        assertThatIllegalArgumentException().isThrownBy(() -> client.send(batch)).withMessageContaining("501");
        assertThat(seen).isEmpty();

        client.send(batch.subList(0, 500));
        assertThat(seen).hasSize(1);
    }

    @Test
    @DisplayName("should send nothing for an empty batch")
    void shouldSkipEmptyBatch() throws IOException {
        var client = clientAnswering(202, ACCEPTED_BODY);

        var result = client.send(List.of());

        assertThat(result).isEqualTo(new SendResult(0, 0, 0, List.of()));
        assertThat(seen).isEmpty();
    }

    @Test
    @DisplayName("should send non-ASCII text as UTF-8")
    void shouldSendUtf8() throws IOException {
        var client = clientAnswering(202, ACCEPTED_BODY);
        var withText = AiUsageEvent.builder().model(new ModelRef("p", "m")).purpose("EXTRA\u00C7\u00C3O")
                .outcome(AiCallOutcome.OK).correlationId("\uD83D\uDE00").build();

        client.send(List.of(withText));

        assertThat(seen.get(0).body()).contains("EXTRA\u00C7\u00C3O").contains("\uD83D\uDE00");
    }

    @Test
    @DisplayName("should not print the key in toString")
    void shouldNotPrintKey() throws IOException {
        var client = clientAnswering(202, ACCEPTED_BODY);

        assertThat(client.toString()).doesNotContain(KEY).contains("/ai-ledger/api/integration/v1/usage-events");
    }
}
