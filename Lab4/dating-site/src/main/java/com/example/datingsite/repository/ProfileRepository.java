package com.example.datingsite.repository;

import com.example.datingsite.model.Profile;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Fake-реалізація сховища анкет (дані зберігаються в пам'яті).
 * Повноцінна робота з БД передбачена в наступних лабораторних роботах.
 */
@Repository
public class ProfileRepository {

    private final Map<Long, Profile> profiles = new ConcurrentHashMap<>();
    private final AtomicLong nextId = new AtomicLong(1);

    public ProfileRepository() {
        save(new Profile(null, "Anna", 22,
                "Люблю подорожі та музику",
                "anna@example.com",
                List.of("музика", "подорожі")));

        save(new Profile(null, "Oleg", 25,
                "Захоплююсь спортом",
                "oleg@example.com",
                List.of("спорт", "футбол")));

        save(new Profile(null, "Iryna", 24,
                "Фотографую міста і варю найкращу каву на районі",
                "iryna@example.com",
                List.of("фото", "кава", "подорожі")));

        save(new Profile(null, "Maksym", 27,
                null,
                "+380 67 000 00 00",
                List.of("ігри", "кіно")));

        save(new Profile(null, "Sofia", 21,
                "Студентка КПІ, бігаю марафони",
                null,
                List.of("спорт", "біг", "книги")));
    }

    public List<Profile> findAll() {
        List<Profile> result = new ArrayList<>(profiles.values());
        result.sort(Comparator.comparing(Profile::getId));
        return result;
    }

    public Optional<Profile> findById(Long id) {
        if (id == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(profiles.get(id));
    }

    public boolean existsById(Long id) {
        return id != null && profiles.containsKey(id);
    }

    public List<Profile> searchByKeyword(String keyword) {
        String needle = keyword.toLowerCase().trim();
        return findAll().stream()
                .filter(profile -> profile.getKeywords().stream()
                        .anyMatch(k -> k.toLowerCase().contains(needle)))
                .toList();
    }

    public Profile save(Profile profile) {
        if (profile.getId() == null) {
            profile.setId(nextId.getAndIncrement());
        }
        profiles.put(profile.getId(), profile);
        return profile;
    }

    public void deleteById(Long id) {
        profiles.remove(id);
    }
}
