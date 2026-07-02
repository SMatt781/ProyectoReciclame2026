package com.example.proyectoreciclame.Controller;

import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class SecurityStatusController {

    @GetMapping("/api/security/session-status")
    public Map<String, Object> sessionStatus(Authentication authentication) {
        boolean authenticated = authentication != null
                && authentication.isAuthenticated()
                && !(authentication instanceof AnonymousAuthenticationToken);

        return Map.of(
                "authenticated", authenticated,
                "redirect", authenticated ? "" : "/login"
        );
    }
}
