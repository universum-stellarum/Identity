package br.api.universum.identity.domain.model;

/** Verificações de invariantes compartilhadas pelos modelos do domínio. */
final class Guard {

    private Guard() {
    }

    static String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }

    static <T> T requireNonNull(T value, String message) {
        if (value == null) {
            throw new IllegalArgumentException(message);
        }
        return value;
    }
}
