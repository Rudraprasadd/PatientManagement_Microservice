package com.pm.authservice.service;

import com.pm.authservice.dto.LoginRequestDTO;
import com.pm.authservice.util.JwtUtil;
import io.jsonwebtoken.JwtException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthService(UserService userService, PasswordEncoder passwordEncoder, JwtUtil jwtUtil) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtUtil = jwtUtil;
    }

    //password request -> password -> encoded -> encoded pass
    public Optional<String> authenticate(LoginRequestDTO loginRequestDTO) {
        Optional<String> token = userService
                .findByEmail(loginRequestDTO.getEmail())
                .filter(U -> passwordEncoder.matches(LoginRequestDTO.getPassword(),
                        U.getPassword()))
                .map(U -> jwtUtil.generateToken(U.getEmail(),U.getRole()));

        return token;
    }

    public boolean validateToken(String token) {
        try {
            JwtUtil.validateToken(token);
            return true;
        }catch(JwtException e){
            return false;
        }
    }
}
