package com.ua.estore.cgsWeb.controllers;

import jakarta.servlet.http.HttpSession;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice
public class GlobalModelAdvice {

    @ModelAttribute("session")
    public HttpSession addSessionToModel(HttpSession session) {
        return session;
    }
}
