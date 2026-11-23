package com.example.datingsite.service;

import com.example.datingsite.exception.NotFoundException;
import com.example.datingsite.exception.OperationNotAllowedException;
import com.example.datingsite.model.Profile;
import com.example.datingsite.repository.InvitationRepository;
import com.example.datingsite.repository.ProfileRepository;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProfileService {

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

    /** Анкети, проіндексовані за id, - зручно для відображення імен у списках запрошень. */
    public Map<Long, Profile> getProfilesById() {
        Map<Long, Profile> result = new LinkedHashMap<>();
        getAllProfiles().forEach(p -> result.put(p.getId(), p));
        return result;
    }

    public Profile createProfile(String name, int age, String publicInfo,
                                 String privateInfo, List<String> keywords) {
        Profile profile = new Profile(null, name.trim(), age,
                normalize(publicInfo), normalize(privateInfo), keywords);
        return profileRepository.save(profile);
    }

    public Profile updateProfile(Long currentUserId, Profile profile) {
        if (!currentUserId.equals(profile.getId())) {
            throw new OperationNotAllowedException("Можна редагувати лише власну анкету");
        }

        if (!profileRepository.existsById(profile.getId())) {
            throw new NotFoundException("Анкету не знайдено");
        }

        profile.setName(profile.getName().trim());
        profile.setPublicInfo(normalize(profile.getPublicInfo()));
        profile.setPrivateInfo(normalize(profile.getPrivateInfo()));
        return profileRepository.save(profile);
    }

    public void deleteProfile(Long id) {
        if (!profileRepository.existsById(id)) {
            throw new NotFoundException("Анкету з id=" + id + " не знайдено");
        }
        invitationRepository.deleteByProfileId(id);
        profileRepository.deleteById(id);
    }

    /** Перетворює рядок "музика, кіно , спорт" на список ключових слів без порожніх та повторів. */
    public static List<String> parseKeywords(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        return Arrays.stream(raw.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .map(String::toLowerCase)
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
