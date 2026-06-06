package com.banking.gateway.controller;

import com.banking.gateway.security.JwtUtil;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final JwtUtil jwtUtil;

    @PostMapping("/token")
    public TokenResponse getToken(@RequestBody LoginRequest request) {
        if (!"admin".equals(request.getUsername()) || !"admin".equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
        }
        return new TokenResponse(jwtUtil.generateToken(request.getUsername()));
    }

    @Getter
    @Setter
    public static class LoginRequest {
        private String username;
        private String password;
    }

    @Getter
    public static class TokenResponse {
        private final String token;
        public TokenResponse(String token) { this.token = token; }
    }
}
