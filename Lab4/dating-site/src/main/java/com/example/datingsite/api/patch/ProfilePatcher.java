package com.example.datingsite.api.patch;

import com.example.datingsite.api.dto.ApiFieldError;
import com.example.datingsite.api.dto.ProfileRequest;
import jakarta.json.Json;
import jakarta.json.JsonException;
import jakarta.json.JsonReader;
import jakarta.json.JsonStructure;
import jakarta.json.JsonValue;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.exc.UnrecognizedPropertyException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Застосовує до анкети документи часткового оновлення:
 * JSON Merge Patch (RFC 7386) та JSON Patch (RFC 6902).
 * <p>
 * Алгоритм:
 * <ol>
 *     <li>поточний стан анкети перетворюється на JSON-документ (формат {@link ProfileRequest});</li>
 *     <li>до документа застосовується патч засобами Jakarta JSON Processing (реалізація Eclipse Parsson);</li>
 *     <li>результат перетворюється назад на {@link ProfileRequest} і проходить ту саму валідацію,
 *         що й тіло запитів POST/PUT.</li>
 * </ol>
 */
@Component
public class ProfilePatcher {

    private static final TypeReference<List<JsonPatchOperation>> OPERATIONS_TYPE = new TypeReference<>() {
    };

    private final JsonMapper jsonMapper;
    private final Validator validator;

    public ProfilePatcher(JsonMapper jsonMapper, Validator validator) {
        this.jsonMapper = jsonMapper;
        this.validator = validator;
    }

    /** RFC 7386: документ - JSON-об'єкт; присутні поля замінюються, null - видаляє поле. */
    public ProfileRequest applyMergePatch(ProfileRequest current, JsonNode patch) {
        if (patch == null || !patch.isObject()) {
            throw PatchException.invalidDocument(
                    "Документ JSON Merge Patch має бути JSON-об’єктом", List.of());
        }

        JsonValue target = toJsonP(current);
        JsonValue result = Json.createMergePatch(toJsonP(patch)).apply(target);
        return toValidatedRequest(result);
    }

    /** RFC 6902: документ - масив операцій add/remove/replace/move/copy/test, що виконуються послідовно. */
    public ProfileRequest applyJsonPatch(ProfileRequest current, JsonNode patch) {
        if (patch == null || !patch.isArray()) {
            throw PatchException.invalidDocument(
                    "Документ JSON Patch має бути JSON-масивом операцій", List.of());
        }

        validateOperations(patch);

        JsonStructure target = (JsonStructure) toJsonP(current);
        JsonValue result;
        try {
            result = Json.createPatch(toJsonP(patch).asJsonArray()).apply(target);
        } catch (JsonException e) {
            throw PatchException.conflict("Не вдалося застосувати JSON Patch: " + e.getMessage());
        }
        return toValidatedRequest(result);
    }

    private void validateOperations(JsonNode patch) {
        List<JsonPatchOperation> operations;
        try {
            operations = jsonMapper.convertValue(patch, OPERATIONS_TYPE);
        } catch (JacksonException | IllegalArgumentException e) {
            throw PatchException.invalidDocument(
                    "Некоректна операція JSON Patch: допустимі op - add, remove, replace, move, copy, test",
                    List.of());
        }

        List<ApiFieldError> errors = new ArrayList<>();
        for (int i = 0; i < operations.size(); i++) {
            JsonPatchOperation operation = operations.get(i);
            if (operation == null) {
                errors.add(new ApiFieldError("[" + i + "]", "Операція не може бути null"));
                continue;
            }
            String prefix = "[" + i + "].";
            for (ConstraintViolation<JsonPatchOperation> v : validator.validate(operation)) {
                errors.add(new ApiFieldError(prefix + fieldName(v), v.getMessage()));
            }
        }
        if (!errors.isEmpty()) {
            errors.sort(Comparator.comparing(ApiFieldError::field));
            throw PatchException.invalidDocument("Документ JSON Patch містить некоректні операції", errors);
        }
    }

    private ProfileRequest toValidatedRequest(JsonValue result) {
        ProfileRequest request;
        try {
            request = jsonMapper.readerFor(ProfileRequest.class)
                    .with(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
                    .readValue(result.toString());
        } catch (UnrecognizedPropertyException e) {
            throw PatchException.unprocessable(
                    "Після застосування патчу анкета має некоректну структуру",
                    List.of(new ApiFieldError(e.getPropertyName(), "Невідоме поле - його не можна додати до анкети")));
        } catch (JacksonException e) {
            String field = e.getPath().isEmpty() ? "" : e.getPath().getLast().getPropertyName();
            throw PatchException.unprocessable(
                    "Після застосування патчу анкета має некоректну структуру",
                    List.of(new ApiFieldError(field, "Значення має некоректний тип")));
        }

        Set<ConstraintViolation<ProfileRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            List<ApiFieldError> errors = violations.stream()
                    .map(v -> new ApiFieldError(fieldName(v), v.getMessage()))
                    .sorted(Comparator.comparing(ApiFieldError::field))
                    .toList();
            throw PatchException.unprocessable(
                    "Після застосування патчу анкета не проходить валідацію", errors);
        }
        return request;
    }

    private JsonValue toJsonP(Object value) {
        String json = jsonMapper.writeValueAsString(value);
        try (JsonReader reader = Json.createReader(new StringReader(json))) {
            return reader.readValue();
        }
    }

    private static String fieldName(ConstraintViolation<?> violation) {
        String path = violation.getPropertyPath().toString();
        // isValuePresentWhenRequired -> valuePresentWhenRequired: залишаємо лише назву поля
        return switch (path) {
            case "valuePresentWhenRequired" -> "value";
            case "fromPresentWhenRequired" -> "from";
            default -> path;
        };
    }
}
