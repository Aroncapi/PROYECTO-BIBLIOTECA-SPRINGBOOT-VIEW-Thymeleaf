package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.config.SecurityConfig;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.RolRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import com.biblioteca.escolarbiblioteca.security.JwtAuthenticationFilter;
import com.biblioteca.escolarbiblioteca.security.JwtUtils;
import com.biblioteca.escolarbiblioteca.security.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas funcionales del API de autenticación (login, registro y check-email).
 */
@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {AuthController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, com.biblioteca.escolarbiblioteca.exception.GlobalExceptionHandler.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthenticationManager authenticationManager;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private RolRepository rolRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private JwtUtils jwtUtils;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    private Usuario alumno() {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Torres")
                .email("ana@biblioteca.com")
                .password("$2a$hash")
                .rol(new Rol(2L, Rol.ERol.ALUMNO))
                .nivel(Usuario.Nivel.PRIMARIA)
                .grado(3)
                .seccion("A")
                .build();
    }

    @Test
    void login_correcto_devuelveTokenYDatosDeUsuario() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenReturn(mock(UsernamePasswordAuthenticationToken.class));
        when(usuarioRepository.findByEmail("ana@biblioteca.com")).thenReturn(Optional.of(alumno()));
        when(jwtUtils.generateJwtToken(any())).thenReturn("jwt-de-ejemplo");

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@biblioteca.com\",\"password\":\"123456\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt-de-ejemplo"))
                .andExpect(jsonPath("$.type").value("Bearer"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.email").value("ana@biblioteca.com"))
                .andExpect(jsonPath("$.rol").value("ALUMNO"));
    }

    @Test
    void login_conPasswordIncorrecto_devuelve401() throws Exception {
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("bad"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"ana@biblioteca.com\",\"password\":\"mala\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Credenciales inválidas"));
    }

    @Test
    void login_conEmailInvalido_devuelve400() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"no-es-email\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void register_alumnoValido_devuelve201() throws Exception {
        when(usuarioRepository.existsByEmail("nuevo@biblioteca.com")).thenReturn(false);
        when(rolRepository.findByNombre(Rol.ERol.ALUMNO)).thenReturn(Optional.of(new Rol(2L, Rol.ERol.ALUMNO)));
        when(passwordEncoder.encode(anyString())).thenReturn("$2a$hash-nuevo");
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> {
            Usuario u = inv.getArgument(0);
            u.setId(10L);
            return u;
        });

        mockMvc.perform(post("/api/auth/register/alumno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luis\",\"apellido\":\"García\","
                                + "\"email\":\"nuevo@biblioteca.com\",\"password\":\"123456\","
                                + "\"nivel\":\"PRIMARIA\",\"grado\":2,\"seccion\":\"B\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10))
                .andExpect(jsonPath("$.rol").value("ALUMNO"));
    }

    @Test
    void register_alumnoSinNivel_devuelve400() throws Exception {
        when(usuarioRepository.existsByEmail("nuevo@biblioteca.com")).thenReturn(false);

        mockMvc.perform(post("/api/auth/register/alumno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Luis\",\"apellido\":\"García\","
                                + "\"email\":\"nuevo@biblioteca.com\",\"password\":\"123456\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Los campos nivel, grado y sección son obligatorios para alumnos"));
    }

    @Test
    void register_emailYaRegistrado_devuelve400() throws Exception {
        when(usuarioRepository.existsByEmail("ana@biblioteca.com")).thenReturn(true);

        mockMvc.perform(post("/api/auth/register/alumno")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Torres\","
                                + "\"email\":\"ana@biblioteca.com\",\"password\":\"123456\","
                                + "\"nivel\":\"PRIMARIA\",\"grado\":3,\"seccion\":\"A\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está registrado"));
    }

    @Test
    void checkEmail_disponible_devuelveTrue() throws Exception {
        when(usuarioRepository.existsByEmail("libre@biblioteca.com")).thenReturn(false);

        mockMvc.perform(get("/api/auth/check-email").param("email", "libre@biblioteca.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void checkEmail_yaRegistrado_devuelveFalse() throws Exception {
        when(usuarioRepository.existsByEmail("ana@biblioteca.com")).thenReturn(true);

        mockMvc.perform(get("/api/auth/check-email").param("email", "ana@biblioteca.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(false));
    }
}
