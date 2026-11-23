package com.example.datingsite.service;

import com.example.datingsite.exception.NotFoundException;
import com.example.datingsite.exception.OperationNotAllowedException;
import com.example.datingsite.model.Profile;
import com.example.datingsite.repository.InvitationRepository;
import com.example.datingsite.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

@Service
public class ProfileService {

    /** Поля, за якими дозволено сортування. */
    public static final List<String> SORTABLE_FIELDS = List.of("id", "name", "age");

    private final ProfileRepository profileRepository;
    private final InvitationRepository invitationRepository;

    // Ін'єкція залежностей через конструктор
    public ProfileService(ProfileRepository profileRepository,
                          InvitationRepository invitationRepository) {
        this.profileRepository = profileRepository;
        this.invitationRepository = invitationRepository;
    }

    public List<Profile> getAllProfiles() {
        return profileRepository.findAll();
    }

    public Optional<Profile> getProfileById(Long id) {
        return profileRepository.findById(id);
    }

    public Profile getRequiredProfile(Long id) {
        return profileRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Анкету з id=" + id + " не знайдено"));
    }

    public boolean exists(Long id) {
        return profileRepository.existsById(id);
    }

    public List<Profile> searchProfiles(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return getAllProfiles();
        }
        return profileRepository.searchByKeyword(keyword);
    }

    /**
     * Фільтрація, сортування та пагінація анкет (використовується REST API).
     *
     * @param sortField одне з {@link #SORTABLE_FIELDS}
     */
    public PagedResult<Profile> findProfiles(ProfileFilter filter, int page, int size,
                                             String sortField, boolean ascending) {
        if (filter.minAge() != null && filter.maxAge() != null && filter.minAge() > filter.maxAge()) {
            throw new IllegalArgumentException("Параметр minAge не може бути більшим за maxAge");
        }
        if (!SORTABLE_FIELDS.contains(sortField)) {
            throw new IllegalArgumentException("Сортування за полем «" + sortField + "» не підтримується");
        }

        Stream<Profile> stream = searchProfiles(filter.keyword()).stream();

        if (filter.name() != null && !filter.name().isBlank()) {
            String needle = filter.name().trim().toLowerCase(Locale.ROOT);
            stream = stream.filter(p -> p.getName().toLowerCase(Locale.ROOT).contains(needle));
        }
        if (filter.minAge() != null) {
            stream = stream.filter(p -> p.getAge() >= filter.minAge());
        }
        if (filter.maxAge() != null) {
            stream = stream.filter(p -> p.getAge() <= filter.maxAge());
        }

        Comparator<Profile> comparator = switch (sortField) {
            case "name" -> Comparator.comparing(Profile::getName, String.CASE_INSENSITIVE_ORDER);
            case "age" -> Comparator.comparingInt(Profile::getAge);
            default -> Comparator.comparing(Profile::getId);
        };
        if (!ascending) {
            comparator = comparator.reversed();
        }
        // Другорядне сортування за id робить порядок детермінованим
        comparator = comparator.thenComparing(Profile::getId);

        return PagedResult.of(stream.sorted(comparator).toList(), page, size);
    }

    /** Анкети, проіндексовані за id, - зручно для відображення імен у списках запрошень. */
    public Map<Long, Profile> getProfilesById() {
        Map<Long, Profile> result = new LinkedHashMap<>();
        getAllProfiles().forEach(p -> result.put(p.getId(), p));
        return result;
    }

    public Profile createProfile(String name, int age, String publicInfo,
                                 String privateInfo, List<String> keywords) {
        Profile profile = new Profile(null, name.trim(), age,
                normalize(publicInfo), normalize(privateInfo), normalizeKeywords(keywords));
        return profileRepository.save(profile);
    }

    /**
     * Перевіряє, що анкета існує (інакше 404) і належить поточному користувачу (інакше 403).
     */
    public Profile requireOwnProfile(Long currentUserId, Long profileId) {
        Profile profile = getRequiredProfile(profileId);
        if (!profileId.equals(currentUserId)) {
            throw new OperationNotAllowedException("Можна змінювати лише власну анкету");
        }
        return profile;
    }

    public Profile updateProfile(Long currentUserId, Profile profile) {
        requireOwnProfile(currentUserId, profile.getId());

        profile.setName(profile.getName().trim());
        profile.setPublicInfo(normalize(profile.getPublicInfo()));
        profile.setPrivateInfo(normalize(profile.getPrivateInfo()));
        profile.setKeywords(normalizeKeywords(profile.getKeywords()));
        return profileRepository.save(profile);
    }

    public void deleteProfile(Long currentUserId, Long id) {
        requireOwnProfile(currentUserId, id);
        invitationRepository.deleteByProfileId(id);
        profileRepository.deleteById(id);
    }

    /** Перетворює рядок "музика, кіно , спорт" на список ключових слів без порожніх та повторів. */
    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return normalizeKeywords(Arrays.asList(raw.split(",")));
    }

    public static List<String> normalizeKeywords(List<String> keywords) {
        if (keywords == null) {
            return List.of();
        }
        return keywords.stream()
                .filter(k -> k != null && !k.isBlank())
                .map(k -> k.trim().toLowerCase())
                .distinct()
                .toList();
    }

    private static String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }
}
