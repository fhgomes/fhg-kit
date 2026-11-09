package io.github.fhgomes.fhgkit.llm.ledger.export;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.sun.net.httpserver.HttpServer;
import io.github.fhgomes.fhgkit.llm.AiCallOutcome;
import io.github.fhgomes.fhgkit.llm.AiUsageEvent;
import io.github.fhgomes.fhgkit.llm.ModelRef;
import io.github.fhgomes.fhgkit.llm.TokenUsage;
import io.github.fhgomes.fhgkit.llm.ledger.LedgerClient;
import io.github.fhgomes.fhgkit.llm.ledger.LedgerClientConfig;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.IntStream;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * One export pass against an in-memory source and store, a fake clock and a JDK HttpServer on the loopback interface
 * that answers with canned statuses; nothing leaves the machine.
 */
class LedgerExporterTest {

    private static final Instant NOW = Instant.parse("2026-11-09T12:00:00Z");
    private static final String KEY = "ail_test_not-a-real-secret";

    /** The application's row. */
    private record Row(String id, Instant createdAt) {
    }

    /** Rows kept in (createdAt, id) order; remembers what the exporter asked for. */
    private static final class MemorySource implements ExportSource<Row> {
        final List<Row> rows = new ArrayList<>();
        final List<Instant> settledBeforeSeen = new ArrayList<>();

        MemorySource add(Row row) {
            rows.add(row);
            rows.sort(Comparator.comparing(Row::createdAt).thenComparing(Row::id));
            return this;
        }

        @Override
        public List<Row> after(ExportCursor cursor, Instant settledBefore, int limit) {
            settledBeforeSeen.add(settledBefore);
            return rows.stream()
                    .filter(r -> r.createdAt().isAfter(cursor.createdAt())
                            || (r.createdAt().equals(cursor.createdAt()) && r.id().compareTo(cursor.id()) > 0))
                    .filter(r -> r.createdAt().isBefore(settledBefore))
                    .limit(limit)
                    .toList();
        }

        @Override
        public ExportCursor cursorOf(Row row) {
            return new ExportCursor(row.createdAt(), row.id());
        }
    }

    private static final class MemoryStore implements ExportCursorStore {
        ExportCursor current = ExportCursor.START;
        final List<ExportCursor> saves = new ArrayList<>();
        boolean failOnSave;

        @Override
        public ExportCursor load() {
            return current;
        }

        @Override
        public void save(ExportCursor cursor) {
            if (failOnSave) {
                throw new IllegalStateException("store down");
            }
            current = cursor;
            saves.add(cursor);
        }
    }

    private static final class MutableClock extends Clock {
        Instant now;

        MutableClock(Instant now) {
            this.now = now;
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return now;
        }
    }

    /** What the fake ledger received: the number of events in the body and the bearer header. */
    private record Request(int events, String authorization) {
    }

    private final MutableClock clock = new MutableClock(NOW);
    private final MemorySource source = new MemorySource();
    private final MemoryStore store = new MemoryStore();
    private final Queue<Integer> statuses = new ConcurrentLinkedQueue<>();
    private final Queue<String> bodies = new ConcurrentLinkedQueue<>();
    private final List<Request> requests = new CopyOnWriteArrayList<>();
    private HttpServer server;

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    // ---- fixtures ----------------------------------------------------------------------------------------

    private static String id(int n) {
        return String.format("row-%04d", n);
    }

    /** {@code count} settled rows, one per second ending two minutes before NOW. */
    private void rows(int count) {
        for (int i = 0; i < count; i++) {
            source.add(new Row(id(i), NOW.minus(Duration.ofMinutes(10)).plusSeconds(i)));
        }
    }

    private static Optional<AiUsageEvent> toEvent(Row row) {
        return Optional.of(AiUsageEvent.builder()
                .id(ExportEventIds.stable("test-app", "call", row.id()))
                .occurredAt(row.createdAt())
                .model(new ModelRef("provider-x", "model-a"))
                .purpose("TEST")
                .outcome(AiCallOutcome.OK)
                .tokens(TokenUsage.exact(10, 2))
                .build());
    }

