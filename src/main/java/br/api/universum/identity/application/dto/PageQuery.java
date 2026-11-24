package br.api.universum.identity.application.dto;

import java.util.List;

/**
 * Pedido de uma página: número da página (a partir de zero), tamanho e ordenação.
 *
 * @param sort ordenações por nome de propriedade, aplicadas em sequência
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
