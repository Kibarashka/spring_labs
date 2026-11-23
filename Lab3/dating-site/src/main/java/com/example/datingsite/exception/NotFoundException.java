package com.example.datingsite.exception;

/**
 * Запитаний об'єкт (анкету, запрошення) не знайдено.
 */
public class NotFoundException extends RuntimeException {

    public NotFoundException(String message) {
        super(message);
    }
}
