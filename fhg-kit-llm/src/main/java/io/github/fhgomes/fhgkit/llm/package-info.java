/**
 * LLM usage accounting: a provider-agnostic usage event, recorders to persist or log it, a price table with
 * cost estimation, and a tracker that wraps a model call and emits the event for you.
 *
 * <p>The package is framework-free: Java 21 and {@code org.jspecify} annotations only. The public API is made of
 * records (values), interfaces (ports an application plugs an adapter into), a few small final classes and one
 * enum. Nothing here talks to a model provider; the consumer makes the call and hands the result to the kit.
 *
 * <h2>Building blocks</h2>
 * <ul>
 *   <li>{@link io.github.fhgomes.fhgkit.llm.AiUsageEvent} — one model call, after the fact: model, purpose,
 *       outcome, tokens, latency, who asked, and an optional cost snapshot. Never carries prompt content.</li>
 *   <li>{@link io.github.fhgomes.fhgkit.llm.AiUsageRecorder} — the port that receives events;
 *       {@link io.github.fhgomes.fhgkit.llm.AiUsageRecorders} has no-op, logging, composite and fail-safe
 *       implementations.</li>
 *   <li>{@link io.github.fhgomes.fhgkit.llm.PriceTable} and {@link io.github.fhgomes.fhgkit.llm.CostEstimator} —
 *       per-million-token prices supplied by the consumer and the arithmetic that turns tokens into money.</li>
 *   <li>{@link io.github.fhgomes.fhgkit.llm.AiUsageTracker} — wraps a {@code Callable} that performs the call,
 *       measures latency, classifies failures and records the event without ever masking the result.</li>
 * </ul>
 *
 * <p>The package is {@link org.jspecify.annotations.NullMarked}: every parameter and return type is non-null unless
 * annotated {@link org.jspecify.annotations.Nullable}.
 */
@NullMarked
package io.github.fhgomes.fhgkit.llm;

import org.jspecify.annotations.NullMarked;
