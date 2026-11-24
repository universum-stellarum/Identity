package br.api.universum.identity.application.dto;

import java.util.List;
import java.util.function.Function;

/** Uma página de resultados, com o total de elementos somando todas as páginas. */
public record PageResult<T>(List<T> content, PageQuery query, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, query, totalElements);
    }
}
