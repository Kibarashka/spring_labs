package com.example.datingsite.api;

import com.example.datingsite.api.dto.PageResponse;
import com.example.datingsite.api.dto.PrivateInfoResponse;
import com.example.datingsite.api.dto.ProfileMergePatch;
import com.example.datingsite.api.dto.ProfileRequest;
import com.example.datingsite.api.dto.ProfileResponse;
import com.example.datingsite.api.patch.JsonPatchOperation;
import com.example.datingsite.api.patch.ProfilePatcher;
import com.example.datingsite.exception.OperationNotAllowedException;
import com.example.datingsite.model.Profile;
import com.example.datingsite.service.InvitationService;
import com.example.datingsite.service.PagedResult;
import com.example.datingsite.service.ProfileFilter;
import com.example.datingsite.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import tools.jackson.databind.JsonNode;

import java.net.URI;

/**
 * REST-ресурс «Анкети»: повний набір CRUD-операцій, фільтрація, пагінація та часткове оновлення.
 */
@RestController
@RequestMapping(path = "/api/profiles", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Анкети", description = "CRUD-операції з анкетами, фільтрація, пагінація та часткове оновлення")
public class ProfileRestController {

    static final String MERGE_PATCH_JSON = "application/merge-patch+json";
    static final String JSON_PATCH_JSON = "application/json-patch+json";
    static final String USER_HEADER = "X-User-Id";
    static final String USER_HEADER_DESCRIPTION =
            "Id користувача, від імені якого виконується операція (імітація автентифікації; "
            + "повноцінна безпека в роботі не реалізується)";

    private final ProfileService profileService;
    private final InvitationService invitationService;
    private final ProfilePatcher profilePatcher;

    public ProfileRestController(ProfileService profileService,
                                 InvitationService invitationService,
                                 ProfilePatcher profilePatcher) {
        this.profileService = profileService;
        this.invitationService = invitationService;
        this.profilePatcher = profilePatcher;
    }

    @GetMapping
    @Operation(
            summary = "Отримати список анкет (фільтрація та пагінація)",
            description = """
                    Повертає сторінку анкет, що відповідають усім заданим фільтрам.
                    Фільтри: keyword - частина ключового слова, name - частина імені (обидва без урахування
                    регістру), minAge / maxAge - діапазон віку включно. Пагінація: page - номер сторінки
                    з 0, size - розмір сторінки (1-100). Сортування: sort=поле[,asc|desc], де поле - id, name або age.
                    Закрита інформація у списку не повертається.""")
    @ApiResponse(responseCode = "200", description = "Сторінку анкет отримано")
    @ApiResponse(responseCode = "400", description = "Некоректні параметри фільтрації, пагінації або сортування")
    public PageResponse<ProfileResponse> getProfiles(
            @Parameter(description = "Частина ключового слова", example = "спорт")
            @RequestParam(required = false) @Size(max = 50) String keyword,
            @Parameter(description = "Частина імені", example = "an")
            @RequestParam(required = false) @Size(max = 50) String name,
            @Parameter(description = "Мінімальний вік (включно)", example = "20")
            @RequestParam(required = false) @Min(18) @Max(100) Integer minAge,
            @Parameter(description = "Максимальний вік (включно)", example = "30")
            @RequestParam(required = false) @Min(18) @Max(100) Integer maxAge,
            @Parameter(description = "Номер сторінки (з 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Розмір сторінки (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size,
            @Parameter(description = "Сортування: id, name або age з напрямом asc/desc", example = "age,desc")
            @RequestParam(defaultValue = "id,asc")
            @Pattern(regexp = "^(id|name|age)(,(asc|desc))?$",
                    message = "Формат: поле[,asc|desc], де поле - id, name або age") String sort) {

        String[] sortParts = sort.split(",");
        boolean ascending = sortParts.length < 2 || sortParts[1].equals("asc");

        PagedResult<Profile> result = profileService.findProfiles(
                new ProfileFilter(keyword, name, minAge, maxAge), page, size, sortParts[0], ascending);
        return PageResponse.from(result, ProfileResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(
            summary = "Отримати анкету за id",
            description = "Повертає публічні дані однієї анкети: ім’я, вік, відкриту інформацію та ключові слова.")
    @ApiResponse(responseCode = "200", description = "Анкету знайдено")
    @ApiResponse(responseCode = "400", description = "Некоректний id")
    @ApiResponse(responseCode = "404", description = "Анкету не знайдено")
    public ProfileResponse getProfile(
            @Parameter(description = "Id анкети", example = "1") @PathVariable Long id) {
        return ProfileResponse.from(profileService.getRequiredProfile(id));
    }

    @GetMapping("/{id}/private-info")
    @Operation(
            summary = "Отримати закриту інформацію анкети",
            description = """
                    Повертає закриту інформацію (контакти) анкети, якщо користувач X-User-Id має до неї доступ:
                    це його власна анкета; або власник анкети надіслав йому запрошення;
                    або користувач сам надіслав запрошення і його прийняли.""")
    @ApiResponse(responseCode = "200", description = "Доступ дозволено, закриту інформацію повернуто")
    @ApiResponse(responseCode = "400", description = "Відсутній або некоректний заголовок X-User-Id")
    @ApiResponse(responseCode = "403", description = "Користувач не має доступу до закритої інформації")
    @ApiResponse(responseCode = "404", description = "Анкету не знайдено")
    public PrivateInfoResponse getPrivateInfo(
            @Parameter(description = "Id анкети", example = "2") @PathVariable Long id,
            @Parameter(description = USER_HEADER_DESCRIPTION, example = "1", in = ParameterIn.HEADER)
            @RequestHeader(USER_HEADER) Long currentUserId) {
        Profile profile = profileService.getRequiredProfile(id);
        if (!invitationService.canViewPrivateInfo(currentUserId, id)) {
            throw new OperationNotAllowedException(
                    "Закрита інформація доступна лише після отримання або підтвердження запрошення");
        }
        return new PrivateInfoResponse(profile.getId(), profile.getPrivateInfo());
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Створити анкету",
            description = """
                    Створює нову анкету. Ім’я та вік (18-100) обов’язкові. Ключові слова нормалізуються:
                    обрізаються пробіли, переводяться в нижній регістр, повтори видаляються.
                    У відповіді повертається створена анкета, а в заголовку Location - її адреса.""")
    @ApiResponse(responseCode = "201", description = "Анкету створено")
    @ApiResponse(responseCode = "400", description = "Тіло запиту некоректне або не пройшло валідацію")
    @ApiResponse(responseCode = "415", description = "Непідтримуваний Content-Type (очікується application/json)")
    public ResponseEntity<ProfileResponse> createProfile(@Valid @RequestBody ProfileRequest request) {
        Profile created = profileService.createProfile(request.name(), request.age(),
                request.publicInfo(), request.privateInfo(), request.keywords());

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(ProfileResponse.from(created));
    }

    @PutMapping(path = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Повністю оновити анкету",
            description = """
                    Замінює всі дані анкети значеннями з тіла запиту (поля, яких немає в тілі, стають порожніми).
                    Змінювати можна лише власну анкету: X-User-Id має збігатися з id.""")
    @ApiResponse(responseCode = "200", description = "Анкету оновлено")
    @ApiResponse(responseCode = "400", description = "Тіло запиту некоректне, не пройшло валідацію або відсутній X-User-Id")
    @ApiResponse(responseCode = "403", description = "Спроба змінити чужу анкету")
    @ApiResponse(responseCode = "404", description = "Анкету не знайдено")
    @ApiResponse(responseCode = "415", description = "Непідтримуваний Content-Type (очікується application/json)")
    public ProfileResponse replaceProfile(
            @Parameter(description = "Id анкети", example = "1") @PathVariable Long id,
            @Parameter(description = USER_HEADER_DESCRIPTION, example = "1", in = ParameterIn.HEADER)
            @RequestHeader(USER_HEADER) Long currentUserId,
            @Valid @RequestBody ProfileRequest request) {
        Profile updated = profileService.updateProfile(currentUserId, request.toProfile(id));
        return ProfileResponse.from(updated);
    }

    @PatchMapping(path = "/{id}", consumes = {MERGE_PATCH_JSON, JSON_PATCH_JSON})
    @Operation(
            summary = "Частково оновити анкету (JSON Merge Patch або JSON Patch)",
            description = """
                    Підтримуються два формати часткового оновлення, формат визначається заголовком Content-Type:

                    * application/merge-patch+json - JSON Merge Patch (RFC 7386): об’єкт лише з полями,
                      які потрібно змінити; значення null видаляє поле;
                    * application/json-patch+json - JSON Patch (RFC 6902): масив операцій
                      add, remove, replace, move, copy, test, які виконуються послідовно та атомарно.

                    Документ, до якого застосовується патч, має структуру ProfileRequest (поле id відсутнє і не
                    може бути змінене). Після застосування результат проходить ту саму валідацію, що й при PUT.
                    Змінювати можна лише власну анкету (X-User-Id = id).""",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Документ часткового оновлення",
                    content = {
                            @Content(mediaType = MERGE_PATCH_JSON,
                                    schema = @Schema(implementation = ProfileMergePatch.class),
                                    examples = @ExampleObject(name = "merge-patch",
                                            summary = "Змінити опис і видалити закриту інформацію",
                                            value = "{\"publicInfo\": \"Люблю гори та каву\", \"privateInfo\": null}")),
                            @Content(mediaType = JSON_PATCH_JSON,
                                    array = @ArraySchema(schema = @Schema(implementation = JsonPatchOperation.class)),
                                    examples = @ExampleObject(name = "json-patch",
                                            summary = "Перевірити вік, змінити його та додати ключове слово",
                                            value = """
                                                    [
                                                      {"op": "test", "path": "/age", "value": 22},
                                                      {"op": "replace", "path": "/age", "value": 23},
                                                      {"op": "add", "path": "/keywords/-", "value": "кава"}
                                                    ]"""))
                    }))
    @ApiResponse(responseCode = "200", description = "Анкету частково оновлено")
    @ApiResponse(responseCode = "400", description = "Некоректний документ патчу або відсутній X-User-Id")
    @ApiResponse(responseCode = "403", description = "Спроба змінити чужу анкету")
    @ApiResponse(responseCode = "404", description = "Анкету не знайдено")
    @ApiResponse(responseCode = "409", description = "Патч неможливо застосувати: немає поля за шляхом path або не пройшла операція test")
    @ApiResponse(responseCode = "415", description = "Непідтримуваний Content-Type")
    @ApiResponse(responseCode = "422", description = "Після застосування патчу анкета не проходить валідацію")
    public ProfileResponse patchProfile(
            @Parameter(description = "Id анкети", example = "1") @PathVariable Long id,
            @Parameter(description = USER_HEADER_DESCRIPTION, example = "1", in = ParameterIn.HEADER)
            @RequestHeader(USER_HEADER) Long currentUserId,
            @RequestBody JsonNode patch,
            HttpServletRequest httpRequest) {

        Profile current = profileService.requireOwnProfile(currentUserId, id);
        ProfileRequest currentState = ProfileRequest.from(current);

        MediaType contentType = MediaType.parseMediaType(httpRequest.getContentType());
        ProfileRequest patched = contentType.isCompatibleWith(MediaType.parseMediaType(JSON_PATCH_JSON))
                ? profilePatcher.applyJsonPatch(currentState, patch)
                : profilePatcher.applyMergePatch(currentState, patch);

        return ProfileResponse.from(profileService.updateProfile(currentUserId, patched.toProfile(id)));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(
            summary = "Видалити анкету",
            description = "Видаляє власну анкету (X-User-Id = id) разом з усіма запрошеннями, у яких вона бере участь.")
    @ApiResponse(responseCode = "204", description = "Анкету видалено")
    @ApiResponse(responseCode = "400", description = "Відсутній або некоректний X-User-Id")
    @ApiResponse(responseCode = "403", description = "Спроба видалити чужу анкету")
    @ApiResponse(responseCode = "404", description = "Анкету не знайдено")
    public void deleteProfile(
            @Parameter(description = "Id анкети", example = "6") @PathVariable Long id,
            @Parameter(description = USER_HEADER_DESCRIPTION, example = "6", in = ParameterIn.HEADER)
            @RequestHeader(USER_HEADER) Long currentUserId) {
        profileService.deleteProfile(currentUserId, id);
    }
}
