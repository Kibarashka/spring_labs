package com.example.datingsite.controller;

import com.example.datingsite.controller.form.ProfileForm;
import com.example.datingsite.model.ConnectionStatus;
import com.example.datingsite.model.Profile;
import com.example.datingsite.service.CurrentUserService;
import com.example.datingsite.service.InvitationService;
import com.example.datingsite.service.ProfileService;
import jakarta.validation.Valid;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
public class ProfileController {

    private final ProfileService profileService;
    private final InvitationService invitationService;
    private final CurrentUserService currentUserService;

    public ProfileController(ProfileService profileService,
                             InvitationService invitationService,
                             CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.invitationService = invitationService;
        this.currentUserService = currentUserService;
    }

    /** Головна сторінка: список анкет та пошук за ключовими словами (GET - запит лише читає дані). */
    @GetMapping("/")
    public String index(@RequestParam(required = false) String keyword, Model model) {
        String normalizedKeyword = (keyword == null || keyword.isBlank()) ? null : keyword.trim();
        List<Profile> profiles = profileService.searchProfiles(normalizedKeyword);

        model.addAttribute("profiles", profiles);
        model.addAttribute("keyword", normalizedKeyword);
        model.addAttribute("totalCount", profileService.getAllProfiles().size());
        return "index";
    }

    @GetMapping("/profiles/{id}")
    public String viewProfile(@PathVariable Long id, Model model) {
        Profile profile = profileService.getRequiredProfile(id);
        Long currentUserId = currentUserService.getCurrentUserId();
        ConnectionStatus connectionStatus = invitationService.getConnectionStatus(currentUserId, id);

        model.addAttribute("profile", profile);
        model.addAttribute("canViewPrivateInfo", invitationService.canViewPrivateInfo(currentUserId, id));
        model.addAttribute("connectionStatus", connectionStatus);
        model.addAttribute("incomingInvitation",
                invitationService.findInvitation(id, currentUserId).orElse(null));
        return "profile";
    }

    @GetMapping("/profiles/new")
    public String newProfileForm(Model model) {
        model.addAttribute("profileForm", new ProfileForm());
        model.addAttribute("mode", "create");
        return "profile-form";
    }

    @PostMapping("/profiles")
    public String createProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                                BindingResult bindingResult,
                                Model model,
                                RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "create");
            return "profile-form";
        }

        Profile created = profileService.createProfile(form.getName(), form.getAge(),
                form.getPublicInfo(), form.getPrivateInfo(),
                ProfileService.parseKeywords(form.getKeywords()));
        currentUserService.switchUser(created.getId());

        redirectAttributes.addFlashAttribute("flashType", "success");
        redirectAttributes.addFlashAttribute("flashMessage",
                "Анкету створено. Тепер ви переглядаєте сайт як " + created.getName() + ".");
        // Post/Redirect/Get: після успішного POST перенаправляємо браузер на GET-сторінку
        return "redirect:/profiles/" + created.getId();
    }

    @GetMapping("/profiles/me/edit")
    public String editProfile(Model model) {
        Profile profile = profileService.getRequiredProfile(currentUserService.getCurrentUserId());
        model.addAttribute("profileForm", ProfileForm.from(profile));
        model.addAttribute("mode", "edit");
        return "profile-form";
    }

    @PostMapping("/profiles/me/edit")
    public String saveProfile(@Valid @ModelAttribute("profileForm") ProfileForm form,
                              BindingResult bindingResult,
                              Model model,
                              RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("mode", "edit");
            return "profile-form";
        }

        Long currentUserId = currentUserService.getCurrentUserId();
        Profile updated = new Profile(currentUserId, form.getName(), form.getAge(),
                form.getPublicInfo(), form.getPrivateInfo(),
                ProfileService.parseKeywords(form.getKeywords()));
        profileService.updateProfile(currentUserId, updated);

        redirectAttributes.addFlashAttribute("flashType", "success");
        redirectAttributes.addFlashAttribute("flashMessage", "Зміни в анкеті збережено.");
        return "redirect:/profiles/" + currentUserId;
    }

    @PostMapping("/switch-user")
    public String switchUser(@RequestParam Long userId, RedirectAttributes redirectAttributes) {
        if (currentUserService.switchUser(userId)) {
            String name = profileService.getRequiredProfile(userId).getName();
            redirectAttributes.addFlashAttribute("flashType", "info");
            redirectAttributes.addFlashAttribute("flashMessage", "Тепер ви переглядаєте сайт як " + name + ".");
        } else {
            redirectAttributes.addFlashAttribute("flashType", "error");
            redirectAttributes.addFlashAttribute("flashMessage", "Користувача не знайдено.");
        }
        return "redirect:/";
    }
}
