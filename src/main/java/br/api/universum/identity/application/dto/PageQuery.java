package br.api.universum.identity.application.dto;

import java.util.List;

/**
 * A page request: zero-based page number, page size and sort order.
 *
 * @param sort sort orders by property name, applied in sequence
 */
public record PageQuery(int page, int size, List<SortOrder> sort) {

    public PageQuery {
        if (page < 0) {
            throw new IllegalArgumentException("Page must not be negative.");
        }
        if (size < 1) {
            throw new IllegalArgumentException("Page size must be at least 1.");
        }
        sort = sort == null ? List.of() : List.copyOf(sort);
    }

    public record SortOrder(String property, boolean ascending) {
    }
}
