package com.example.datingsite.api;

import com.example.datingsite.api.dto.InvitationCreateRequest;
import com.example.datingsite.api.dto.InvitationResponse;
import com.example.datingsite.api.dto.InvitationStatusUpdateRequest;
import com.example.datingsite.api.dto.PageResponse;
import com.example.datingsite.model.Invitation;
import com.example.datingsite.model.InvitationStatus;
import com.example.datingsite.service.InvitationFilter;
import com.example.datingsite.service.InvitationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.enums.ParameterIn;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

import static com.example.datingsite.api.ProfileRestController.MERGE_PATCH_JSON;
import static com.example.datingsite.api.ProfileRestController.USER_HEADER;
import static com.example.datingsite.api.ProfileRestController.USER_HEADER_DESCRIPTION;

/**
 * REST-ресурс «Запрошення»: CRUD-операції, фільтрація з пагінацією та зміна стану (JSON Merge Patch).
 */
@RestController
@RequestMapping(path = "/api/invitations", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Запрошення", description = "Надсилання, перегляд, прийняття/відхилення та видалення запрошень")
public class InvitationRestController {

    private final InvitationService invitationService;

    public InvitationRestController(InvitationService invitationService) {
        this.invitationService = invitationService;
    }

    @GetMapping
    @Operation(
            summary = "Отримати список запрошень (фільтрація та пагінація)",
            description = """
                    Повертає сторінку запрошень, відсортованих за id. Фільтри (необов’язкові, поєднуються через «І»):
                    senderId - відправник, receiverId - отримувач, status - стан запрошення.""")
    @ApiResponse(responseCode = "200", description = "Сторінку запрошень отримано")
    @ApiResponse(responseCode = "400", description = "Некоректні параметри (наприклад, невідомий status)")
    public PageResponse<InvitationResponse> getInvitations(
            @Parameter(description = "Id відправника", example = "1")
            @RequestParam(required = false) Long senderId,
            @Parameter(description = "Id отримувача", example = "2")
            @RequestParam(required = false) Long receiverId,
            @Parameter(description = "Стан запрошення", example = "PENDING")
            @RequestParam(required = false) InvitationStatus status,
            @Parameter(description = "Номер сторінки (з 0)", example = "0")
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @Parameter(description = "Розмір сторінки (1-100)", example = "10")
            @RequestParam(defaultValue = "10") @Min(1) @Max(100) int size) {
        return PageResponse.from(
                invitationService.findInvitations(new InvitationFilter(senderId, receiverId, status), page, size),
                InvitationResponse::from);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Отримати запрошення за id",
            description = "Повертає одне запрошення: учасників, стан і дату створення.")
    @ApiResponse(responseCode = "200", description = "Запрошення знайдено")
    @ApiResponse(responseCode = "400", description = "Некоректний id")
    @ApiResponse(responseCode = "404", description = "Запрошення не знайдено")
    public InvitationResponse getInvitation(
            @Parameter(description = "Id запрошення", example = "1") @PathVariable Long id) {
        return InvitationResponse.from(invitationService.getRequiredInvitation(id));
    }

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
            summary = "Надіслати запрошення",
            description = """
                    Створює запрошення від senderId до receiverId у стані PENDING. Не можна запросити себе;
                    між двома користувачами може існувати лише одне запрошення (в будь-якому напрямку).
                    Після створення отримувач одразу отримує доступ до закритої інформації відправника.""")
    @ApiResponse(responseCode = "201", description = "Запрошення створено")
    @ApiResponse(responseCode = "400", description = "Тіло запиту некоректне або не пройшло валідацію (зокрема запрошення самому собі)")
    @ApiResponse(responseCode = "404", description = "Відправника або отримувача не знайдено")
    @ApiResponse(responseCode = "409", description = "Запрошення між цими користувачами вже існує")
    @ApiResponse(responseCode = "415", description = "Непідтримуваний Content-Type (очікується application/json)")
    public ResponseEntity<InvitationResponse> createInvitation(@Valid @RequestBody InvitationCreateRequest request) {
        Invitation created = invitationService.sendInvitation(request.senderId(), request.receiverId());
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.getId()).toUri();
        return ResponseEntity.created(location).body(InvitationResponse.from(created));
    }

    @PatchMapping(path = "/{id}", consumes = {MERGE_PATCH_JSON, MediaType.APPLICATION_JSON_VALUE})
    @Operation(
            summary = "Прийняти або відхилити запрошення",
            description = """
                    Часткове оновлення запрошення у форматі JSON Merge Patch (RFC 7386): змінюється лише поле status.
                    Дозволені переходи: PENDING → ACCEPTED, PENDING → DECLINED. Повторне встановлення поточного
                    стану нічого не змінює (операція ідемпотентна). Змінювати стан може лише отримувач (X-User-Id).""")
    @ApiResponse(responseCode = "200", description = "Стан запрошення змінено")
    @ApiResponse(responseCode = "400", description = "Некоректне тіло запиту або відсутній X-User-Id")
    @ApiResponse(responseCode = "403", description = "Змінити стан може лише отримувач")
    @ApiResponse(responseCode = "404", description = "Запрошення не знайдено")
    @ApiResponse(responseCode = "409", description = "Недопустимий перехід стану (наприклад, ACCEPTED → DECLINED)")
    @ApiResponse(responseCode = "415", description = "Непідтримуваний Content-Type")
    public InvitationResponse updateInvitationStatus(
            @Parameter(description = "Id запрошення", example = "1") @PathVariable Long id,
            @Parameter(description = USER_HEADER_DESCRIPTION, example = "2", in = ParameterIn.HEADER)
            @RequestHeader(USER_HEADER) Long currentUserId,
            @Valid @RequestBody InvitationStatusUpdateRequest request) {
        return InvitationResponse.from(invitationService.changeStatus(id, currentUserId, request.status()));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Видалити (відкликати) запрошення",
            description = "Видаляє запрошення. Виконати операцію може лише учасник запрошення - відправник або отримувач.")
    @ApiResponse(responseCode = "204", description = "Запрошення видалено")
    @ApiResponse(responseCode = "400", description = "Відсутній або некоректний X-User-Id")
    @ApiResponse(responseCode = "403", description = "Користувач не є учасником запрошення")
    @ApiResponse(responseCode = "404", description = "Запрошення не знайдено")
    public void deleteInvitation(
            @Parameter(description = "Id запрошення", example = "1") @PathVariable Long id,
            @Parameter(description = USER_HEADER_DESCRIPTION, example = "1", in = ParameterIn.HEADER)
            @RequestHeader(USER_HEADER) Long currentUserId) {
        invitationService.deleteInvitation(id, currentUserId);
    }
}
