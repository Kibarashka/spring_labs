package com.example.datingsite.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.info.Info;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Налаштування документації OpenAPI (springdoc-openapi).
 * Специфікація: /v3/api-docs (JSON), /v3/api-docs.yaml (YAML); інтерфейс Swagger UI: /swagger-ui.html
 */
@Configuration
public class OpenApiConfig {

    private static final String PROBLEM_SCHEMA = "ProblemDetail";
    private static final String PROBLEM_MEDIA_TYPE = "application/problem+json";

    @Bean
    public OpenAPI datingSiteOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Сайт знайомств - REST API")
                        .version("1.0.0")
                        .description("""
                                RESTful вебсервіс для варіанта №10 «Сайт знайомств» (лабораторна робота №4).

                                **Ресурси:** анкети (`/api/profiles`) та запрошення (`/api/invitations`).

                                **Ідентифікація користувача.** Операції, що виконуються від імені користувача
                                (зміна чи видалення власної анкети, перегляд закритої інформації, відповідь на
                                запрошення), вимагають заголовок `X-User-Id`. Це імітація автентифікації:
                                функції безпеки в роботі не реалізуються.

                                **Часткове оновлення:** JSON Merge Patch (RFC 7386, `application/merge-patch+json`)
                                та JSON Patch (RFC 6902, `application/json-patch+json`).

                                **Помилки** повертаються у форматі Problem Details (RFC 9457,
                                `application/problem+json`)."""))
                .components(new Components().addSchemas(PROBLEM_SCHEMA, problemDetailSchema()));
    }

    /**
     * Для всіх відповідей з кодами 4xx/5xx, у яких не описано тіло, додає схему ProblemDetail -
     * так не потрібно повторювати однаковий опис помилки в кожній анотації @ApiResponse.
     */
    @Bean
    public OpenApiCustomizer problemDetailResponsesCustomizer() {
        return openApi -> {
            openApi.getComponents().addSchemas(PROBLEM_SCHEMA, problemDetailSchema());

            // value в операції JSON Patch може бути будь-яким JSON-значенням (рядок, число, масив...),
            // тому прибираємо згенерований type: object - схема без type означає «будь-який тип»
            Schema<?> patchOperation = openApi.getComponents().getSchemas().get("JsonPatchOperation");
            if (patchOperation != null && patchOperation.getProperties() != null
                    && patchOperation.getProperties().get("value") != null) {
                patchOperation.getProperties().get("value").setType(null);
            }

            openApi.getPaths().values().forEach(pathItem -> pathItem.readOperations().forEach(operation ->
                    operation.getResponses().forEach((code, response) -> {
                        boolean isError = code.startsWith("4") || code.startsWith("5");
                        if (isError) {
                            response.setContent(new Content().addMediaType(PROBLEM_MEDIA_TYPE,
                                    new MediaType().schema(new Schema<>().$ref("#/components/schemas/" + PROBLEM_SCHEMA))));
                        }
                    })));
        };
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static Schema<?> problemDetailSchema() {
        Schema fieldError = new ObjectSchema()
                .addProperty("field", new StringSchema().description("Поле або параметр").example("age"))
                .addProperty("message", new StringSchema().description("Опис помилки")
                        .example("Сайт доступний лише повнолітнім (від 18 років)"));

        return new ObjectSchema()
                .description("Опис помилки у форматі Problem Details (RFC 9457)")
                .addProperty("type", new StringSchema().description("URI типу помилки").example("about:blank"))
                .addProperty("title", new StringSchema().description("Короткий заголовок").example("Ресурс не знайдено"))
                .addProperty("status", new IntegerSchema().description("Код стану HTTP").example(404))
                .addProperty("detail", new StringSchema().description("Детальний опис").example("Анкету з id=999 не знайдено"))
                .addProperty("instance", new StringSchema().description("Шлях запиту").example("/api/profiles/999"))
                .addProperty("timestamp", new StringSchema().description("Час виникнення помилки")
                        .example("2026-11-21T12:00:00.000+02:00"))
                .addProperty("errors", new ArraySchema().items(fieldError)
                        .description("Помилки валідації окремих полів (лише для помилок валідації)"))
                .required(List.of("title", "status"));
    }
}
