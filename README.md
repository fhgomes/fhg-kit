# fhg-kit

fhg-kit — framework-free shared library for the fhgomes constellation of micro-SaaS products (LLM usage ledger and cost estimation first; credentials, flags, audit and integration contracts to follow)

Shared library (constellation **Kit**): versioned, framework-free core modules, no business logic, no secrets.
Applications pin one version in `gradle.properties` and upgrade deliberately.

## Modules

| Module | Purpose |
|---|---|
| `fhg-kit-bom` | Bill of materials — import it and omit versions on the other modules |
| `fhg-kit-llm` | LLM usage accounting: a content-free `AiUsageEvent` per model call, `AiUsageRecorder` port (no-op, logging, composite, fail-safe), per-million-token `PriceTable` with `CostEstimator`, an `AiUsageTracker` that wraps the call and records the event for you, a `LedgerClient` that ships the events to an AI usage ledger service, and a `LedgerExporter` that runs the export loop over your own table |
| `fhg-kit-spring-boot-starter` | Spring Boot auto-configuration for the modules above (the only module that knows Spring) |

## fhg-kit-llm in 30 seconds

Plain Java, no framework. The kit never talks to a provider: you make the call, you hand back the facts only the call site knows (served model, tokens), the kit measures, classifies and records.

```java
import io.github.fhgomes.fhgkit.llm.*;

// 1. Prices come from you (or from the starter's properties). Exact match on id/alias, never by prefix.
PriceTable prices = new InMemoryPriceTable(
        List.of(new ModelPrice("model-a", Set.of("model-a-2026-01-01"), new BigDecimal("3.00"), new BigDecimal("15.00"), 128_000)),
        "USD", "provider price list", LocalDate.of(2026, 1, 1));

// 2. Where events go: your ledger adapter, a log line, or several at once. `safe` means a broken ledger never breaks a call.
AiUsageRecorder recorder = AiUsageRecorders.composite(AiUsageRecorders.logging(), myLedgerAdapter);
AiUsageTracker tracker = new AiUsageTracker(recorder, new DefaultCostEstimator(prices), Clock.systemUTC());

// 3. Wrap the call. Latency, outcome, tokens, cost snapshot and context end up in one AiUsageEvent.
String summary = tracker.track(
        AiCallContext.builder("summarize").tenantId(tenantId).correlationId(requestId).attribute("promptVersion", "v2").build(),
        new ModelRef("my-provider", "model-a"),
        () -> {
            var response = client.chat(prompt);                       // your SDK, your exceptions
            if (response.refused()) throw new ModelRefusedException("blocked by provider");   // -> REFUSED
            return AiCallResult.of(response.text(), new ModelRef("my-provider", response.model()),
                    TokenUsage.exact(response.inputTokens(), response.outputTokens()));
        });
```

Outcomes: `OK`, `FAILED_TIMEOUT` (a `TimeoutException`/`HttpTimeoutException` anywhere in the cause chain), `INVALID_OUTPUT` (`InvalidModelOutputException`), `REFUSED` (`ModelRefusedException`), `FAILED_UPSTREAM` (anything else). The exception is always rethrown; the event carries only the exception class name, never a message or prompt content.

Cost is `BigDecimal` at scale 6, `HALF_UP`, per million tokens. `CostEstimate.status()` says whether the number means anything: `ESTIMATED`, `UNKNOWN_MODEL` (not in the table) or `UNKNOWN_TOKENS` (tokens estimated as zero, i.e. not known). Leave the tracker's estimator `null` to store events unpriced and price them on read.

### Starter: `fhg.kit.llm.pricing`

With `fhg-kit-spring-boot-starter` on the classpath you get `PriceTable`, `CostEstimator`, a logging `AiUsageRecorder` and an `AiUsageTracker` as beans (each `@ConditionalOnMissingBean`, so declaring your own `AiUsageRecorder` bean replaces the default). `fhg.kit.llm.enabled=false` turns all of them off.

```yaml
fhg:
  kit:
    llm:
      enabled: true
      pricing:
        currency: USD
        source: "provider price list"      # label for humans
        as-of: 2026-01-01                  # date the prices were valid at
        models:
          - id: model-a
            aliases: [model-a-2026-01-01, gateway/model-a]
            input-per-mtok: 3.00
            output-per-mtok: 15.00
            context-window: 128000
          - id: model-tiny
            input-per-mtok: 0.10
            output-per-mtok: 0.40
```

An entry with only one side of the price, or an id/alias declared twice, fails the context at startup instead of producing a silently wrong estimate.

### Shipping events to a ledger: `LedgerClient`

`io.github.fhgomes.fhgkit.llm.ledger` sends batches of `AiUsageEvent` to an AI usage ledger service over HTTP (`java.net.http`, no JSON library, no framework). The client does one request per call and **never retries**: you keep a cursor over your own event source and move it only after `send` returns.

```java
import io.github.fhgomes.fhgkit.llm.ledger.*;

// baseUri must end with '/': the ingest path "api/integration/v1/usage-events" is resolved against it.
var config = new LedgerClientConfig(URI.create("https://ledger.example.invalid/ai-ledger/"), apiKey, Duration.ofSeconds(10));
var client = new LedgerClient(config);

try {
    SendResult result = client.send(batch);          // 1..500 events
    // result.accepted(), result.duplicates() (already stored, ignored), result.rejected() (id + reason, do not resend)
    cursor.advanceTo(lastEventOf(batch));
} catch (LedgerClientException e) {
    // transport error, timeout, non-2xx (e.httpStatus()) or unreadable answer: keep the cursor, send the same batch later
}
```

