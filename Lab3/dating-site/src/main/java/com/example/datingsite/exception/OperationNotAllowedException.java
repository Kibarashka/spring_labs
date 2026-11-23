package com.example.datingsite.exception;

/**
 * Користувач не має права виконувати операцію
 * (наприклад, редагувати чужу анкету або приймати чуже запрошення).
 */
public class OperationNotAllowedException extends RuntimeException {

    public OperationNotAllowedException(String message) {
        super(message);
    }
}