    private LedgerClient ledger() throws IOException {
        server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
        server.createContext("/", exchange -> {
            try (exchange) {
                String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                int events = body.split("\"occurredAt\"", -1).length - 1;
                requests.add(new Request(events, exchange.getRequestHeaders().getFirst("Authorization")));
                Integer status = statuses.poll();
                int code = status == null ? 202 : status;
                String custom = bodies.poll();
                String answer = code == 202
                        ? (custom != null ? custom : "{\"accepted\":" + events + ",\"duplicates\":0,\"rejected\":[]}")
                        : "{}";
                byte[] bytes = answer.getBytes(StandardCharsets.UTF_8);
                exchange.getResponseHeaders().add("Content-Type", "application/json");
                exchange.sendResponseHeaders(code, bytes.length);
                exchange.getResponseBody().write(bytes);
            }
        });
        server.start();
        URI base = URI.create("http://localhost:" + server.getAddress().getPort() + "/ai-ledger/");
        return new LedgerClient(new LedgerClientConfig(base, KEY, Duration.ofSeconds(5)));
    }

    private LedgerExporter<Row> exporter(ExporterConfig config) throws IOException {
        return new LedgerExporter<>(ledger(), source, store, LedgerExporterTest::toEvent, config);
    }

    private ExporterConfig config() {
        return ExporterConfig.defaults().withClock(clock);
    }

    // ---- US2: the pass --------------------------------------------------------------------------------------

    @Test
    @DisplayName("should make 3 sends for 450 rows at batch size 200 and save the cursor after each 2xx")
    void shouldSendThreeBatchesAndSaveAfterEach() throws IOException {
        rows(450);

        var result = exporter(config().withBatchSize(200)).runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(200, 200, 50);
        assertThat(requests).extracting(Request::authorization).containsOnly("Bearer " + KEY);
        assertThat(store.saves).extracting(ExportCursor::id).containsExactly(id(199), id(399), id(449));
        assertThat(result).isEqualTo(new ExportRunResult(450, 0, 0, 0, 0, StopReason.DRAINED));
        assertThat(result.failed()).isFalse();
    }

    @Test
    @DisplayName("should send nothing on a second pass: the cursor is the state")
    void shouldSendNothingTheSecondTime() throws IOException {
        rows(30);
        var exporter = exporter(config());

        exporter.runOnce();
        var second = exporter.runOnce();

        assertThat(requests).hasSize(1);
        assertThat(second).isEqualTo(new ExportRunResult(0, 0, 0, 0, 0, StopReason.DRAINED));
    }

    @Test
    @DisplayName("should resume from the stored cursor, not from the start")
    void shouldResumeFromStoredCursor() throws IOException {
        rows(10);
        store.current = new ExportCursor(NOW.minus(Duration.ofMinutes(10)).plusSeconds(6), id(6));

        var result = exporter(config()).runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(3);
        assertThat(result.sent()).isEqualTo(3);
        assertThat(store.current.id()).isEqualTo(id(9));
    }

    @Test
    @DisplayName("should order ties on createdAt by id and not lose or repeat a row across batches")
    void shouldHandleTiesOnCreatedAt() throws IOException {
        Instant same = NOW.minus(Duration.ofMinutes(5));
        IntStream.range(0, 5).forEach(i -> source.add(new Row(id(i), same)));

        var result = exporter(config().withBatchSize(2)).runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(2, 2, 1);
        assertThat(result.sent()).isEqualTo(5);
        assertThat(store.saves).extracting(ExportCursor::id).containsExactly(id(1), id(3), id(4));
    }

    @ParameterizedTest(name = "HTTP {0} refuses the whole batch: skip it, move the cursor, carry on")
    @ValueSource(ints = {400, 413, 422})
    void shouldSkipABatchTheLedgerRefusesForGood(int status) throws IOException {
        rows(450);
        statuses.add(202);
        statuses.add(status);

        var result = exporter(config()).runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(200, 200, 50);
        assertThat(store.saves).extracting(ExportCursor::id).containsExactly(id(199), id(399), id(449));
        assertThat(result).isEqualTo(new ExportRunResult(250, 0, 0, 0, 1, StopReason.DRAINED));
    }

