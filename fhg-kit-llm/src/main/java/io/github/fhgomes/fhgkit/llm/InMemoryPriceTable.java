package io.github.fhgomes.fhgkit.llm;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * {@link PriceTable} backed by a fixed list of {@link ModelPrice} entries.
 *
 * <p>Lookup is an exact, case-insensitive match of the trimmed {@link ModelRef#modelId()} against each entry's id
 * and aliases. There is deliberately no prefix or fuzzy matching: a dated snapshot that is priced differently
 * must be listed explicitly, otherwise the estimate is silently wrong. For the same reason an id or alias that
 * appears in more than one entry is rejected at construction.
 *
 * <p>Immutable and thread-safe.
 */
public final class InMemoryPriceTable implements PriceTable {

    private final List<ModelPrice> prices;
    private final Map<String, ModelPrice> index;
    private final String currency;
    private final String source;
    private final LocalDate asOf;

    /**
     * Builds the table and indexes every id and alias.
     *
     * @param prices   the entries; may be empty
     * @param currency ISO 4217 code shared by every entry, never blank
     * @param source   label of where the prices came from, never blank
     * @param asOf     date the prices were valid at
     * @throws IllegalArgumentException when an id or alias (after normalization) belongs to more than one entry,
     *                                  or when currency or source is blank
     */
    public InMemoryPriceTable(List<ModelPrice> prices, String currency, String source, LocalDate asOf) {
        this.prices = List.copyOf(Objects.requireNonNull(prices, "prices"));
        this.currency = Checks.requireNonBlank(currency, "currency");
        this.source = Checks.requireNonBlank(source, "source");
        this.asOf = Objects.requireNonNull(asOf, "asOf");
        this.index = index(this.prices);
    }

    /** An empty table: every lookup misses, every estimate is {@link CostEstimate.Status#UNKNOWN_MODEL}. */
    public static InMemoryPriceTable empty(String currency) {
        return new InMemoryPriceTable(List.of(), currency, "empty", LocalDate.EPOCH);
    }

    private static Map<String, ModelPrice> index(List<ModelPrice> prices) {
        var index = new HashMap<String, ModelPrice>();
        for (ModelPrice price : prices) {
            register(index, price.id(), price);
            for (String alias : price.aliases()) {
                register(index, alias, price);
            }
        }
        return Map.copyOf(index);
    }

    private static void register(Map<String, ModelPrice> index, String name, ModelPrice price) {
        String key = normalize(name);
        ModelPrice previous = index.putIfAbsent(key, price);
        if (previous != null && previous != price) {
            throw new IllegalArgumentException(
                    "'" + name + "' is declared by both '" + previous.id() + "' and '" + price.id() + "'");
        }
    }

    private static String normalize(String name) {
        return name.trim().toLowerCase(Locale.ROOT);
    }

    @Override
    public Optional<ModelPrice> lookup(ModelRef model) {
        Objects.requireNonNull(model, "model");
        return Optional.ofNullable(index.get(normalize(model.modelId())));
    }

    @Override
    public String currency() {
        return currency;
    }

    @Override
    public String source() {
        return source;
    }

    @Override
    public LocalDate asOf() {
        return asOf;
    }

    /** The entries this table was built from, in declaration order (unmodifiable). */
    public List<ModelPrice> prices() {
        return prices;
    }

    /** Number of entries (not of indexed names). */
    public int size() {
        return prices.size();
    }

    @Override
    public String toString() {
        return "InMemoryPriceTable[" + prices.size() + " models, " + currency + ", " + source + " as of " + asOf + "]";
    }
}
