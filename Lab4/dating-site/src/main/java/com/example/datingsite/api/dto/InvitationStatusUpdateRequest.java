package com.example.datingsite.api.dto;

import com.example.datingsite.model.InvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

/**
 * Часткове оновлення запрошення у форматі JSON Merge Patch (RFC 7386):
 * змінюється лише поле status.
 */
@Schema(description = "Документ JSON Merge Patch для запрошення: дозволено змінювати лише status")
public record InvitationStatusUpdateRequest(

        @Schema(description = "Новий стан: ACCEPTED (прийняти) або DECLINED (відхилити)",
                example = "ACCEPTED", allowableValues = {"ACCEPTED", "DECLINED"})
        @NotNull(message = "Вкажіть новий стан (status)")
        InvitationStatus status
) {
}
