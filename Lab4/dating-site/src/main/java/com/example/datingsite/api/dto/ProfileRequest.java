package com.example.datingsite.api.dto;

import com.example.datingsite.model.Profile;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

/**
 * Дані для створення (POST) або повної заміни (PUT) анкети.
 * Цей самий формат є «документом», до якого застосовуються JSON Patch / JSON Merge Patch.
 */
@Schema(description = "Дані для створення або повної заміни анкети")
public record ProfileRequest(

        @Schema(description = "Ім’я користувача", example = "Taras")
        @NotBlank(message = "Вкажіть ім’я")
        @Size(max = 50, message = "Ім’я має містити не більше 50 символів")
        String name,

        @Schema(description = "Вік (від 18 до 100)", example = "23")
        @NotNull(message = "Вкажіть вік")
        @Min(value = 18, message = "Сайт доступний лише повнолітнім (від 18 років)")
        @Max(value = 100, message = "Вкажіть реальний вік (не більше 100)")
        Integer age,

        @Schema(description = "Відкрита інформація «про себе»", example = "Граю на гітарі", nullable = true)
        @Size(max = 500, message = "Відкрита інформація - не більше 500 символів")
        String publicInfo,

        @Schema(description = "Закрита інформація (контакти)", example = "telegram: @taras_example", nullable = true)
        @Size(max = 200, message = "Закрита інформація - не більше 200 символів")
        String privateInfo,

        @ArraySchema(arraySchema = @Schema(description = "Ключові слова (до 10 шт.)",
                example = "[\"музика\", \"гітара\"]"),
                schema = @Schema(maxLength = 30))
        @Size(max = 10, message = "Не більше 10 ключових слів")
        List<@NotBlank(message = "Ключове слово не може бути порожнім")
             @Size(max = 30, message = "Ключове слово - не більше 30 символів") String> keywords
) {

    public static ProfileRequest from(Profile profile) {
        return new ProfileRequest(profile.getName(), profile.getAge(), profile.getPublicInfo(),
                profile.getPrivateInfo(), List.copyOf(profile.getKeywords()));
    }

    public Profile toProfile(Long id) {
        return new Profile(id, name, age, publicInfo, privateInfo, keywords);
    }
}
