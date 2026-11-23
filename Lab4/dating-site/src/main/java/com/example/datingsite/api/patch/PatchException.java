package com.example.datingsite.api.patch;

import com.example.datingsite.api.dto.ApiFieldError;
import org.springframework.http.HttpStatusCode;

import java.util.List;

/**
 * Помилка під час часткового оновлення ресурсу (RFC 5789, розділ 2.2):
 * <ul>
 *     <li>400 Bad Request - документ патчу має некоректну структуру;</li>
 *     <li>409 Conflict - патч неможливо застосувати до поточного стану ресурсу
 *         (немає поля за вказаним шляхом, не пройшла операція test);</li>
 *     <li>422 Unprocessable Content - патч застосовано, але результат не проходить валідацію.</li>
 * </ul>
 */
public class PatchException extends RuntimeException {

    private final HttpStatusCode status;
    private final String title;
    private final List<ApiFieldError> errors;

    private PatchException(int status, String title, String detail, List<ApiFieldError> errors) {
        super(detail);
        this.status = HttpStatusCode.valueOf(status);
        this.title = title;
        this.errors = errors;
    }

    public static PatchException invalidDocument(String detail, List<ApiFieldError> errors) {
        return new PatchException(400, "Некоректний документ патчу", detail, errors);
    }

    public static PatchException conflict(String detail) {
        return new PatchException(409, "Патч неможливо застосувати", detail, List.of());
    }

    public static PatchException unprocessable(String detail, List<ApiFieldError> errors) {
        return new PatchException(422, "Результат патчу некоректний", detail, errors);
    }

    public HttpStatusCode getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public List<ApiFieldError> getErrors() {
        return errors;
    }
}
