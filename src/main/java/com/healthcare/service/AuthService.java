package com.healthcare.service;

import com.healthcare.dto.request.AuthRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.User;
import com.healthcare.exception.DuplicateResourceException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.UserRepository;
import com.healthcare.security.JwtService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;

    @Transactional
    public ApiResponse.AuthResponse register(AuthRequest.RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new DuplicateResourceException("User", "email", request.email());
        }
        User user = User.builder()
                .name(request.name())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .role(User.Role.USER)
                .language(request.language() != null ? request.language() : "en")
                .build();
        user = userRepository.save(user);
        log.info("New user registered: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional
    public ApiResponse.AuthResponse login(AuthRequest.LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.email(), request.password()));
        User user = userRepository.findByEmail(request.email())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", request.email()));
        log.info("User logged in: {}", user.getEmail());
        return generateAuthResponse(user);
    }

    @Transactional(readOnly = true)
    public ApiResponse.UserResponse getCurrentUser(String email) {
        User user = findByEmail(email);
        return mapToUserResponse(user);
    }

    @Transactional
    public ApiResponse.AuthResponse refreshToken(AuthRequest.RefreshTokenRequest request) {
        String userEmail = jwtService.extractUsername(request.refreshToken());
        UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
        if (!jwtService.isTokenValid(request.refreshToken(), userDetails)) {
            throw new IllegalArgumentException("Invalid or expired refresh token");
        }
        User user = findByEmail(userEmail);
        // Validate stored refresh token matches
        if (user.getRefreshToken() == null || !user.getRefreshToken().equals(request.refreshToken())) {
            throw new IllegalArgumentException("Refresh token has been revoked");
        }
        return generateAuthResponse(user);
    }

    @Transactional
    public ApiResponse.UserResponse updateProfile(String email, AuthRequest.UpdateProfileRequest request) {
        User user = findByEmail(email);
        if (request.name() != null && !request.name().isBlank()) {
            user.setName(request.name());
        }
        if (request.language() != null && !request.language().isBlank()) {
            user.setLanguage(request.language());
        }
        user = userRepository.save(user);
        log.info("Profile updated for user: {}", email);
        return mapToUserResponse(user);
    }

    @Transactional
    public void changePassword(String email, AuthRequest.ChangePasswordRequest request) {
        User user = findByEmail(email);
        if (!passwordEncoder.matches(request.currentPassword(), user.getPassword())) {
            throw new BadCredentialsException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.newPassword()));
        user.setRefreshToken(null); // Invalidate all sessions
        userRepository.save(user);
        log.info("Password changed for user: {}", email);
    }

    @Transactional
    public void logout(String email) {
        userRepository.clearRefreshToken(email);
        log.info("User logged out: {}", email);
    }

    @Transactional
    public void deactivateAccount(String email) {
        User user = findByEmail(email);
        user.setIsActive(false);
        user.setRefreshToken(null);
        userRepository.save(user);
        log.info("Account deactivated for user: {}", email);
    }

    private ApiResponse.AuthResponse generateAuthResponse(User user) {
        UserDetails userDetails = userDetailsService.loadUserByUsername(user.getEmail());
        String accessToken = jwtService.generateAccessToken(userDetails);
        String refreshToken = jwtService.generateRefreshToken(userDetails);
        userRepository.updateRefreshToken(user.getId(), refreshToken);
        return new ApiResponse.AuthResponse(
                accessToken, refreshToken, "Bearer",
                jwtService.getAccessTokenExpiration(), mapToUserResponse(user));
    }

    private User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", email));
    }

    public ApiResponse.UserResponse mapToUserResponse(User user) {
        return new ApiResponse.UserResponse(
                user.getId(), user.getName(), user.getEmail(),
                user.getRole(), user.getLanguage(), user.getIsActive(), user.getCreatedAt());
    }
}
