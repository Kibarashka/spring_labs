package com.example.datingsite.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

/**
 * Опис документа JSON Merge Patch (RFC 7386) для анкети - використовується лише в документації OpenAPI.
 * Передаються тільки поля, які потрібно змінити; значення null видаляє поле.
 */
@Schema(description = """
        Документ JSON Merge Patch (RFC 7386). Містить лише поля, які потрібно змінити.
        Поле зі значенням null видаляється (для publicInfo / privateInfo / keywords).
        Відсутні поля залишаються без змін.""")
public record ProfileMergePatch(
        @Schema(description = "Нове ім’я", example = "Anna", nullable = true) String name,
        @Schema(description = "Новий вік", example = "23", nullable = true) Integer age,
        @Schema(description = "Нова відкрита інформація або null для видалення",
                example = "Люблю гори", nullable = true) String publicInfo,
        @Schema(description = "Нова закрита інформація або null для видалення",
                nullable = true) String privateInfo,
        @Schema(description = "Новий повний список ключових слів", nullable = true) List<String> keywords
) {
}
