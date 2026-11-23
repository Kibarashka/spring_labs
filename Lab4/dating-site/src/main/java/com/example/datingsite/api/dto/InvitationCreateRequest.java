package com.example.datingsite.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Schema(description = "Дані для надсилання запрошення")
public record InvitationCreateRequest(

        @Schema(description = "Id анкети відправника", example = "1")
        @NotNull(message = "Вкажіть відправника (senderId)")
        @Positive(message = "senderId має бути додатним числом")
        Long senderId,

        @Schema(description = "Id анкети отримувача", example = "2")
        @NotNull(message = "Вкажіть отримувача (receiverId)")
        @Positive(message = "receiverId має бути додатним числом")
        Long receiverId
) {

    /** Перевірка на рівні всього об'єкта: не можна запросити самого себе. */
    @JsonIgnore
    @Schema(hidden = true)
    @AssertTrue(message = "Не можна надіслати запрошення самому собі")
    public boolean isDifferentParticipants() {
        return senderId == null || receiverId == null || !senderId.equals(receiverId);
    }
}
