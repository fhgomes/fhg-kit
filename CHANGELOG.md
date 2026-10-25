# Changelog

All notable changes to this library are documented here. Format: [Keep a Changelog](https://keepachangelog.com/en/1.1.0/), versions follow [SemVer](https://semver.org/).

## [Unreleased]

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
