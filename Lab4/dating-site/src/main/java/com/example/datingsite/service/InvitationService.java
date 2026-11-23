package com.example.datingsite.service;

import com.example.datingsite.exception.BusinessRuleException;
import com.example.datingsite.exception.NotFoundException;
import com.example.datingsite.exception.OperationNotAllowedException;
import com.example.datingsite.model.ConnectionStatus;
import com.example.datingsite.model.Invitation;
import com.example.datingsite.model.InvitationStatus;
import com.example.datingsite.repository.InvitationRepository;
import com.example.datingsite.repository.ProfileRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class InvitationService {

    private InvitationRepository invitationRepository;
    private ProfileRepository profileRepository;

    // Ін'єкція залежностей через сетери
    @Autowired
    public void setInvitationRepository(InvitationRepository invitationRepository) {
        this.invitationRepository = invitationRepository;
    }

    @Autowired
    public void setProfileRepository(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    public Invitation sendInvitation(Long senderId, Long receiverId) {
        if (senderId.equals(receiverId)) {
            throw new BusinessRuleException("Не можна надіслати запрошення самому собі");
        }

        if (!profileRepository.existsById(senderId) || !profileRepository.existsById(receiverId)) {
            throw new NotFoundException("Користувача не знайдено");
        }

        if (invitationRepository.findBySenderAndReceiver(senderId, receiverId).isPresent()) {
            throw new BusinessRuleException("Запрошення вже було надіслано");
        }

        if (invitationRepository.findBySenderAndReceiver(receiverId, senderId).isPresent()) {
            throw new BusinessRuleException("Цей користувач уже надіслав вам запрошення - дайте на нього відповідь");
        }

        return invitationRepository.save(senderId, receiverId);
    }

    public List<Invitation> getAllInvitations() {
        return invitationRepository.findAll();
    }

    /** Фільтрація та пагінація запрошень (використовується REST API). */
    public PagedResult<Invitation> findInvitations(InvitationFilter filter, int page, int size) {
        List<Invitation> matched = invitationRepository.findAll().stream()
                .filter(i -> filter.senderId() == null || i.getSenderId().equals(filter.senderId()))
                .filter(i -> filter.receiverId() == null || i.getReceiverId().equals(filter.receiverId()))
                .filter(i -> filter.status() == null || i.getStatus() == filter.status())
                .toList();
        return PagedResult.of(matched, page, size);
    }

    public Invitation getRequiredInvitation(Long invitationId) {
        return invitationRepository.findById(invitationId)
                .orElseThrow(() -> new NotFoundException("Запрошення з id=" + invitationId + " не знайдено"));
    }

    public List<Invitation> getReceivedInvitations(Long receiverId) {
        return invitationRepository.findByReceiverId(receiverId);
    }

    public List<Invitation> getSentInvitations(Long senderId) {
        return invitationRepository.findBySenderId(senderId);
    }

    public Invitation acceptInvitation(Long invitationId, Long currentUserId) {
        return changeStatus(invitationId, currentUserId, InvitationStatus.ACCEPTED);
    }

    public Invitation declineInvitation(Long invitationId, Long currentUserId) {
        return changeStatus(invitationId, currentUserId, InvitationStatus.DECLINED);
    }

    /**
     * Змінити стан запрошення може лише отримувач і лише поки воно очікує відповіді.
     * Повторне встановлення того самого стану нічого не змінює (ідемпотентність).
     */
    public Invitation changeStatus(Long invitationId, Long currentUserId, InvitationStatus newStatus) {
        Invitation invitation = getRequiredInvitation(invitationId);

        if (!invitation.getReceiverId().equals(currentUserId)) {
            throw new OperationNotAllowedException("Відповісти на запрошення може лише отримувач");
        }

        if (invitation.getStatus() == newStatus) {
            return invitation;
        }

        if (newStatus == InvitationStatus.PENDING || !invitation.isPending()) {
            throw new BusinessRuleException("Неможливо змінити стан запрошення з «"
                    + invitation.getStatus().getDisplayName() + "» на «" + newStatus.getDisplayName() + "»");
        }

        invitation.setStatus(newStatus);
        return invitationRepository.update(invitation);
    }

    /** Видалити (відкликати) запрошення може лише його учасник - відправник або отримувач. */
    public void deleteInvitation(Long invitationId, Long currentUserId) {
        Invitation invitation = getRequiredInvitation(invitationId);
        if (!invitation.involves(currentUserId)) {
            throw new OperationNotAllowedException("Видалити запрошення може лише його учасник");
        }
        invitationRepository.deleteById(invitationId);
    }

    /**
     * Отримувач бачить закриту інформацію відправника одразу після отримання запрошення,
     * а відправник бачить закриту інформацію отримувача лише після підтвердження.
     */
    public boolean canViewPrivateInfo(Long viewerId, Long profileId) {
        if (viewerId.equals(profileId)) {
            return true;
        }

        boolean viewerInvitationAccepted = invitationRepository
                .findBySenderAndReceiver(viewerId, profileId)
                .map(Invitation::isAccepted)
                .orElse(false);

        boolean profileInvitedViewer = invitationRepository
                .findBySenderAndReceiver(profileId, viewerId)
                .isPresent();

        return viewerInvitationAccepted || profileInvitedViewer;
    }

    public ConnectionStatus getConnectionStatus(Long viewerId, Long profileId) {
        if (viewerId.equals(profileId)) {
            return ConnectionStatus.SELF;
        }

        Optional<Invitation> sent = invitationRepository.findBySenderAndReceiver(viewerId, profileId);
        if (sent.isPresent()) {
            return switch (sent.get().getStatus()) {
                case PENDING -> ConnectionStatus.INVITATION_SENT;
                case ACCEPTED -> ConnectionStatus.MATCHED;
                case DECLINED -> ConnectionStatus.DECLINED;
            };
        }

        Optional<Invitation> received = invitationRepository.findBySenderAndReceiver(profileId, viewerId);
        if (received.isPresent()) {
            return switch (received.get().getStatus()) {
                case PENDING -> ConnectionStatus.INVITATION_RECEIVED;
                case ACCEPTED -> ConnectionStatus.MATCHED;
                case DECLINED -> ConnectionStatus.DECLINED;
            };
        }

        return ConnectionStatus.NONE;
    }

    /** Запрошення від profileId до viewerId (потрібне, щоб показати кнопки «Прийняти/Відхилити» на сторінці анкети). */
    public Optional<Invitation> findInvitation(Long senderId, Long receiverId) {
        return invitationRepository.findBySenderAndReceiver(senderId, receiverId);
    }
}
