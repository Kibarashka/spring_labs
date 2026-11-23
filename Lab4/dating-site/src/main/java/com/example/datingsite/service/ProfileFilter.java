package com.example.datingsite.service;

/**
 * Критерії фільтрації анкет. Будь-який критерій може бути null - тоді він не застосовується.
 *
 * @param keyword частина ключового слова (без урахування регістру)
 * @param name    частина імені (без урахування регістру)
 * @param minAge  мінімальний вік (включно)
 * @param maxAge  максимальний вік (включно)
 */
public record ProfileFilter(String keyword, String name, Integer minAge, Integer maxAge) {
}
