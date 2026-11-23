package com.example.datingsite.controller;

import com.example.datingsite.exception.BusinessRuleException;
import com.example.datingsite.service.CurrentUserService;
import com.example.datingsite.service.InvitationService;
import com.example.datingsite.service.ProfileService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
public class InvitationController {

    private final InvitationService invitationService;
    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public InvitationController(InvitationService invitationService,
                                ProfileService profileService,
                                CurrentUserService currentUserService) {
        this.invitationService = invitationService;
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @GetMapping("/invitations")
    public String viewInvitations(Model model) {
        Long currentUserId = currentUserService.getCurrentUserId();

        model.addAttribute("received", invitationService.getReceivedInvitations(currentUserId));
        model.addAttribute("sent", invitationService.getSentInvitations(currentUserId));
        model.addAttribute("profilesById", profileService.getProfilesById());
        return "invitations";
    }

    @PostMapping("/invitations")
    public String sendInvitation(@RequestParam Long receiverId, RedirectAttributes redirectAttributes) {
        Long currentUserId = currentUserService.getCurrentUserId();

        try {
            invitationService.sendInvitation(currentUserId, receiverId);
            redirectAttributes.addFlashAttribute("flashType", "success");
            redirectAttributes.addFlashAttribute("flashMessage", "Запрошення надіслано.");
        } catch (BusinessRuleException e) {
            redirectAttributes.addFlashAttribute("flashType", "error");
            redirectAttributes.addFlashAttribute("flashMessage", e.getMessage());
        }

        return "redirect:/profiles/" + receiverId;
    }

    @PostMapping("/invitations/{id}/accept")
    public String acceptInvitation(@PathVariable Long id,
                                   @RequestParam(required = false) Long fromProfile,
                                   RedirectAttributes redirectAttributes) {
        invitationService.acceptInvitation(id, currentUserService.getCurrentUserId());
        redirectAttributes.addFlashAttribute("flashType", "success");
        redirectAttributes.addFlashAttribute("flashMessage", "Запрошення прийнято.");
        return redirectBack(fromProfile);
    }

    @PostMapping("/invitations/{id}/decline")
    public String declineInvitation(@PathVariable Long id,
                                    @RequestParam(required = false) Long fromProfile,
                                    RedirectAttributes redirectAttributes) {
        invitationService.declineInvitation(id, currentUserService.getCurrentUserId());
        redirectAttributes.addFlashAttribute("flashType", "info");
        redirectAttributes.addFlashAttribute("flashMessage", "Запрошення відхилено.");
        return redirectBack(fromProfile);
    }

    /** Повертає користувача на сторінку, з якої він прийшов (адреса формується лише з числового id). */
    private String redirectBack(Long fromProfile) {
        return fromProfile != null ? "redirect:/profiles/" + fromProfile : "redirect:/invitations";
    }
}
