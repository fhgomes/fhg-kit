# Changelog

All notable changes to this library are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow [SemVer](https://semver.org/).

## [Unreleased]

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
