package com.example.datingsite.service;

import com.example.datingsite.model.InvitationStatus;

/**
 * Критерії фільтрації запрошень. Будь-який критерій може бути null - тоді він не застосовується.
 */
public record InvitationFilter(Long senderId, Long receiverId, InvitationStatus status) {
}
