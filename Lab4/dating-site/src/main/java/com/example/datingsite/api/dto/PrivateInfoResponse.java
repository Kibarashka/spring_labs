package com.example.datingsite.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Закрита інформація анкети")
public record PrivateInfoResponse(

        @Schema(description = "Ідентифікатор анкети", example = "2", requiredMode = REQUIRED)
        Long profileId,

        @Schema(description = "Закрита інформація (контакти); null, якщо не заповнена",
                example = "oleg@example.com", nullable = true)
        String privateInfo
) {
}
