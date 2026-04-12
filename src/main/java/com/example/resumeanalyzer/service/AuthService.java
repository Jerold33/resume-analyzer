package com.example.resumeanalyzer.service;

import com.example.resumeanalyzer.config.JwtProperties;
import com.example.resumeanalyzer.dto.AuthResponse;
import com.example.resumeanalyzer.dto.LoginRequest;
import com.example.resumeanalyzer.dto.RegisterRequest;
import com.example.resumeanalyzer.entity.User;
import com.example.resumeanalyzer.exception.ApiException;
import com.example.resumeanalyzer.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthenticationManager authenticationManager,
            UserDetailsServiceImpl userDetailsService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ApiException(HttpStatus.CONFLICT, "Email is already registered");
        }
        User user = new User(request.email().trim().toLowerCase(), passwordEncoder.encode(request.password()));
        userRepository.save(user);
        return issueToken(user.getEmail());
    }

    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.email().trim().toLowerCase(),
                        request.password()
                )
        );
        return issueToken(request.email().trim().toLowerCase());
    }

    private AuthResponse issueToken(String email) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(email);
        String token = jwtService.generateToken(userDetails);
        return new AuthResponse(token, email, jwtProperties.expirationMs());
    }
}
