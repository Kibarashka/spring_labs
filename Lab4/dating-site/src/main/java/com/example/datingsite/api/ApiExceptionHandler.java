package com.example.datingsite.api;

import com.example.datingsite.api.dto.ApiFieldError;
import com.example.datingsite.api.patch.PatchException;
import com.example.datingsite.exception.BusinessRuleException;
import com.example.datingsite.exception.NotFoundException;
import com.example.datingsite.exception.OperationNotAllowedException;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.TypeMismatchException;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.ServletRequestBindingException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.net.URI;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * Глобальна обробка винятків REST API.
 * <p>
 * Усі помилки повертаються в єдиному форматі Problem Details (RFC 9457, тип application/problem+json):
 * type, title, status, detail, instance, а також додаткові поля timestamp та errors (для помилок валідації).
 * Базовий клас {@link ResponseEntityExceptionHandler} уже обробляє стандартні винятки Spring MVC
 * (415, 405, 400 для нечитабельного JSON тощо) у цьому ж форматі.
 */
@RestControllerAdvice(basePackageClasses = ApiExceptionHandler.class)
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    /** Українські заголовки для стандартних помилок Spring MVC. */
    private static final Map<HttpStatus, String> STANDARD_TITLES = Map.of(
            HttpStatus.BAD_REQUEST, "Некоректний запит",
            HttpStatus.NOT_FOUND, "Ресурс не знайдено",
            HttpStatus.METHOD_NOT_ALLOWED, "Метод не підтримується",
            HttpStatus.NOT_ACCEPTABLE, "Неприйнятний формат відповіді",
            HttpStatus.UNSUPPORTED_MEDIA_TYPE, "Непідтримуваний тип вмісту",
            HttpStatus.INTERNAL_SERVER_ERROR, "Внутрішня помилка сервера");

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ProblemDetail> handleNotFound(NotFoundException e, HttpServletRequest request) {
        return problem(HttpStatus.NOT_FOUND, "Ресурс не знайдено", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ResponseEntity<ProblemDetail> handleForbidden(OperationNotAllowedException e, HttpServletRequest request) {
        return problem(HttpStatus.FORBIDDEN, "Операцію заборонено", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ResponseEntity<ProblemDetail> handleConflict(BusinessRuleException e, HttpServletRequest request) {
        return problem(HttpStatus.CONFLICT, "Конфлікт із поточним станом ресурсу", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> handleBadRequest(IllegalArgumentException e, HttpServletRequest request) {
        return problem(HttpStatus.BAD_REQUEST, "Некоректний запит", e.getMessage(), request, List.of());
    }

    @ExceptionHandler(PatchException.class)
    public ResponseEntity<ProblemDetail> handlePatch(PatchException e, HttpServletRequest request) {
        return problem(e.getStatus(), e.getTitle(), e.getMessage(), request, e.getErrors());
    }

    /** Помилки валідації тіла запиту (@Valid @RequestBody). */
    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<ApiFieldError> errors = new ArrayList<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.add(new ApiFieldError(fieldError.getField(), fieldError.getDefaultMessage()));
        }
        ex.getBindingResult().getGlobalErrors().forEach(
                error -> errors.add(new ApiFieldError(error.getObjectName(), error.getDefaultMessage())));
        errors.sort(Comparator.comparing(ApiFieldError::field));

        ProblemDetail body = ex.getBody();
        body.setTitle("Помилка валідації");
        body.setDetail("Дані запиту не пройшли перевірку");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Помилки валідації параметрів методу (@RequestParam з @Min, @Max, @Pattern тощо). */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<ApiFieldError> errors = new ArrayList<>();
        ex.getParameterValidationResults().forEach(result -> {
            String parameter = result.getMethodParameter().getParameterName();
            for (MessageSourceResolvable error : result.getResolvableErrors()) {
                errors.add(new ApiFieldError(parameter, error.getDefaultMessage()));
            }
        });

        ProblemDetail body = ex.getBody();
        body.setTitle("Помилка валідації");
        body.setDetail("Параметри запиту не пройшли перевірку");
        body.setProperty("errors", errors);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status,
                "Тіло запиту відсутнє або містить некоректний JSON");
        body.setTitle("Некоректне тіло запиту");
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Некоректне значення параметра шляху / запиту / заголовка (наприклад, /api/profiles/abc). */
    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex, HttpHeaders headers,
                                                        HttpStatusCode status, WebRequest request) {
        String name = ex instanceof MethodArgumentTypeMismatchException mismatch ? mismatch.getName() : ex.getPropertyName();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status,
                "Некоректне значення параметра «" + name + "»: " + ex.getValue());
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Відсутній обов'язковий заголовок (X-User-Id) або параметр запиту. */
    @Override
    protected ResponseEntity<Object> handleServletRequestBindingException(ServletRequestBindingException ex,
                                                                          HttpHeaders headers,
                                                                          HttpStatusCode status,
                                                                          WebRequest request) {
        String detail = ex instanceof MissingRequestHeaderException missing
                ? "Відсутній обов’язковий заголовок " + missing.getHeaderName()
                : ex.getBody().getDetail();
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        return handleExceptionInternal(ex, body, headers, status, request);
    }

    /** Додає до всіх стандартних відповідей-помилок поля instance і timestamp. */
    @Override
    protected ResponseEntity<Object> createResponseEntity(Object body, HttpHeaders headers,
                                                          HttpStatusCode statusCode, WebRequest request) {
        if (body instanceof ProblemDetail problem) {
            HttpStatus status = HttpStatus.resolve(statusCode.value());
            if (status != null && status.getReasonPhrase().equals(problem.getTitle())) {
                problem.setTitle(STANDARD_TITLES.getOrDefault(status, problem.getTitle()));
            }
            if (problem.getInstance() == null && request instanceof ServletWebRequest servletRequest) {
                problem.setInstance(URI.create(servletRequest.getRequest().getRequestURI()));
            }
            problem.setProperty("timestamp", OffsetDateTime.now().toString());
        }
        return super.createResponseEntity(body, headers, statusCode, request);
    }

    private ResponseEntity<ProblemDetail> problem(HttpStatusCode status, String title, String detail,
                                                  HttpServletRequest request, List<ApiFieldError> errors) {
        ProblemDetail body = ProblemDetail.forStatusAndDetail(status, detail);
        body.setTitle(title);
        body.setInstance(URI.create(request.getRequestURI()));
        body.setProperty("timestamp", OffsetDateTime.now().toString());
        if (!errors.isEmpty()) {
            body.setProperty("errors", errors);
        }
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
