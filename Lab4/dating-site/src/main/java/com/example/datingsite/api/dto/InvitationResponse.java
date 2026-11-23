package com.example.datingsite.api.dto;

import com.example.datingsite.model.Invitation;
import com.example.datingsite.model.InvitationStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Запрошення від одного користувача іншому")
public record InvitationResponse(

        @Schema(description = "Ідентифікатор запрошення", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "Id анкети відправника", example = "1", requiredMode = REQUIRED)
        Long senderId,

        @Schema(description = "Id анкети отримувача", example = "2", requiredMode = REQUIRED)
        Long receiverId,

        @Schema(description = "Стан запрошення", example = "PENDING", requiredMode = REQUIRED)
        InvitationStatus status,

        @Schema(description = "Дата і час створення", example = "2026-11-21T12:30:00", requiredMode = REQUIRED)
        LocalDateTime createdAt
) {

    public static InvitationResponse from(Invitation invitation) {
        return new InvitationResponse(invitation.getId(), invitation.getSenderId(),
                invitation.getReceiverId(), invitation.getStatus(), invitation.getCreatedAt());
    }
}
