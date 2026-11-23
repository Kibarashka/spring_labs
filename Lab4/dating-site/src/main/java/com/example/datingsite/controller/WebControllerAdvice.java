package com.example.datingsite.controller;

import com.example.datingsite.exception.BusinessRuleException;
import com.example.datingsite.exception.NotFoundException;
import com.example.datingsite.exception.OperationNotAllowedException;
import com.example.datingsite.model.Profile;
import com.example.datingsite.service.CurrentUserService;
import com.example.datingsite.service.ProfileService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.ModelAndView;

import java.util.List;

/**
 * Спільні дані моделі для всіх сторінок та обробка винятків веб-рівня.
 * Діє лише на контролери з пакета controller (HTML-сторінки).
 */
@ControllerAdvice(basePackageClasses = WebControllerAdvice.class)
public class WebControllerAdvice {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;

    public WebControllerAdvice(ProfileService profileService, CurrentUserService currentUserService) {
        this.profileService = profileService;
        this.currentUserService = currentUserService;
    }

    @ModelAttribute("currentUser")
    public Profile currentUser() {
        Long id = currentUserService.getCurrentUserId();
        return id == null ? null : profileService.getProfileById(id).orElse(null);
    }

    @ModelAttribute("switchableProfiles")
    public List<Profile> switchableProfiles() {
        return profileService.getAllProfiles();
    }

    @ExceptionHandler(NotFoundException.class)
    public ModelAndView handleNotFound(NotFoundException e, HttpServletRequest request) {
        return errorPage(HttpStatus.NOT_FOUND, e.getMessage(), request);
    }

    @ExceptionHandler(OperationNotAllowedException.class)
    public ModelAndView handleForbidden(OperationNotAllowedException e, HttpServletRequest request) {
        return errorPage(HttpStatus.FORBIDDEN, e.getMessage(), request);
    }

    @ExceptionHandler(BusinessRuleException.class)
    public ModelAndView handleConflict(BusinessRuleException e, HttpServletRequest request) {
        return errorPage(HttpStatus.CONFLICT, e.getMessage(), request);
    }

    @ExceptionHandler({MethodArgumentTypeMismatchException.class, MissingServletRequestParameterException.class})
    public ModelAndView handleBadRequest(Exception e, HttpServletRequest request) {
        return errorPage(HttpStatus.BAD_REQUEST, "Некоректні параметри запиту", request);
    }

    private ModelAndView errorPage(HttpStatus status, String message, HttpServletRequest request) {
        ModelAndView mav = new ModelAndView("error");
        mav.setStatus(status);
        mav.addObject("status", status.value());
        mav.addObject("error", status.getReasonPhrase());
        mav.addObject("message", message);
        mav.addObject("path", request.getRequestURI());
        // @ModelAttribute-методи не застосовуються до ModelAndView з обробника винятків,
        // тому дані для шапки сторінки додаємо вручну
        mav.addObject("currentUser", currentUser());
        mav.addObject("switchableProfiles", switchableProfiles());
        return mav;
    }
}
