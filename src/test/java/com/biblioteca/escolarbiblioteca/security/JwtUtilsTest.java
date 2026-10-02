package com.biblioteca.escolarbiblioteca.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtUtilsTest {

    private JwtUtils jwtUtils;

    @BeforeEach
    void setUp() {
        jwtUtils = new JwtUtils();
        ReflectionTestUtils.setField(jwtUtils, "jwtSecret",
                "testSecretKeyForJwtTestingWithEnoughLength32Chars");
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", 3600000);
    }

    private Authentication authentication() {
        UserDetails user = User.withUsername("ana@biblioteca.com")
                .password("$2a$hash")
                .authorities("ROLE_ALUMNO")
                .build();
        return new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities());
    }

    @Test
    void generateJwtToken_contieneElEmailComoSubject() {
        String token = jwtUtils.generateJwtToken(authentication());

        assertNotNull(token);
        assertFalse(token.isBlank());
        assertEquals("ana@biblioteca.com", jwtUtils.getUserNameFromJwtToken(token));
    }

    @Test
    void validateJwtToken_tokenFirmado_devuelveTrue() {
        String token = jwtUtils.generateJwtToken(authentication());

        assertTrue(jwtUtils.validateJwtToken(token));
    }

    @Test
    void validateJwtToken_tokenMalformado_devuelveFalse() {
        assertFalse(jwtUtils.validateJwtToken("esto-no-es-un-jwt"));
    }

    @Test
    void validateJwtToken_tokenVacio_devuelveFalse() {
        assertFalse(jwtUtils.validateJwtToken(""));
    }

    @Test
    void validateJwtToken_tokenExpirado_devuelveFalse() {
        ReflectionTestUtils.setField(jwtUtils, "jwtExpirationMs", -60000);
        String token = jwtUtils.generateJwtToken(authentication());

        assertFalse(jwtUtils.validateJwtToken(token));
    }
}
