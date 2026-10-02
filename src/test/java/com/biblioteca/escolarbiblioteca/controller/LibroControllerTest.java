package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.config.SecurityConfig;
import com.biblioteca.escolarbiblioteca.dto.LibroDTO;
import com.biblioteca.escolarbiblioteca.exception.ResourceNotFoundException;
import com.biblioteca.escolarbiblioteca.security.JwtAuthenticationFilter;
import com.biblioteca.escolarbiblioteca.security.JwtUtils;
import com.biblioteca.escolarbiblioteca.security.UserDetailsServiceImpl;
import com.biblioteca.escolarbiblioteca.service.LibroService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas funcionales del catálogo de libros + control de acceso por roles.
 */
@WebMvcTest(controllers = LibroController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {LibroController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, com.biblioteca.escolarbiblioteca.exception.GlobalExceptionHandler.class})
class LibroControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LibroService libroService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    private LibroDTO libroDto() {
        LibroDTO dto = new LibroDTO();
        dto.setId(1L);
        dto.setNombre("Don Quijote de la Mancha");
        dto.setCategoria("Novela");
        dto.setCodigoLibro("LIB-001");
        dto.setStockTotal(5);
        dto.setStockDisponible(4);
        return dto;
    }

    @Test
    @WithMockUser(username = "alumno@biblioteca.com", roles = "ALUMNO")
    void getLibros_autenticado_devuelveLista() throws Exception {
        when(libroService.findAll()).thenReturn(List.of(libroDto()));

        mockMvc.perform(get("/api/libros"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].nombre").value("Don Quijote de la Mancha"))
                .andExpect(jsonPath("$[0].codigoLibro").value("LIB-001"));
    }

    @Test
    void getLibros_sinSesion_noDevuelve200() throws Exception {
        when(libroService.findAll()).thenReturn(List.of(libroDto()));

        mockMvc.perform(get("/api/libros"))
                .andExpect(result -> {
                    int status = result.getResponse().getStatus();
                    org.junit.jupiter.api.Assertions.assertTrue(status == 302 || status == 401,
                            "Se esperaba rechazo (302/401) pero llegó " + status);
                });
    }

    @Test
    @WithMockUser(username = "alumno@biblioteca.com", roles = "ALUMNO")
    void postLibros_comoAlumno_devuelve403() throws Exception {
        mockMvc.perform(post("/api/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Libro Nuevo\",\"categoria\":\"Novela\","
                                + "\"codigoLibro\":\"LIB-999\",\"stockTotal\":3}"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "admin@biblioteca.com", roles = "ADMIN")
    void postLibros_comoAdmin_devuelve201() throws Exception {
        when(libroService.save(any(LibroDTO.class))).thenAnswer(inv -> {
            LibroDTO dto = inv.getArgument(0);
            dto.setId(9L);
            return dto;
        });

        mockMvc.perform(post("/api/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Libro Nuevo\",\"categoria\":\"Novela\","
                                + "\"codigoLibro\":\"LIB-999\",\"stockTotal\":3}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(9))
                .andExpect(jsonPath("$.codigoLibro").value("LIB-999"));
    }

    @Test
    @WithMockUser(username = "admin@biblioteca.com", roles = "ADMIN")
    void postLibros_conflictoOptimista_devuelve409() throws Exception {
        when(libroService.save(any(LibroDTO.class)))
                .thenThrow(new org.springframework.dao.OptimisticLockingFailureException("conflicto"));

        mockMvc.perform(post("/api/libros")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Libro Nuevo\",\"categoria\":\"Novela\","
                                + "\"codigoLibro\":\"LIB-999\",\"stockTotal\":3}"))
                .andExpect(status().isConflict());
    }

    @Test
    @WithMockUser(username = "alumno@biblioteca.com", roles = "ALUMNO")
    void getLibroInexistente_devuelve404() throws Exception {
        when(libroService.findById(99L)).thenThrow(new ResourceNotFoundException("Libro no encontrado con id: 99"));

        mockMvc.perform(get("/api/libros/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message").value("Libro no encontrado con id: 99"));
    }
}
