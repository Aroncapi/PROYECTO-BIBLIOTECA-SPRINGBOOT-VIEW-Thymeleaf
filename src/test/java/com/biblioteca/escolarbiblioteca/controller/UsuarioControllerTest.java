package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.config.SecurityConfig;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.security.JwtAuthenticationFilter;
import com.biblioteca.escolarbiblioteca.security.JwtUtils;
import com.biblioteca.escolarbiblioteca.security.UserDetailsImpl;
import com.biblioteca.escolarbiblioteca.security.UserDetailsServiceImpl;
import com.biblioteca.escolarbiblioteca.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas funcionales del API de perfil de usuario (/api/usuarios/**).
 */
@WebMvcTest(controllers = UsuarioController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {UsuarioController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, com.biblioteca.escolarbiblioteca.exception.GlobalExceptionHandler.class})
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    private Usuario alumno() {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Torres")
                .email("ana@test.com")
                .password("$2a$oldHash")
                .rol(new Rol(2L, Rol.ERol.ALUMNO))
                .nivel(Usuario.Nivel.PRIMARIA)
                .grado(3)
                .seccion("A")
                .build();
    }

    @Test
    void getPerfil_devuelveDatosDelUsuario() throws Exception {
        mockMvc.perform(get("/api/usuarios/perfil").with(user(UserDetailsImpl.build(alumno()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("ana@test.com"))
                .andExpect(jsonPath("$.rol").value("ALUMNO"));
    }

    @Test
    void actualizarPerfil_mismoEmail_devuelve200() throws Exception {
        when(usuarioService.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/usuarios/perfil")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana Maria\",\"apellido\":\"Torres\","
                                + "\"email\":\"ana@test.com\",\"rol\":\"ALUMNO\","
                                + "\"nivel\":\"PRIMARIA\",\"grado\":3,\"seccion\":\"A\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"))
                .andExpect(jsonPath("$.usuario.nombre").value("Ana Maria"));
    }

    @Test
    void actualizarPerfil_emailYaRegistrado_devuelve400() throws Exception {
        when(usuarioService.existsByEmail("otro@test.com")).thenReturn(true);

        mockMvc.perform(put("/api/usuarios/perfil")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Torres\","
                                + "\"email\":\"otro@test.com\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("El email ya está registrado por otro usuario"));
    }

    @Test
    void actualizarPerfil_conPasswordEncriptaNuevaHash() throws Exception {
        when(passwordEncoder.encode("nueva123")).thenReturn("$2a$newHash");
        when(usuarioService.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(put("/api/usuarios/perfil")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Ana\",\"apellido\":\"Torres\","
                                + "\"email\":\"ana@test.com\",\"password\":\"nueva123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Perfil actualizado correctamente"));
    }

    @Test
    void cambiarPassword_correcto_devuelve200() throws Exception {
        when(passwordEncoder.matches("actual123", "$2a$oldHash")).thenReturn(true);
        when(passwordEncoder.encode("nueva123")).thenReturn("$2a$newHash");
        when(usuarioService.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        mockMvc.perform(post("/api/usuarios/cambiar-password")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"actual123\",\"passwordNueva\":\"nueva123\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Contraseña cambiada correctamente"));
    }

    @Test
    void cambiarPassword_actualIncorrecto_devuelve400() throws Exception {
        when(passwordEncoder.matches("mala", "$2a$oldHash")).thenReturn(false);

        mockMvc.perform(post("/api/usuarios/cambiar-password")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"passwordActual\":\"mala\",\"passwordNueva\":\"nueva123\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("La contraseña actual es incorrecta"));
    }

    @Test
    void cambiarPassword_camposFaltantes_devuelve400() throws Exception {
        mockMvc.perform(post("/api/usuarios/cambiar-password")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Debe proporcionar contraseña actual y nueva"));
    }

    @Test
    void checkEmail_devuelveDisponibilidad() throws Exception {
        when(usuarioService.existsByEmail("libre@test.com")).thenReturn(false);

        mockMvc.perform(get("/api/usuarios/check-email")
                        .with(user(UserDetailsImpl.build(alumno())))
                        .param("email", "libre@test.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.disponible").value(true));
    }

    @Test
    void health_devuelveOK() throws Exception {
        mockMvc.perform(get("/api/usuarios/health").with(user(UserDetailsImpl.build(alumno()))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }
}
