package com.healthcare.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;

@DisplayName("JwtService Unit Tests")
class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    private static final String TEST_SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", TEST_SECRET);
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration",  86400000L);
        ReflectionTestUtils.setField(jwtService, "refreshTokenExpiration", 604800000L);

        userDetails = User.builder()
                .username("test@example.com")
                .password("password")
                .roles("USER")
                .build();
    }

    @Test
    @DisplayName("generateAccessToken() — produces non-null token")
    void generateAccessToken() {
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("generateRefreshToken() — produces non-null token")
    void generateRefreshToken() {
        String token = jwtService.generateRefreshToken(userDetails);
        assertThat(token).isNotBlank();
    }

    @Test
    @DisplayName("extractUsername() — returns correct email from token")
    void extractUsername() {
        String token = jwtService.generateAccessToken(userDetails);
        String username = jwtService.extractUsername(token);
        assertThat(username).isEqualTo("test@example.com");
    }

    @Test
    @DisplayName("isTokenValid() — valid token returns true")
    void isTokenValid_valid() {
        String token = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid() — tampered token returns false")
    void isTokenValid_tampered() {
        String token = jwtService.generateAccessToken(userDetails) + "tampered";
        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() — expired token returns false")
    void isTokenValid_expired() {
        ReflectionTestUtils.setField(jwtService, "accessTokenExpiration", -1000L);
        String expiredToken = jwtService.generateAccessToken(userDetails);
        assertThat(jwtService.isTokenValid(expiredToken, userDetails)).isFalse();
    }

    @Test
    @DisplayName("isTokenValid() — wrong user returns false")
    void isTokenValid_wrongUser() {
        String token = jwtService.generateAccessToken(userDetails);
        UserDetails otherUser = User.builder()
                .username("other@example.com")
                .password("pass")
                .roles("USER")
                .build();
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }

    @Test
    @DisplayName("getAccessTokenExpiration() — returns configured value")
    void getAccessTokenExpiration() {
        assertThat(jwtService.getAccessTokenExpiration()).isEqualTo(86400000L);
    }
}