    @ParameterizedTest(name = "HTTP {0} stops the pass with AUTH and keeps the cursor at the last 2xx")
    @ValueSource(ints = {401, 403})
    void shouldStopOnAuthFailure(int status) throws IOException {
        rows(450);
        statuses.add(202);
        statuses.add(status);

        var result = exporter(config()).runOnce();

        assertThat(requests).hasSize(2);
        assertThat(store.saves).extracting(ExportCursor::id).containsExactly(id(199));
        assertThat(result).isEqualTo(new ExportRunResult(200, 0, 0, 0, 0, StopReason.AUTH));
        assertThat(result.failed()).isTrue();
    }

    @ParameterizedTest(name = "HTTP {0} stops the pass with UNAVAILABLE and keeps the cursor")
    @ValueSource(ints = {500, 503, 429})
    void shouldStopWhenTheLedgerIsUnavailable(int status) throws IOException {
        rows(450);
        statuses.add(status);

        var result = exporter(config()).runOnce();

        assertThat(requests).hasSize(1);
        assertThat(store.saves).isEmpty();
        assertThat(store.current).isEqualTo(ExportCursor.START);
        assertThat(result).isEqualTo(new ExportRunResult(0, 0, 0, 0, 0, StopReason.UNAVAILABLE));
    }

    @Test
    @DisplayName("should resend the same batch on the next pass after a 503 (the ledger sees ids it never stored)")
    void shouldResendTheSameBatchAfterAFailure() throws IOException {
        rows(5);
        statuses.add(503);
        var exporter = exporter(config());

        exporter.runOnce();
        var retry = exporter.runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(5, 5);
        assertThat(retry).isEqualTo(new ExportRunResult(5, 0, 0, 0, 0, StopReason.DRAINED));
    }

    @Test
    @DisplayName("should report UNAVAILABLE and keep the cursor when nothing answers at all")
    void shouldStopWhenNothingAnswers() throws IOException {
        rows(5);
        var client = ledger();
        server.stop(0);
        var exporter = new LedgerExporter<>(client, source, store, LedgerExporterTest::toEvent, config());

        var result = exporter.runOnce();

        assertThat(result.stoppedBy()).isEqualTo(StopReason.UNAVAILABLE);
        assertThat(store.saves).isEmpty();
    }

    @Test
    @DisplayName("should count a row the mapper cannot map in skippedRows and move past it")
    void shouldSkipUnmappableRows() throws IOException {
        rows(10);
        var client = ledger();
        ExportRowMapper<Row> mapper = row -> {
            if (row.id().equals(id(2))) {
                return Optional.empty();
            }
            if (row.id().equals(id(9))) {
                throw new IllegalArgumentException("purpose must not be blank");
            }
            return toEvent(row);
        };

        var result = new LedgerExporter<>(client, source, store, mapper, config()).runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(8);
        assertThat(store.current.id()).isEqualTo(id(9));
        assertThat(result).isEqualTo(new ExportRunResult(8, 0, 0, 2, 0, StopReason.DRAINED));
    }

    @Test
    @DisplayName("should send nothing but still move the cursor when every row of a batch is unmappable")
    void shouldMoveCursorWhenWholeBatchIsUnmappable() throws IOException {
        rows(4);
        var client = ledger();

        var result = new LedgerExporter<Row>(client, source, store, row -> Optional.empty(), config()).runOnce();

        assertThat(requests).isEmpty();
        assertThat(store.current.id()).isEqualTo(id(3));
        assertThat(result).isEqualTo(new ExportRunResult(0, 0, 0, 4, 0, StopReason.DRAINED));
    }

    @Test
    @DisplayName("should stop at maxBatchesPerRun with MAX_BATCHES and finish on the next pass")
    void shouldCapBatchesPerRun() throws IOException {
        rows(450);
        var exporter = exporter(config().withMaxBatchesPerRun(2));

        var first = exporter.runOnce();

        assertThat(requests).extracting(Request::events).containsExactly(200, 200);
        assertThat(store.current.id()).isEqualTo(id(399));
        assertThat(first).isEqualTo(new ExportRunResult(400, 0, 0, 0, 0, StopReason.MAX_BATCHES));

        var second = exporter.runOnce();

        assertThat(second).isEqualTo(new ExportRunResult(50, 0, 0, 0, 0, StopReason.DRAINED));
    }

