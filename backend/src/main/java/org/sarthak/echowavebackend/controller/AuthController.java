package org.sarthak.echowavebackend.controller;

import org.sarthak.echowavebackend.auth.JwtUtil;
import org.sarthak.echowavebackend.entity.User;
import org.sarthak.echowavebackend.service.AuthService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {
    private final AuthService authService;
    private final JwtUtil jwtUtil;
    public AuthController(AuthService authService, JwtUtil jwtUtil) {
        this.authService = authService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Map<String, String> login(@RequestBody Map<String, String> req) {

        String username = req.get("username");
        String password = req.get("password");

        User user = authService.authenticate(username, password);

        String token = jwtUtil.generateToken(user.getUsername());

        return Map.of("token", token);
    }

    @PostMapping("/register")
    public Map<String, String> register(@RequestBody Map<String, String> req) {

        String username = req.get("username");
        String password = req.get("password");

        authService.register(username, password);

        return Map.of("message", "User registered successfully");
    }
}
