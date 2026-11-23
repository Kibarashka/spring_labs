package com.example.datingsite.exception;

/**
 * Операцію не можна виконати, бо вона порушує правила предметної області
 * (наприклад, повторне запрошення або запрошення самому собі).
 */
public class BusinessRuleException extends RuntimeException {

    public BusinessRuleException(String message) {
        super(message);
    }
}
