package dev.enzo.giacpe.identity.infrastructure.web;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class CurrentUserController {

    @GetMapping("/api/auth/me")
    public ResponseEntity<Map<String, Object>> currentUser(
            Authentication authentication) {

        return ResponseEntity.ok(Map.of(
                "email", authentication.getName(),
                "roles", authentication.getAuthorities().stream()
                        .map(authority -> authority.getAuthority())
                        .toList()
        ));
    }
}
