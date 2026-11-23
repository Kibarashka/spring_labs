package com.example.datingsite.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.webmvc.error.DefaultErrorAttributes;
import org.springframework.stereotype.Component;
import org.springframework.web.ErrorResponse;
import org.springframework.web.context.request.WebRequest;

import java.util.Map;

/**
 * Атрибути стандартної сторінки помилок Spring Boot (/error).
 * <p>
 * Деякі помилки виникають ще до вибору контролера - невідомий шлях (404), непідтримуваний метод (405)
 * чи Content-Type (415) - тому їх не обробляє {@code ApiExceptionHandler}. Щоб клієнти REST API завжди
 * отримували однакову структуру помилки, до стандартних атрибутів додаються поля формату
 * Problem Details (RFC 9457): type, title, detail, instance.
 * Для браузера ці ж атрибути використовуються в шаблоні error.html.
 */
@Component
public class ProblemDetailErrorAttributes extends DefaultErrorAttributes {

    private static final Map<Object, String> TITLES = Map.of(
            400, "Некоректний запит",
            404, "Ресурс не знайдено",
            405, "Метод не підтримується",
            406, "Неприйнятний формат відповіді",
            415, "Непідтримуваний тип вмісту",
            500, "Внутрішня помилка сервера");

    @Override
    public Map<String, Object> getErrorAttributes(WebRequest webRequest, ErrorAttributeOptions options) {
        Map<String, Object> attributes = super.getErrorAttributes(webRequest, options);

        attributes.put("type", "about:blank");
        attributes.put("title", TITLES.getOrDefault(attributes.get("status"), String.valueOf(attributes.get("error"))));
        attributes.put("instance", attributes.get("path"));

        Throwable error = getError(webRequest);
        if (error instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null) {
            attributes.put("detail", errorResponse.getBody().getDetail());
        } else if (Integer.valueOf(404).equals(attributes.get("status"))) {
            attributes.put("detail", "Ресурс за вказаною адресою не знайдено");
        }
        return attributes;
    }
}