    @Test
    @DisplayName("should hold back rows younger than the settle window, driven by the clock")
    void shouldHoldBackUnsettledRows() throws IOException {
        for (int i = 0; i < 3; i++) {
            source.add(new Row(id(i), NOW.minus(Duration.ofMinutes(2)).plusSeconds(i)));
        }
        for (int i = 3; i < 5; i++) {
            source.add(new Row(id(i), NOW.minus(Duration.ofSeconds(30)).plusSeconds(i)));
        }
        var exporter = exporter(config().withSettle(Duration.ofMinutes(1)));

        var first = exporter.runOnce();

        assertThat(source.settledBeforeSeen.get(0)).isEqualTo(NOW.minus(Duration.ofMinutes(1)));
        assertThat(first.sent()).isEqualTo(3);
        assertThat(store.current.id()).isEqualTo(id(2));

        clock.now = NOW.plus(Duration.ofMinutes(1));
        var second = exporter.runOnce();

        assertThat(second.sent()).isEqualTo(2);
        assertThat(store.current.id()).isEqualTo(id(4));
        assertThat(requests).extracting(Request::events).containsExactly(3, 2);
    }

    @Test
    @DisplayName("should count duplicates and per-event rejections from a 202 and not resend them")
    void shouldCountDuplicatesAndRejections() throws IOException {
        rows(5);
        bodies.add("{\"accepted\":3,\"duplicates\":1,\"rejected\":[{\"id\":\"8f0c1d7e-2b51-4a8e-9a43-1c2f0e9d1a11\","
                + "\"reason\":\"INVALID_VALUE\"}]}");

        var result = exporter(config()).runOnce();

        assertThat(result).isEqualTo(new ExportRunResult(5, 1, 1, 0, 0, StopReason.DRAINED));
        assertThat(store.current.id()).isEqualTo(id(4));
        assertThat(requests).hasSize(1);
    }

    @Test
    @DisplayName("should call nothing when the source is empty")
    void shouldDoNothingWithoutRows() throws IOException {
        var result = exporter(config()).runOnce();

        assertThat(requests).isEmpty();
        assertThat(result).isEqualTo(new ExportRunResult(0, 0, 0, 0, 0, StopReason.DRAINED));
    }

    @Test
    @DisplayName("should throw on a cursor-store failure (the caller logs it) but never on a send failure")
    void shouldThrowOnlyOnStoreFailure() throws IOException {
        rows(3);
        store.failOnSave = true;
        var exporter = exporter(config());

        assertThatThrownBy(exporter::runOnce).isInstanceOf(IllegalStateException.class).hasMessage("store down");

        statuses.add(503);
        store.failOnSave = false;
        assertThat(exporter.runOnce().stoppedBy()).isEqualTo(StopReason.UNAVAILABLE);
    }

    // ---- value types ----------------------------------------------------------------------------------------

    @Test
    @DisplayName("should derive a stable event id from app, kind and row id, different per row")
    void shouldDeriveStableIds() {
        var a = ExportEventIds.stable("app", "llm_usage_event", "42");

        assertThat(ExportEventIds.stable("app", "llm_usage_event", "42")).isEqualTo(a);
        assertThat(ExportEventIds.stable("app", "llm_usage_event", "43")).isNotEqualTo(a);
        assertThat(ExportEventIds.stable("other", "llm_usage_event", "42")).isNotEqualTo(a);
        assertThat(a).isEqualTo(java.util.UUID.nameUUIDFromBytes(
                "app:llm_usage_event:42".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("should validate the config and default to 200 rows, 20 batches, 1 minute")
    void shouldValidateConfig() {
        var defaults = ExporterConfig.defaults();

        assertThat(defaults.batchSize()).isEqualTo(200);
        assertThat(defaults.maxBatchesPerRun()).isEqualTo(20);
        assertThat(defaults.settle()).isEqualTo(Duration.ofMinutes(1));
        assertThat(defaults.withBatchSize(500).batchSize()).isEqualTo(500);
        for (int bad : new int[] {0, 501, -1}) {
            assertThatThrownBy(() -> defaults.withBatchSize(bad)).isInstanceOf(IllegalArgumentException.class);
        }
        assertThatThrownBy(() -> defaults.withMaxBatchesPerRun(0)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> defaults.withSettle(Duration.ofSeconds(-1))).isInstanceOf(IllegalArgumentException.class);
        assertThat(Set.of(StopReason.values())).hasSize(4);
    }
}
