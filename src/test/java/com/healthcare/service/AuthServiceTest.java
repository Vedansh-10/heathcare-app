package com.healthcare.service;

import com.healthcare.dto.request.AuthRequest;
import com.healthcare.dto.response.ApiResponse;
import com.healthcare.entity.User;
import com.healthcare.exception.DuplicateResourceException;
import com.healthcare.exception.ResourceNotFoundException;
import com.healthcare.repository.UserRepository;
import com.healthcare.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock JwtService jwtService;
    @Mock AuthenticationManager authenticationManager;
    @Mock UserDetailsService userDetailsService;

    @InjectMocks AuthService authService;

    private User testUser;
    private UserDetails mockUserDetails;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .name("Test User")
                .email("test@example.com")
                .password("encoded-password")
                .role(User.Role.USER)
                .language("en")
                .isActive(true)
                .build();

        mockUserDetails = org.springframework.security.core.userdetails.User.builder()
                .username("test@example.com")
                .password("encoded-password")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("register() — success: returns auth response with tokens")
    void register_success() {
        var request = new AuthRequest.RegisterRequest("Test User", "test@example.com", "Password1", "en");
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-password");
        when(userRepository.save(any())).thenReturn(testUser);
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(86400000L);
        doNothing().when(userRepository).updateRefreshToken(any(), any());

        ApiResponse.AuthResponse result = authService.register(request);

        assertThat(result).isNotNull();
        assertThat(result.accessToken()).isEqualTo("access-token");
        assertThat(result.refreshToken()).isEqualTo("refresh-token");
        assertThat(result.user().email()).isEqualTo("test@example.com");
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("register() — throws DuplicateResourceException if email exists")
    void register_emailAlreadyExists() {
        var request = new AuthRequest.RegisterRequest("Test User", "test@example.com", "Password1", "en");
        when(userRepository.existsByEmail("test@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(DuplicateResourceException.class)
                .hasMessageContaining("already exists");
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("login() — success: authenticates and returns tokens")
    void login_success() {
        var request = new AuthRequest.LoginRequest("test@example.com", "Password1");
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class))).thenReturn(null);
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userDetailsService.loadUserByUsername(anyString())).thenReturn(mockUserDetails);
        when(jwtService.generateAccessToken(any())).thenReturn("access-token");
        when(jwtService.generateRefreshToken(any())).thenReturn("refresh-token");
        when(jwtService.getAccessTokenExpiration()).thenReturn(86400000L);
        doNothing().when(userRepository).updateRefreshToken(any(), any());

        ApiResponse.AuthResponse result = authService.login(request);

        assertThat(result.accessToken()).isEqualTo("access-token");
    }

    @Test
    @DisplayName("login() — throws BadCredentialsException on wrong password")
    void login_wrongPassword() {
        var request = new AuthRequest.LoginRequest("test@example.com", "wrong");
        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(BadCredentialsException.class);
    }

    @Test
    @DisplayName("getCurrentUser() — returns user response for valid email")
    void getCurrentUser_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        ApiResponse.UserResponse result = authService.getCurrentUser("test@example.com");

        assertThat(result.email()).isEqualTo("test@example.com");
        assertThat(result.name()).isEqualTo("Test User");
        assertThat(result.role()).isEqualTo(User.Role.USER);
    }

    @Test
    @DisplayName("getCurrentUser() — throws ResourceNotFoundException for unknown email")
    void getCurrentUser_notFound() {
        when(userRepository.findByEmail("unknown@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getCurrentUser("unknown@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("updateProfile() — updates name and language")
    void updateProfile_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        var request = new AuthRequest.UpdateProfileRequest("New Name", "hi");
        ApiResponse.UserResponse result = authService.updateProfile("test@example.com", request);

        assertThat(result).isNotNull();
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("changePassword() — throws BadCredentialsException for wrong current password")
    void changePassword_wrongCurrentPassword() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(passwordEncoder.matches("wrongPass", "encoded-password")).thenReturn(false);

        var request = new AuthRequest.ChangePasswordRequest("wrongPass", "NewPass1");
        assertThatThrownBy(() -> authService.changePassword("test@example.com", request))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    @DisplayName("logout() — calls clearRefreshToken")
    void logout_success() {
        doNothing().when(userRepository).clearRefreshToken("test@example.com");

        authService.logout("test@example.com");

        verify(userRepository).clearRefreshToken("test@example.com");
    }

    @Test
    @DisplayName("deactivateAccount() — sets isActive to false")
    void deactivateAccount_success() {
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(userRepository.save(any())).thenReturn(testUser);

        authService.deactivateAccount("test@example.com");

        verify(userRepository).save(argThat(u -> !u.getIsActive()));
    }
}
