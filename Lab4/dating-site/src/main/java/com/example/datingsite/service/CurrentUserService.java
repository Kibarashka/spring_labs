package com.example.datingsite.service;

import com.example.datingsite.model.Profile;
import com.example.datingsite.repository.ProfileRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.context.annotation.SessionScope;

import java.util.List;

/**
 * Зберігає "поточного користувача" окремо для кожної HTTP-сесії.
 * <p>
 * У лабораторній роботі №2 цей бін був singleton, тому поточний користувач
 * був спільним для всіх відвідувачів. Тепер бін має область видимості session:
 * Spring створює окремий екземпляр для кожного браузера (сесії), а в singleton-контролери
 * впроваджується проксі, який делегує виклики екземпляру поточної сесії.
 * Повноцінна аутентифікація в цій роботі не реалізується (п. 2.5 завдання).
 */
@Service
@SessionScope
public class CurrentUserService {

    private final ProfileRepository profileRepository;
    private Long currentUserId = 1L;

    public CurrentUserService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    /**
     * Повертає id поточного користувача. Якщо його анкету видалено,
     * поточним стає користувач з найменшим id.
     */
    public Long getCurrentUserId() {
        if (!profileRepository.existsById(currentUserId)) {
            List<Profile> all = profileRepository.findAll();
            currentUserId = all.isEmpty() ? null : all.getFirst().getId();
        }
        return currentUserId;
    }

    public boolean switchUser(Long userId) {
        if (profileRepository.existsById(userId)) {
            currentUserId = userId;
            return true;
        }
        return false;
    }
}
