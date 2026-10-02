package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.config.SecurityConfig;
import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
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
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

/**
 * Pruebas funcionales del controlador de vistas (páginas HTML del servidor).
 */
@WebMvcTest(controllers = ViewController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ViewController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, com.biblioteca.escolarbiblioteca.exception.GlobalExceptionHandler.class})
class ViewControllerTest {

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
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    private Usuario usuario(Rol.ERol rol) {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Torres")
                .email("ana@test.com")
                .password("$2a$hash")
                .rol(new Rol(rol == Rol.ERol.ADMIN ? 1L : 2L, rol))
                .nivel(rol == Rol.ERol.ALUMNO ? Usuario.Nivel.PRIMARIA : null)
                .grado(rol == Rol.ERol.ALUMNO ? 3 : null)
                .seccion(rol == Rol.ERol.ALUMNO ? "A" : null)
                .build();
    }

    private PrestamoDTO prestamo(String estado) {
        PrestamoDTO dto = new PrestamoDTO();
        dto.setId(1L);
        dto.setEstado(estado);
        return dto;
    }

    // ---------- Página principal y login ----------

    @Test
    void home_sinSesion_redirigeALogin() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void home_comoAdmin_redirigeAlDashboard() throws Exception {
        mockMvc.perform(get("/").with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void home_comoAlumno_redirigeAlCatalogo() throws Exception {
        mockMvc.perform(get("/").with(user(UserDetailsImpl.build(usuario(Rol.ERol.ALUMNO)))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/alumno/catalogo"));
    }

    @Test
    void login_sinSesion_muestraVistaLogin() throws Exception {
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    void login_comoAdmin_redirigeAlDashboard() throws Exception {
        mockMvc.perform(get("/login").with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void registro_muestraVistaRegistro() throws Exception {
        mockMvc.perform(get("/registro"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"));
    }

    // ---------- Páginas de administrador ----------

    @Test
    void dashboard_admin_muestraVista() throws Exception {
        when(reporteService.getDashboardStats()).thenReturn(Map.of(
                "totalLibros", 10, "totalAlumnos", 5,
                "prestamosActivos", 3, "prestamosVencidos", 1, "topLibros", List.of()));
        when(prestamoService.getPrestamosActivos()).thenReturn(List.of(prestamo("ACTIVO")));

        mockMvc.perform(get("/admin/dashboard")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/dashboard"));
    }

    @Test
    void usuarios_admin_muestraVista() throws Exception {
        when(usuarioService.findAll()).thenReturn(List.of(
                usuario(Rol.ERol.ADMIN), usuario(Rol.ERol.ALUMNO)));

        mockMvc.perform(get("/admin/usuarios")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/usuarios"));
    }

    @Test
    void libros_admin_muestraVista() throws Exception {
        when(libroService.findAll()).thenReturn(List.of());

        mockMvc.perform(get("/admin/libros")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/libros"));
    }

    @Test
    void prestamos_admin_muestraVista() throws Exception {
        when(prestamoService.getAllPrestamos())
                .thenReturn(List.of(prestamo("DEVUELTO"), prestamo("ACTIVO")));
        when(prestamoService.getPrestamosActivos()).thenReturn(List.of(prestamo("ACTIVO")));
        when(prestamoService.getPrestamosVencidos()).thenReturn(List.of(prestamo("VENCIDO")));

        mockMvc.perform(get("/admin/prestamos")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/prestamos"));
    }

    @Test
    void reportes_admin_muestraVista() throws Exception {
        mockMvc.perform(get("/admin/reportes")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/reportes"));
    }

    @Test
    void perfil_admin_muestraVista() throws Exception {
        mockMvc.perform(get("/admin/perfil")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ADMIN)))))
                .andExpect(status().isOk())
                .andExpect(view().name("admin/perfil"));
    }

    // ---------- Páginas de alumno ----------

    @Test
    void catalogo_alumno_muestraVista() throws Exception {
        when(libroService.findAll()).thenReturn(List.of());
        when(prestamoService.getPrestamosActivosByUsuario(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(prestamo("ACTIVO")));

        mockMvc.perform(get("/alumno/catalogo")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ALUMNO)))))
                .andExpect(status().isOk())
                .andExpect(view().name("alumno/catalogo"));
    }

    @Test
    void misPrestamos_alumno_muestraVista() throws Exception {
        when(prestamoService.getPrestamosByUsuario(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(prestamo("ACTIVO"), prestamo("DEVUELTO"), prestamo("VENCIDO")));
        when(prestamoService.getPrestamosActivosByUsuario(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(prestamo("ACTIVO")));

        mockMvc.perform(get("/alumno/mis-prestamos")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ALUMNO)))))
                .andExpect(status().isOk())
                .andExpect(view().name("alumno/mis-prestamos"));
    }

    @Test
    void perfil_alumno_muestraVista() throws Exception {
        when(prestamoService.getPrestamosByUsuario(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(prestamo("DEVUELTO"), prestamo("ACTIVO")));
        when(prestamoService.getPrestamosActivosByUsuario(org.mockito.ArgumentMatchers.any()))
                .thenReturn(List.of(prestamo("ACTIVO")));

        mockMvc.perform(get("/alumno/perfil")
                        .with(user(UserDetailsImpl.build(usuario(Rol.ERol.ALUMNO)))))
                .andExpect(status().isOk())
                .andExpect(view().name("alumno/perfil"));
    }

    // ---------- Páginas de error ----------

    @Test
    @WithMockUser
    void error404_muestraVista() throws Exception {
        mockMvc.perform(get("/error/404"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/404"));
    }

    @Test
    @WithMockUser
    void error403_muestraVista() throws Exception {
        mockMvc.perform(get("/error/403"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/403"));
    }

    @Test
    @WithMockUser
    void error500_muestraVista() throws Exception {
        mockMvc.perform(get("/error/500"))
                .andExpect(status().isOk())
                .andExpect(view().name("error/500"));
    }
}
