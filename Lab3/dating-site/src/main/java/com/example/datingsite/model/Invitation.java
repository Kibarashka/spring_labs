package com.example.datingsite.model;

import java.time.LocalDateTime;

public class Invitation {

    private Long id;
    private Long senderId;
    private Long receiverId;
    private InvitationStatus status;
    private LocalDateTime createdAt;

    public Invitation(Long id, Long senderId, Long receiverId) {
        this.id = id;
        this.senderId = senderId;
        this.receiverId = receiverId;
        this.status = InvitationStatus.PENDING;
        this.createdAt = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getSenderId() {
        return senderId;
    }

    public Long getReceiverId() {
        return receiverId;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean involves(Long profileId) {
        return senderId.equals(profileId) || receiverId.equals(profileId);
    }
}
