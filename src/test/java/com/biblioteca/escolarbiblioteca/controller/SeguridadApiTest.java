package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.config.SecurityConfig;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.RolRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import com.biblioteca.escolarbiblioteca.security.JwtAuthenticationFilter;
import com.biblioteca.escolarbiblioteca.security.JwtUtils;
import com.biblioteca.escolarbiblioteca.security.UserDetailsImpl;
import com.biblioteca.escolarbiblioteca.security.UserDetailsServiceImpl;
import com.biblioteca.escolarbiblioteca.service.LibroService;
import com.biblioteca.escolarbiblioteca.service.PrestamoService;
import com.biblioteca.escolarbiblioteca.service.ReporteService;
import com.biblioteca.escolarbiblioteca.service.UsuarioService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;

/**
 * Pruebas de seguridad: verificación de roles en los endpoints.
 * Requisito matriz CIBERTEC 2: seguridad de la información.
 */
@WebMvcTest(controllers = {AdminController.class, AlumnoController.class})
@AutoConfigureMockMvc
@ContextConfiguration(classes = {AdminController.class, AlumnoController.class,
        SecurityConfig.class, JwtAuthenticationFilter.class,
        com.biblioteca.escolarbiblioteca.exception.GlobalExceptionHandler.class})
class SeguridadApiTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UsuarioService usuarioService;

    @MockBean
    private LibroService libroService;

    @MockBean
    private PrestamoService prestamoService;

    @MockBean
    private ReporteService reporteService;

    @MockBean
    private UsuarioRepository usuarioRepository;

    @MockBean
    private RolRepository rolRepository;

    @MockBean
    private PasswordEncoder passwordEncoder;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    // ---------- /api/admin/** ----------

    @Test
    void adminHealth_sinSesion_noDevuelve200() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 302 || status == 401,
                            "Se esperaba rechazo (302/401) pero llegó " + status);
                });
    }

    @Test
    @WithMockUser(username = "alumno@biblioteca.com", roles = "ALUMNO")
    void adminHealth_comoAlumno_devuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@biblioteca.com", roles = "ADMIN")
    void adminHealth_comoAdmin_devuelve200() throws Exception {
        mockMvc.perform(get("/api/admin/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OK"));
    }

    @Test
    @WithMockUser(username = "admin@biblioteca.com", roles = "ADMIN")
    void adminDashboard_comoAdmin_devuelve200() throws Exception {
        when(reporteService.getDashboardStats()).thenReturn(new java.util.HashMap<>());

        mockMvc.perform(get("/api/admin/dashboard"))
                .andExpect(status().isOk());
    }

    // ---------- /api/alumno/** ----------

    @Test
    @WithMockUser(username = "admin@biblioteca.com", roles = "ADMIN")
    void misPrestamos_comoAdmin_devuelve403() throws Exception {
        mockMvc.perform(get("/api/alumno/mis-prestamos"))
                .andExpect(status().isForbidden());
    }

    @Test
    void misPrestamos_sinSesion_noDevuelve200() throws Exception {
        mockMvc.perform(get("/api/alumno/mis-prestamos"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 302 || status == 401,
                            "Se esperaba rechazo (302/401) pero llegó " + status);
                });
    }

    @Test
    void misPrestamos_comoAlumno_devuelve200() throws Exception {
        Usuario alumno = Usuario.builder()
                .id(1L).nombre("Ana").apellido("Torres")
                .email("alumno@biblioteca.com")
                .rol(new Rol(2L, Rol.ERol.ALUMNO))
                .build();
        UserDetailsImpl userDetails = UserDetailsImpl.build(alumno);
        when(prestamoService.getPrestamosByUsuario(alumno)).thenReturn(List.of());

        mockMvc.perform(get("/api/alumno/mis-prestamos").with(user(userDetails)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    // ---------- Ruta inexistente ----------

    @Test
    @WithMockUser(username = "admin@biblioteca.com", roles = "ADMIN")
    void rutaInexistente_devuelve404() throws Exception {
        mockMvc.perform(get("/api/ruta-que-no-existe"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
