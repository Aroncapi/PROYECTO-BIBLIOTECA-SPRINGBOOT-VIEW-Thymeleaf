package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.config.SecurityConfig;
import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.security.JwtAuthenticationFilter;
import com.biblioteca.escolarbiblioteca.security.JwtUtils;
import com.biblioteca.escolarbiblioteca.security.UserDetailsServiceImpl;
import com.biblioteca.escolarbiblioteca.service.ExportService;
import com.biblioteca.escolarbiblioteca.service.PrestamoService;
import com.biblioteca.escolarbiblioteca.service.ReporteService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Pruebas funcionales de exportación de reportes (PDF/Excel/CSV).
 */
@WebMvcTest(controllers = ReporteExportController.class)
@AutoConfigureMockMvc
@ContextConfiguration(classes = {ReporteExportController.class, SecurityConfig.class,
        JwtAuthenticationFilter.class, com.biblioteca.escolarbiblioteca.exception.GlobalExceptionHandler.class})
class ReporteExportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private PrestamoService prestamoService;

    @MockBean
    private ReporteService reporteService;

    @MockBean
    private ExportService exportService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @MockBean
    private JwtUtils jwtUtils;

    private PrestamoDTO prestamo() {
        PrestamoDTO dto = new PrestamoDTO();
        dto.setId(1L);
        dto.setUsuarioNombre("Ana Torres");
        dto.setUsuarioEmail("ana@test.com");
        dto.setLibroNombre("Don Quijote");
        dto.setLibroCodigo("LIB-001");
        dto.setFechaPrestamo(LocalDateTime.now());
        dto.setFechaDevolucionEstimada(LocalDateTime.now().plusDays(7));
        dto.setEstado("ACTIVO");
        dto.setDiasRestantes(7);
        return dto;
    }

    private void stubExport() throws Exception {
        when(exportService.exportarPdf(anyString(), any(String[].class), anyList()))
                .thenReturn("%PDF-1.4".getBytes());
        when(exportService.exportarExcel(anyString(), any(String[].class), anyList()))
                .thenReturn(new byte[]{1, 2, 3});
        when(exportService.exportarCsv(any(String[].class), anyList()))
                .thenReturn("col1,col2".getBytes());
        when(exportService.nombreArchivo(anyString(), anyString()))
                .thenReturn("reporte.pdf");
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_prestamosActivos_pdf_devuelve200() throws Exception {
        stubExport();
        when(prestamoService.getPrestamosActivos()).thenReturn(List.of(prestamo()));

        mockMvc.perform(get("/api/admin/reportes/prestamos-activos/export/pdf"))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Disposition",
                        org.hamcrest.Matchers.containsString("attachment")))
                .andExpect(header().string("Content-Type", "application/pdf"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_prestamosVencidos_xlsx_devuelve200() throws Exception {
        stubExport();
        when(prestamoService.getPrestamosVencidos()).thenReturn(List.of(prestamo()));

        mockMvc.perform(get("/api/admin/reportes/prestamos-vencidos/export/xlsx"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_historial_sinFechas_devuelve400() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/historial/export/csv"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value(
                        "Los parámetros fechaInicio y fechaFin son obligatorios (yyyy-MM-dd)"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_historial_conFechas_devuelve200() throws Exception {
        stubExport();
        when(reporteService.getPrestamosPorRangoFechas("2026-01-01", "2026-01-31"))
                .thenReturn(List.of(prestamo()));

        mockMvc.perform(get("/api/admin/reportes/historial/export/csv")
                        .param("fechaInicio", "2026-01-01")
                        .param("fechaFin", "2026-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_topLibros_csv_devuelve200() throws Exception {
        stubExport();
        when(reporteService.getTopLibrosPrestados()).thenReturn(List.of(
                Map.<String, Object>of("libroNombre", "Don Quijote", "libroCodigo", "LIB-001",
                        "categoria", "Novela", "totalPrestamos", 5)));

        mockMvc.perform(get("/api/admin/reportes/top-libros/export/csv"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_usuariosActivos_csv_devuelve200() throws Exception {
        stubExport();
        when(reporteService.getTopUsuariosActivos(anyInt())).thenReturn(List.of(
                Map.<String, Object>of("usuario", "Ana Torres", "email", "ana@test.com",
                        "totalPrestamos", 4)));

        mockMvc.perform(get("/api/admin/reportes/usuarios-activos/export/csv")
                        .param("limite", "5"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_estadisticasCategorias_pdf_devuelve200() throws Exception {
        stubExport();
        Map<String, Long> libros = Map.of("Novela", 3L);
        Map<String, Long> prestamos = Map.of("Novela", 10L);
        when(reporteService.getEstadisticasCategorias())
                .thenReturn(Map.of("librosPorCategoria", libros, "prestamosPorCategoria", prestamos));

        mockMvc.perform(get("/api/admin/reportes/estadisticas-categorias/export/pdf"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_tipoInvalido_devuelve400() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/tipo-inexistente/export/pdf"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error")
                        .value("Tipo de reporte inválido: tipo-inexistente"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_formatoInvalido_devuelve400() throws Exception {
        when(prestamoService.getPrestamosActivos()).thenReturn(List.of(prestamo()));

        mockMvc.perform(get("/api/admin/reportes/prestamos-activos/export/doc"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.error").value("Formato inválido: doc"));
    }

    @Test
    @WithMockUser(username = "admin@test.com", roles = "ADMIN")
    void exportar_errorDelServicio_devuelve500() throws Exception {
        when(prestamoService.getPrestamosActivos())
                .thenThrow(new RuntimeException("fallo de base de datos"));

        mockMvc.perform(get("/api/admin/reportes/prestamos-activos/export/pdf"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error")
                        .value("Error al generar el reporte: fallo de base de datos"));
    }

    @Test
    @WithMockUser(username = "alumno@test.com", roles = "ALUMNO")
    void exportar_comoAlumno_devuelve403() throws Exception {
        mockMvc.perform(get("/api/admin/reportes/prestamos-activos/export/pdf"))
                .andExpect(status().isForbidden());
    }
}
