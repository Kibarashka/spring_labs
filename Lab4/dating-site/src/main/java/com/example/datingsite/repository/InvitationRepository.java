package com.example.datingsite.repository;

import com.example.datingsite.model.Invitation;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fake-реалізація сховища запрошень (дані зберігаються в пам'яті).
 */
@Repository
public class InvitationRepository {

    private final Map<Long, Invitation> invitations = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public Invitation save(Long senderId, Long receiverId) {
        Invitation invitation = new Invitation(nextId.getAndIncrement(), senderId, receiverId);
        invitations.put(invitation.getId(), invitation);
        return invitation;
    }

    public Invitation update(Invitation invitation) {
        invitations.put(invitation.getId(), invitation);
        return invitation;
    }

    public List<Invitation> findAll() {
        List<Invitation> result = new ArrayList<>(invitations.values());
        result.sort(Comparator.comparing(Invitation::getId));
        return result;
    }

    public Optional<Invitation> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(invitations.get(id));
    }

    public List<Invitation> findByReceiverId(Long receiverId) {
        return findAll().stream()
                .filter(i -> i.getReceiverId().equals(receiverId))
                .toList();
    }

    public List<Invitation> findBySenderId(Long senderId) {
        return findAll().stream()
                .filter(i -> i.getSenderId().equals(senderId))
                .toList();
    }

    public Optional<Invitation> findBySenderAndReceiver(Long senderId, Long receiverId) {
        return findAll().stream()
                .filter(i -> i.getSenderId().equals(senderId)
                        && i.getReceiverId().equals(receiverId))
                .findFirst();
    }

    public void deleteById(Long id) {
        invitations.remove(id);
    }

    /** Видаляє всі запрошення, у яких бере участь анкета (використовується при видаленні анкети). */
    public void deleteByProfileId(Long profileId) {
        invitations.values().removeIf(i -> i.involves(profileId));
    }
}
