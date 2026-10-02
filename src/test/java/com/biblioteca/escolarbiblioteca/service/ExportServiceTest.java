package com.biblioteca.escolarbiblioteca.service;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de las exportaciones de reportes (competencia: reportes exportables
 * a diferentes formatos).
 */
class ExportServiceTest {

    private final ExportService exportService = new ExportService();

    private static final String[] COLUMNAS = {"Libro", "Préstamos"};
    private static final List<String[]> FILAS = List.of(
            new String[]{"Don Quijote", "15"},
            new String[]{"El principito", "9"}
    );

    @Test
    void exportarPdf_generaUnArchivoPdfValido() {
        byte[] pdf = exportService.exportarPdf("Top libros", COLUMNAS, FILAS);

        assertTrue(pdf.length > 0, "El PDF no debe estar vacío");
        String inicio = new String(pdf, 0, 4, StandardCharsets.ISO_8859_1);
        assertEquals("%PDF", inicio, "Un PDF debe empezar por %PDF");
    }

    @Test
    void exportarExcel_generaUnArchivoZipValido() {
        byte[] xlsx = exportService.exportarExcel("Reporte", COLUMNAS, FILAS);

        assertTrue(xlsx.length > 0, "El Excel no debe estar vacío");
        // Los .xlsx son archivos ZIP: comienzan con las firmas 'P' 'K'
        assertEquals('P', (char) xlsx[0]);
        assertEquals('K', (char) xlsx[1]);
    }

    @Test
    void exportarCsv_contieneCabecerasYDatos() {
        byte[] csv = exportService.exportarCsv(COLUMNAS, FILAS);
        String contenido = new String(csv, StandardCharsets.UTF_8);

        assertTrue(contenido.contains("Libro"));
        assertTrue(contenido.contains("Préstamos"));
        assertTrue(contenido.contains("Don Quijote"));
        assertTrue(contenido.contains("El principito"));
    }

    @Test
    void exportarCsv_contraeBomDeUtf8ParaExcel() {
        byte[] csv = exportService.exportarCsv(COLUMNAS, FILAS);

        // BOM UTF-8: EF BB BF
        assertEquals((byte) 0xEF, csv[0]);
        assertEquals((byte) 0xBB, csv[1]);
        assertEquals((byte) 0xBF, csv[2]);
    }

    @Test
    void nombreArchivo_incluyeTipoFechaYExtension() {
        String nombre = exportService.nombreArchivo("top-libros", "pdf");

        assertTrue(nombre.startsWith("reporte_top-libros_"));
        assertTrue(nombre.endsWith(".pdf"));
    }
}
