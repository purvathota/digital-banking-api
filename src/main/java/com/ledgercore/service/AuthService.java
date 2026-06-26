package com.ledgercore.service;

import com.ledgercore.config.JwtTokenProvider;
import com.ledgercore.dto.request.LoginRequest;
import com.ledgercore.dto.request.RegisterRequest;
import com.ledgercore.dto.response.AuthResponse;
import com.ledgercore.entity.User;
import com.ledgercore.exception.DuplicateEmailException;
import com.ledgercore.repository.UserRepository;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service handling user registration and authentication.
 * Passwords are hashed with BCrypt. Tokens are signed JWTs (HMAC-SHA256).
 */
@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;

    public AuthService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder,
                       JwtTokenProvider jwtTokenProvider) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenProvider = jwtTokenProvider;
    }

    /**
     * Register a new user. Validates that the email is unique,
     * hashes the password with BCrypt, and returns a signed JWT.
     */
    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateEmailException(
                    "An account with email '" + request.email() + "' already exists");
        }

        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.fullName()
        );
        userRepository.save(user);

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, jwtTokenProvider.getExpirationSeconds());
    }

    /**
     * Authenticate a user by email and password.
     * Verifies the BCrypt hash and returns a signed JWT on success.
     */
    @Transactional(readOnly = true)
    public AuthResponse authenticate(LoginRequest request) {
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new BadCredentialsException("Invalid email or password"));

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BadCredentialsException("Invalid email or password");
        }

        String token = jwtTokenProvider.generateToken(user);
        return new AuthResponse(token, jwtTokenProvider.getExpirationSeconds());
    }
}
