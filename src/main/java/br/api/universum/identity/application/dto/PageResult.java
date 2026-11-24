package br.api.universum.identity.application.dto;

import java.util.List;
import java.util.function.Function;

/** One page of results, along with the total number of elements across all pages. */
public record PageResult<T>(List<T> content, PageQuery query, long totalElements) {

    public PageResult {
        content = List.copyOf(content);
    }

    public <R> PageResult<R> map(Function<? super T, ? extends R> mapper) {
        List<R> mapped = content.stream().<R>map(mapper).toList();
        return new PageResult<>(mapped, query, totalElements);
    }
}