The ledger de-duplicates by event id, so resending a batch whose first attempt actually arrived is safe. The key is only sent in the `Authorization` header; it never appears in exception messages, `toString()` or logs, and redirects are not followed. A batch over 500 events throws `IllegalArgumentException` before any request; an empty batch sends nothing.

### Cache-aware cost

Prompt caching changes the price of the same prompt by an order of magnitude, so `TokenUsage` can carry the split. `input` is always the **whole** prompt; `cacheRead` and `cacheWrite` are parts of it.

```java
// 36 000 prompt tokens, of which 35 000 were read from the cache and 900 written to it
TokenUsage usage = TokenUsage.exact(36_000, 100, 35_000, 900);

ModelPrice price = new ModelPrice("model-a", Set.of(), new BigDecimal("3.00"), new BigDecimal("15.00"), null,
        new BigDecimal("0.30"), new BigDecimal("3.75"));          // cache read / cache write per Mtok
// (100 x 3.00 + 35 000 x 0.30 + 900 x 3.75 + 100 x 15.00) / 1e6 = 0.015675   (0.1095 without the split)
```

A cache rate you do not declare falls back to the input rate, so a missing price never makes a call look cheaper. In the starter: `cache-read-per-mtok` / `cache-write-per-mtok` next to `input-per-mtok`. The ledger writer sends `tokens.cacheRead` / `tokens.cacheWrite` only when above zero.

### Exporting a table to the ledger: `LedgerExporter`

If your application already writes one row per model call, that table is the outbox: `io.github.fhgomes.fhgkit.llm.ledger.export` runs the export loop over it. You provide three small ports; the kit provides the loop. No scheduler (you call `runOnce()` from your timer, behind your flag) and no SQL.

```java
import io.github.fhgomes.fhgkit.llm.ledger.export.*;

ExportSource<CallRow> source = new ExportSource<>() {             // your table
    public List<CallRow> after(ExportCursor cursor, Instant settledBefore, int limit) { /* rows strictly after (createdAt, id), oldest first, createdAt < settledBefore */ }
    public ExportCursor cursorOf(CallRow row) { return new ExportCursor(row.createdAt(), row.id()); }
};
ExportCursorStore store = /* load() / save(cursor): a one-row table */;
ExportRowMapper<CallRow> mapper = row -> Optional.of(AiUsageEvent.builder()
        .id(ExportEventIds.stable("my-app", "llm_call", row.id()))   // stable per row: a resend is a duplicate, never a second row
        /* ... */ .build());

var exporter = new LedgerExporter<>(ledgerClient, source, store, mapper, ExporterConfig.defaults());
ExportRunResult result = exporter.runOnce();    // every 5 minutes, say
```

One pass sends up to 20 batches of 200 rows and saves the cursor after each 2xx. A batch the ledger refuses as a whole (400, 413, 422) is skipped so the export cannot get stuck; 401/403 (`StopReason.AUTH`) and 5xx, 429 or network errors (`UNAVAILABLE`) stop the pass with the cursor kept, and the next pass resends the same batch. A row the mapper cannot map (empty `Optional` or `IllegalArgumentException`) is counted in `skippedRows` and passed over. A send failure never throws; a failure of your source or store does, so you log it.

## Consume

```kotlin
// settings.gradle.kts or build.gradle.kts of the consuming app
repositories {
    mavenCentral()
    maven {
        name = "GitHubPackages"
        url = uri("https://maven.pkg.github.com/fhgomes/fhg-kit")
        credentials {
            // GitHub Packages requires a token even for public packages (read:packages scope).
            username = System.getenv("GITHUB_ACTOR") ?: (findProperty("gpr.user") as String?)
            password = System.getenv("GITHUB_TOKEN") ?: (findProperty("gpr.key") as String?)
        }
    }
}

dependencies {
    implementation(platform("io.github.fhgomes:fhg-kit-bom:${property("fhgKitVersion")}"))
    implementation("io.github.fhgomes:fhg-kit-llm")
    implementation("io.github.fhgomes:fhg-kit-spring-boot-starter") // Spring Boot apps only
}
```

`gradle.properties` of the app: `fhgKitVersion=0.3.0`.

## Build

```bash
./gradlew build                 # compile + test every module
./gradlew publishToMavenLocal   # try a snapshot in a local app before releasing
```

## Release

1. Update `libVersion` in `gradle.properties` and move `[Unreleased]` entries in `CHANGELOG.md` under the new version.
2. Commit, then tag: `git tag v$(grep ^libVersion gradle.properties | cut -d= -f2) && git push --tags`.
3. The `publish` workflow builds, checks the tag against `libVersion` and publishes every module to GitHub Packages.

## Rules

- **No business logic.** A module enters the kit only after the same need exists in two applications.
- **Core modules are framework-free** (Java 21, `org.jspecify` only). Spring wiring lives in `fhg-kit-spring-boot-starter`.
- **Ports, not implementations.** Anything an app must plug in is an interface here and an adapter there.
- **SemVer.** Breaking a public type means a major bump and a `CHANGELOG` entry with a migration note.
- **Open-source hygiene.** No credentials, hostnames, internal URLs or personal data in code, tests, or docs. Everything configurable comes from the consumer. Run a secrets/data-exposure review before every release.

## License

Apache License 2.0 — see `LICENSE`.
