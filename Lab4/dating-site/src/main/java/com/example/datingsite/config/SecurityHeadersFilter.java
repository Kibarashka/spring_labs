package com.example.datingsite.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Додає до кожної відповіді HTTP-заголовки, що є другою лінією захисту від XSS.
 * <p>
 * Перша лінія - екранування: Thymeleaf у th:text / th:value / th:attr перетворює
 * символи &lt; &gt; &amp; " ' на HTML-сутності, тому введений користувачем код
 * відображається як текст і не виконується.
 * <p>
 * Content-Security-Policy забороняє браузеру виконувати будь-які скрипти, окрім
 * завантажених з власного домену (вбудовані &lt;script&gt; та обробники onclick=... блокуються),
 * тож навіть якщо шкідливий код якось потрапить на сторінку, він не запуститься.
 */
@Component
public class SecurityHeadersFilter extends OncePerRequestFilter {

    private static final String CONTENT_SECURITY_POLICY =
            "default-src 'self'; "
            + "script-src 'self'; "
            + "style-src 'self'; "
            + "img-src 'self' data:; "
            + "object-src 'none'; "
            + "base-uri 'self'; "
            + "form-action 'self'; "
            + "frame-ancestors 'none'";

    /**
     * Swagger UI формує сторінку динамічно і несумісний із суворою CSP,
     * тому для документації API заголовок не додається.
     */
    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return path.startsWith("/swagger-ui") || path.startsWith("/v3/api-docs");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        response.setHeader("Content-Security-Policy", CONTENT_SECURITY_POLICY);
        response.setHeader("X-Content-Type-Options", "nosniff");
        response.setHeader("X-Frame-Options", "DENY");
        response.setHeader("Referrer-Policy", "same-origin");
        filterChain.doFilter(request, response);
    }
}
