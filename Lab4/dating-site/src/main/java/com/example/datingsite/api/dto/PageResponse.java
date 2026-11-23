package com.example.datingsite.api.dto;

import com.example.datingsite.service.PagedResult;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.function.Function;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Сторінка результатів")
public record PageResponse<T>(

        @Schema(description = "Елементи поточної сторінки", requiredMode = REQUIRED)
        List<T> content,

        @Schema(description = "Номер сторінки (з 0)", example = "0", requiredMode = REQUIRED)
        int page,

        @Schema(description = "Розмір сторінки", example = "10", requiredMode = REQUIRED)
        int size,

        @Schema(description = "Загальна кількість елементів, що відповідають фільтру", example = "5",
                requiredMode = REQUIRED)
        long totalElements,

        @Schema(description = "Загальна кількість сторінок", example = "1", requiredMode = REQUIRED)
        int totalPages,

        @Schema(description = "Чи є сторінка першою", example = "true", requiredMode = REQUIRED)
        boolean first,

        @Schema(description = "Чи є сторінка останньою", example = "true", requiredMode = REQUIRED)
        boolean last
) {

    public static <S, T> PageResponse<T> from(PagedResult<S> result, Function<S, T> mapper) {
        return new PageResponse<>(
                result.content().stream().map(mapper).toList(),
                result.page(),
                result.size(),
                result.totalElements(),
                result.totalPages(),
                result.page() == 0,
                result.page() >= result.totalPages() - 1);
    }
}
