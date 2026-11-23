package com.example.datingsite.service;

import java.util.List;
import java.util.function.Function;

/**
 * Одна сторінка результатів вибірки.
 *
 * @param content       елементи поточної сторінки
 * @param page          номер сторінки (з 0)
 * @param size          розмір сторінки
 * @param totalElements загальна кількість елементів, що відповідають фільтру
 * @param totalPages    загальна кількість сторінок
 */
public record PagedResult<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

    public static <T> PagedResult<T> of(List<T> all, int page, int size) {
        int total = all.size();
        int totalPages = (int) Math.ceil((double) total / size);
        int from = (int) Math.min((long) page * size, total);
        int to = Math.min(from + size, total);
        return new PagedResult<>(List.copyOf(all.subList(from, to)), page, size, total, totalPages);
    }

    public <R> PagedResult<R> map(Function<T, R> mapper) {
        return new PagedResult<>(content.stream().map(mapper).toList(), page, size, totalElements, totalPages);
    }
}
