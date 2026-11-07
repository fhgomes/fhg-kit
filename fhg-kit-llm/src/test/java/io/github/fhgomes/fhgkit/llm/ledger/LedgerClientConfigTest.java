package io.github.fhgomes.fhgkit.llm.ledger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.net.URI;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

class LedgerClientConfigTest {

    private static final URI BASE = URI.create("https://ledger.example.invalid/ai-ledger/");
    private static final Duration TIMEOUT = Duration.ofSeconds(5);

    @Test
    @DisplayName("should accept an http(s) base URI ending in a slash")
    void shouldAcceptValidConfig() {
        var config = new LedgerClientConfig(BASE, "key-1", TIMEOUT);

        assertThat(config.baseUri()).isEqualTo(BASE);
        assertThat(config.apiKey()).isEqualTo("key-1");
        assertThat(config.timeout()).isEqualTo(TIMEOUT);
    }

    @Test
    @DisplayName("should reject a base path without the trailing slash, which would drop its last segment")
    void shouldRejectMissingTrailingSlash() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LedgerClientConfig(URI.create("https://ledger.example.invalid/ai-ledger"), "k", TIMEOUT))
                .withMessageContaining("must end with '/'");
    }

    @Test
    @DisplayName("should reject relative URIs and other schemes")
    void shouldRejectNonHttpUris() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LedgerClientConfig(URI.create("/ai-ledger/"), "k", TIMEOUT));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LedgerClientConfig(URI.create("ftp://ledger.example.invalid/"), "k", TIMEOUT));
    }

    @Test
    @DisplayName("should reject blank keys, keys with whitespace or control characters, without echoing the key")
    void shouldRejectBadKeys() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LedgerClientConfig(BASE, "", TIMEOUT));
        assertThatIllegalArgumentException().isThrownBy(() -> new LedgerClientConfig(BASE, "  ", TIMEOUT));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LedgerClientConfig(BASE, "secret-part\r\nX-Injected: 1", TIMEOUT))
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("secret-part"));
    }

    @Test
    @DisplayName("should reject keys with non-ASCII characters, which the JDK refuses in a header, without echoing the key")
    void shouldRejectNonAsciiKeys() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LedgerClientConfig(BASE, "ail_secret-part-é-中", TIMEOUT))
                .satisfies(ex -> assertThat(ex.getMessage()).doesNotContain("secret-part"));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new LedgerClientConfig(BASE, "ail_secret-part-é", TIMEOUT));
    }

    @Test
    @DisplayName("should reject zero or negative timeouts and null parts")
    void shouldRejectBadTimeoutAndNulls() {
        assertThatIllegalArgumentException().isThrownBy(() -> new LedgerClientConfig(BASE, "k", Duration.ZERO));
        assertThatIllegalArgumentException().isThrownBy(() -> new LedgerClientConfig(BASE, "k", Duration.ofSeconds(-1)));
        assertThatNullPointerException().isThrownBy(() -> new LedgerClientConfig(null, "k", TIMEOUT));
        assertThatNullPointerException().isThrownBy(() -> new LedgerClientConfig(BASE, null, TIMEOUT));
        assertThatNullPointerException().isThrownBy(() -> new LedgerClientConfig(BASE, "k", null));
    }

    @Test
    @DisplayName("should never print the key in toString")
    void shouldRedactKeyInToString() {
        var text = new LedgerClientConfig(BASE, "super-secret-key", TIMEOUT).toString();

        assertThat(text).doesNotContain("super-secret-key").contains("ledger.example.invalid");
    }

    @Test
    @DisplayName("should keep the ledger package null-marked like the rest of the module")
    void packageIsNullMarked() {
        assertThat(getClass().getPackage().isAnnotationPresent(org.jspecify.annotations.NullMarked.class)).isTrue();
    }
}
