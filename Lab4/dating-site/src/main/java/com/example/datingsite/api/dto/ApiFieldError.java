package com.example.datingsite.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Помилка валідації окремого поля або параметра")
public record ApiFieldError(

        @Schema(description = "Поле або параметр запиту", example = "age")
        String field,

        @Schema(description = "Опис помилки", example = "Сайт доступний лише повнолітнім (від 18 років)")
        String message
) {
}
