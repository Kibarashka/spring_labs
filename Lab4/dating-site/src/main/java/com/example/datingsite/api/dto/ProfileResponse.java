package com.example.datingsite.api.dto;

import com.example.datingsite.model.Profile;
import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;

import static io.swagger.v3.oas.annotations.media.Schema.RequiredMode.REQUIRED;

@Schema(description = "Публічні дані анкети (без закритої інформації)")
public record ProfileResponse(

        @Schema(description = "Ідентифікатор анкети", example = "1", requiredMode = REQUIRED)
        Long id,

        @Schema(description = "Ім’я користувача", example = "Anna", requiredMode = REQUIRED)
        String name,

        @Schema(description = "Вік", example = "22", requiredMode = REQUIRED)
        int age,

        @Schema(description = "Відкрита інформація «про себе»; null, якщо не заповнена",
                example = "Люблю подорожі та музику", nullable = true)
        String publicInfo,

        @Schema(description = "Ключові слова (інтереси)", example = "[\"музика\", \"подорожі\"]",
                requiredMode = REQUIRED)
        List<String> keywords
) {

    public static ProfileResponse from(Profile profile) {
        return new ProfileResponse(profile.getId(), profile.getName(), profile.getAge(),
                profile.getPublicInfo(), List.copyOf(profile.getKeywords()));
    }
}
