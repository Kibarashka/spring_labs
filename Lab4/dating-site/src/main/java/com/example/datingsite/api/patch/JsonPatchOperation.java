package com.example.datingsite.api.patch;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import tools.jackson.databind.JsonNode;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

/**
 * Одна операція документа JSON Patch (RFC 6902).
 */
@Schema(description = "Операція JSON Patch (RFC 6902)")
public record JsonPatchOperation(

        @Schema(description = "Тип операції", example = "replace", requiredMode = REQUIRED)
        @NotNull(message = "Поле op є обов’язковим")
        Op op,

        @Schema(description = "JSON Pointer (RFC 6901) на поле документа", example = "/age", requiredMode = REQUIRED)
        @NotNull(message = "Поле path є обов’язковим")
        @Pattern(regexp = "^(/[^/]*)*$", message = "path має бути JSON Pointer, наприклад /age")
        String path,

        @Schema(description = "Джерело для операцій move та copy", example = "/keywords/0")
        @Pattern(regexp = "^(/[^/]*)*$", message = "from має бути JSON Pointer, наприклад /keywords/0")
        String from,

        @Schema(description = "Значення для операцій add, replace, test (будь-яке JSON-значення)",
                example = "25", implementation = Object.class)
        JsonNode value
) {

    public enum Op {
        @JsonProperty("add") ADD,
        @JsonProperty("remove") REMOVE,
        @JsonProperty("replace") REPLACE,
        @JsonProperty("move") MOVE,
        @JsonProperty("copy") COPY,
        @JsonProperty("test") TEST
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Для операцій add, replace і test потрібне поле value")
    public boolean isValuePresentWhenRequired() {
        boolean needsValue = op == Op.ADD || op == Op.REPLACE || op == Op.TEST;
        return !needsValue || value != null;
    }

    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Для операцій move і copy потрібне поле from")
    public boolean isFromPresentWhenRequired() {
        boolean needsFrom = op == Op.MOVE || op == Op.COPY;
        return !needsFrom || from != null;
    }
}
