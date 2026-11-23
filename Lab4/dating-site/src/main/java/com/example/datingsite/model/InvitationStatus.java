package com.example.datingsite.model;

/**
 * Стан запрошення.
 */
public enum InvitationStatus {

    PENDING("Очікує підтвердження"),
    ACCEPTED("Прийнято"),
    DECLINED("Відхилено");

    private final String displayName;

    InvitationStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
