# Changelog

All notable changes to this library are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow [SemVer](https://semver.org/).

## [Unreleased]

## [0.3.0] - 2026-11-09
### Added
- `fhg-kit-llm`: `io.github.fhgomes.fhgkit.llm.ledger.export` - one export pass from an application's own table to the
  ledger, the loop proven in two stars, written once (no scheduler, no SQL, no framework).
  - `LedgerExporter<R>(LedgerClient, ExportSource<R>, ExportCursorStore, ExportRowMapper<R>, ExporterConfig).runOnce()`
    returns an `ExportRunResult(sent, duplicates, rejected, skippedRows, skippedBatches, stoppedBy)`.
  - Policy: rows strictly after the `(createdAt, id)` cursor, oldest first, held back by a settle window; the cursor is
    saved after each 2xx; a batch the ledger refuses as a whole (400, 413, 422) is skipped and the cursor moves; 401 and
    403 stop the pass (`StopReason.AUTH`); 5xx, 429, timeouts and network errors stop it (`UNAVAILABLE`) with the cursor
    kept; a row the mapper cannot map is counted and passed over; `maxBatchesPerRun` caps the pass (`MAX_BATCHES`).
    A send failure never throws; a source or cursor-store failure propagates to the caller.
  - Ports the application implements: `ExportSource<R>`, `ExportCursorStore`, `ExportRowMapper<R>`.
    `ExporterConfig` defaults: 200 rows per request, 20 requests per pass, 1 minute settle window, system UTC clock.
  - `ExportEventIds.stable(app, kind, rowId)` derives a stable UUID for rows whose id is not a UUID.
- `fhg-kit-llm`: cache-aware tokens and cost.
  - `TokenUsage` gains `cacheRead` and `cacheWrite` (5 components) and `uncachedInput()`. `input` stays the whole
    prompt; the cache parts are parts of it (`cacheRead + cacheWrite <= input`, validated). New factory
    `TokenUsage.exact(input, output, cacheRead, cacheWrite)`.
  - `ModelPrice` gains optional `cacheReadPerMtok` and `cacheWritePerMtok` (7 components), `withCacheRates(...)` and
    `effectiveCacheReadPerMtok()` / `effectiveCacheWritePerMtok()`.
  - `DefaultCostEstimator` prices `input - cacheRead - cacheWrite` at the input rate and the cache parts at their rates;
    an absent cache rate falls back to the input rate (never cheaper by guess). Without a cache split the result is
    identical to 0.2.0.
  - The ledger JSON writer emits `tokens.cacheRead` / `tokens.cacheWrite` only when above zero (contract
    `usage-events` v1.1, additive); an event without a split serializes byte-identically to 0.2.0.
- `fhg-kit-spring-boot-starter`: `fhg.kit.llm.pricing.models[n].cache-read-per-mtok` and `cache-write-per-mtok`
  (both optional).

### Compatibility
- Source and binary compatible with 0.2.0: the 3-argument `TokenUsage` constructor, the 5-argument `ModelPrice`
  constructor, `TokenUsage.exact/estimated/zero` and `ModelPrice.of` are kept. The only thing that breaks is code that
  deconstructs these records with a record pattern (`TokenUsage(var i, var o, var e)`), which now needs the two extra
  components. Record `equals` now includes the cache parts.

## [0.2.0] - 2026-11-07
### Added
- `fhg-kit-llm`: `io.github.fhgomes.fhgkit.llm.ledger` - a framework-free client for an AI usage ledger service
  (built on `java.net.http`, no JSON library).
  - `LedgerClient.send(List<AiUsageEvent>)` POSTs a batch (1 to 500 events) to `api/integration/v1/usage-events`
    with `Authorization: Bearer <key>` and returns a `SendResult(httpStatus, accepted, duplicates, rejected)`.
    No retry inside: the caller keeps a cursor and resends the same batch after a failure; the ledger de-duplicates by
    event id.
  - `LedgerClientConfig(baseUri, apiKey, timeout)`: the base URI must be absolute http(s) and end with `/`; `toString()`
    never prints the key. Redirects are never followed.
  - `LedgerClientException` (unchecked, with `httpStatus()`) on IO errors, timeouts, non-2xx answers and unreadable
    2xx bodies; the message carries the status and never the key or the response body.
  - Batches over 500 events are refused locally with `IllegalArgumentException`; an empty batch sends nothing.
  - Event serialization (money as plain decimal strings, nulls written as `null`, full string escaping) and the answer
    parser are package-private and hand-written.
- Tests start a JDK `HttpServer` on the loopback interface; nothing leaves the machine.

## [0.1.0] - 2026-10-25
### Added
- `fhg-kit-llm`: LLM usage accounting, extracted from three internal applications.
  - `AiUsageEvent` (content-free usage ledger row with fluent `Builder`), `ModelRef`, `TokenUsage`, `AiCallOutcome`.
  - `AiUsageRecorder` port and `AiUsageRecorders` factories: `noop()`, `logging()`, `composite(...)`, `safe(...)`.
  - Cost: `ModelPrice`, `PriceTable` port, `InMemoryPriceTable` (exact id/alias match, duplicates rejected),
    `CostEstimator` port, `DefaultCostEstimator` (BigDecimal, scale 6, HALF_UP, per-Mtok) and `CostEstimate`
    with `ESTIMATED` / `UNKNOWN_MODEL` / `UNKNOWN_TOKENS` status.
  - Tracking: `AiUsageTracker.track(ctx, model, call)` measures latency, classifies failures
    (`FAILED_TIMEOUT`, `INVALID_OUTPUT`, `REFUSED`, `FAILED_UPSTREAM`), records through a fail-safe recorder and
    rethrows; `AiCallContext`, `AiCallResult`, `InvalidModelOutputException`, `ModelRefusedException`.
- `fhg-kit-spring-boot-starter`: `fhg.kit.llm.*` properties (`enabled`, `pricing.{currency,source,as-of,models}`)
  and auto-configured `PriceTable`, `CostEstimator`, logging `AiUsageRecorder` and `AiUsageTracker` beans,
  all `@ConditionalOnMissingBean`, gated by `fhg.kit.enabled` and `fhg.kit.llm.enabled`.

- Initial scaffold via projects-machine (`java-lib` template).
