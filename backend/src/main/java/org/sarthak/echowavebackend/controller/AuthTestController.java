package org.sarthak.echowavebackend.controller;

import org.sarthak.echowavebackend.websocket.security.JwtService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthTestController {

    private final JwtService jwtService;

    public AuthTestController(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @GetMapping("/token/{username}")
    public Map<String, String> token(@PathVariable String username) {
        return Map.of(
                "token",
                jwtService.generateToken(username)
        );
    }
}

