package com.connect.pairr.auth;

import com.connect.pairr.model.dto.AuthResponse;
import com.connect.pairr.model.dto.LoginRequest;
import com.connect.pairr.model.dto.RegisterRequest;
import com.connect.pairr.model.entity.User;
import com.connect.pairr.model.enums.Role;
import com.connect.pairr.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse register(RegisterRequest request) {

        if (userRepository.existsByEmail(request.email())) {
            throw new RuntimeException("Email already exists");
        }

        if (userRepository.existsByUsername(request.username())) {
            throw new RuntimeException("Username already exists");
        }

        User user = new User();
        user.setRole(Role.USER);
        user.setEmail(request.email());
        user.setUsername(request.username());
        user.setDisplayName(request.displayName());
        user.setPassword(passwordEncoder.encode(request.password()));

        userRepository.save(user);

        String token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        // Search by email or username
        User user = userRepository.findByEmailOrUsername(request.email(), request.email())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (user.getPassword() == null) {
            throw new RuntimeException("Please login with Google for this account");
        }

        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        String token = jwtService.generateToken(user);

        return new AuthResponse(token);
    }
}
