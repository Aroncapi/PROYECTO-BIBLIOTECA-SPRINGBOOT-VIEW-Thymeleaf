package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.service.ExportService;
import com.biblioteca.escolarbiblioteca.service.PrestamoService;
import com.biblioteca.escolarbiblioteca.service.ReporteService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

/**
 * Exportación de reportes a diferentes formatos: PDF, Excel (.xlsx) y CSV.
 * Ruta: GET /api/admin/reportes/{tipo}/export/{formato}
 * Tipos: prestamos-activos, prestamos-vencidos, historial, top-libros,
 *        usuarios-activos, estadisticas-categorias
 * Formatos: pdf, xlsx, csv
 */
@RestController
@RequestMapping("/api/admin/reportes")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class ReporteExportController {

    private static final String[] COLUMNAS_PRESTAMO = {
            "ID", "Usuario", "Email", "Libro", "Código",
            "F. Préstamo", "F. Devolución Estimada", "F. Devolución Real",
            "Estado", "Días Restantes", "Días Retraso"
    };

    private final PrestamoService prestamoService;
    private final ReporteService reporteService;
    private final ExportService exportService;

    @GetMapping("/{tipo}/export/{formato}")
    public ResponseEntity<?> exportar(
            @PathVariable String tipo,
            @PathVariable String formato,
            @RequestParam(required = false) String fechaInicio,
            @RequestParam(required = false) String fechaFin,
            @RequestParam(name = "limite", defaultValue = "10") int limite) {

        log.info("Exportando reporte '{}' en formato '{}'", tipo, formato);

        // 1. Construir los datos del reporte
        String titulo;
        String nombreHoja;
        String[] columnas;
        List<String[]> filas;
        try {
            switch (tipo) {
                case "prestamos-activos":
                    titulo = "Reporte de Préstamos Activos";
                    columnas = COLUMNAS_PRESTAMO;
                    filas = filasDesdePrestamos(prestamoService.getPrestamosActivos());
                    break;
                case "prestamos-vencidos":
                    titulo = "Reporte de Préstamos Vencidos";
                    columnas = COLUMNAS_PRESTAMO;
                    filas = filasDesdePrestamos(prestamoService.getPrestamosVencidos());
                    break;
                case "historial":
                    if (fechaInicio == null || fechaFin == null) {
                        return ResponseEntity.badRequest()
                                .body(Map.of("error", "Los parámetros fechaInicio y fechaFin son obligatorios (yyyy-MM-dd)"));
                    }
                    titulo = "Historial de Préstamos (" + fechaInicio + " al " + fechaFin + ")";
                    columnas = COLUMNAS_PRESTAMO;
                    filas = filasDesdePrestamos(
                            reporteService.getPrestamosPorRangoFechas(fechaInicio, fechaFin));
                    break;
                case "top-libros": {
                    titulo = "Top Libros Más Prestados";
                    columnas = new String[]{"#", "Libro", "Código", "Categoría", "Total Préstamos"};
                    filas = new ArrayList<>();
                    List<Map<String, Object>> top = reporteService.getTopLibrosPrestados();
                    for (int i = 0; i < top.size(); i++) {
                        Map<String, Object> item = top.get(i);
                        filas.add(new String[]{
                                String.valueOf(i + 1),
                                str(item.get("libroNombre")),
                                str(item.get("libroCodigo")),
                                str(item.get("categoria")),
                                str(item.get("totalPrestamos"))
                        });
                    }
                    break;
                }
                case "usuarios-activos": {
                    titulo = "Usuarios Más Activos";
                    columnas = new String[]{"#", "Usuario", "Email", "Total Préstamos"};
                    filas = new ArrayList<>();
                    List<Map<String, Object>> usuarios = reporteService.getTopUsuariosActivos(limite);
                    for (int i = 0; i < usuarios.size(); i++) {
                        Map<String, Object> item = usuarios.get(i);
                        filas.add(new String[]{
                                String.valueOf(i + 1),
                                str(item.get("usuario")),
                                str(item.get("email")),
                                str(item.get("totalPrestamos"))
                        });
                    }
                    break;
                }
                case "estadisticas-categorias": {
                    titulo = "Estadísticas por Categoría";
                    columnas = new String[]{"Categoría", "Libros", "Préstamos"};
                    filas = new ArrayList<>();
                    Map<String, Object> stats = reporteService.getEstadisticasCategorias();
                    @SuppressWarnings("unchecked")
                    Map<String, Long> librosPorCategoria = (Map<String, Long>) stats.get("librosPorCategoria");
                    @SuppressWarnings("unchecked")
                    Map<String, Long> prestamosPorCategoria = (Map<String, Long>) stats.get("prestamosPorCategoria");
                    TreeSet<String> categorias = new TreeSet<>();
                    categorias.addAll(librosPorCategoria.keySet());
                    categorias.addAll(prestamosPorCategoria.keySet());
                    for (String categoria : categorias) {
                        filas.add(new String[]{
                                categoria,
                                String.valueOf(librosPorCategoria.getOrDefault(categoria, 0L)),
                                String.valueOf(prestamosPorCategoria.getOrDefault(categoria, 0L))
                        });
                    }
                    break;
                }
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Tipo de reporte inválido: " + tipo,
                            "tiposValidos", List.of("prestamos-activos", "prestamos-vencidos",
                                    "historial", "top-libros", "usuarios-activos", "estadisticas-categorias")));
            }
        } catch (DateTimeParseException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Formato de fecha inválido. Use yyyy-MM-dd"));
        } catch (Exception e) {
            log.error("Error al construir reporte '{}': {}", tipo, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al generar el reporte: " + e.getMessage()));
        }

        nombreHoja = tipo.substring(0, Math.min(tipo.length(), 31));

        // 2. Exportar al formato solicitado
        byte[] contenido;
        String contentType;
        String extension;
        try {
            switch (formato.toLowerCase()) {
                case "pdf":
                    contenido = exportService.exportarPdf(titulo, columnas, filas);
                    contentType = "application/pdf";
                    extension = "pdf";
                    break;
                case "xlsx":
                    contenido = exportService.exportarExcel(nombreHoja, columnas, filas);
                    contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
                    extension = "xlsx";
                    break;
                case "csv":
                    contenido = exportService.exportarCsv(columnas, filas);
                    contentType = "text/csv";
                    extension = "csv";
                    break;
                default:
                    return ResponseEntity.badRequest().body(Map.of(
                            "error", "Formato inválido: " + formato,
                            "formatosValidos", List.of("pdf", "xlsx", "csv")));
            }
        } catch (Exception e) {
            log.error("Error al exportar '{}' a '{}': {}", tipo, formato, e.getMessage());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Error al exportar el archivo: " + e.getMessage()));
        }

        // 3. Responder como descarga
        String nombreArchivo = exportService.nombreArchivo(tipo, extension);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType(contentType))
                .body(contenido);
    }

    private List<String[]> filasDesdePrestamos(List<PrestamoDTO> prestamos) {
        List<String[]> filas = new ArrayList<>();
        for (PrestamoDTO p : prestamos) {
            filas.add(new String[]{
                    p.getId() != null ? String.valueOf(p.getId()) : "",
                    p.getUsuarioNombre(),
                    p.getUsuarioEmail(),
                    p.getLibroNombre(),
                    p.getLibroCodigo(),
                    p.getFechaPrestamoFormatted(),
                    p.getFechaDevolucionEstimadaFormatted(),
                    p.getFechaDevolucionRealFormatted(),
                    p.getEstado(),
                    p.getDiasRestantes() != null ? String.valueOf(p.getDiasRestantes()) : "",
                    p.getDiasRetraso() != null ? String.valueOf(p.getDiasRetraso()) : ""
            });
        }
        return filas;
    }

    private String str(Object valor) {
        return valor != null ? String.valueOf(valor) : "";
    }
}
