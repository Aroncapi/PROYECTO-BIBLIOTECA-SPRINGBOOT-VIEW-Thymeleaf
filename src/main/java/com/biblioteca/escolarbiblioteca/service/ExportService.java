package com.biblioteca.escolarbiblioteca.service;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Servicio de exportación de reportes a diferentes formatos:
 * PDF (OpenHTMLtoPDF), Excel (Apache POI) y CSV (Apache Commons CSV).
 */
@Service
@Slf4j
public class ExportService {

    private static final DateTimeFormatter FECHA_ARCHIVO =
            DateTimeFormatter.ofPattern("yyyy-MM-dd");

    /**
     * Genera un archivo PDF con una tabla estilizada.
     */
    public byte[] exportarPdf(String titulo, String[] columnas, List<String[]> filas) {
        String html = construirHtml(titulo, columnas, filas);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, null);
            builder.toStream(salida);
            builder.run();
        } catch (Exception e) {
            log.error("Error al generar PDF: {}", e.getMessage());
            throw new RuntimeException("Error al generar el archivo PDF", e);
        }
        return salida.toByteArray();
    }

    /**
     * Genera un archivo Excel (.xlsx) con encabezado resaltado.
     */
    public byte[] exportarExcel(String nombreHoja, String[] columnas, List<String[]> filas) {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet(nombreHoja);

            CellStyle estiloEncabezado = workbook.createCellStyle();
            Font fuente = workbook.createFont();
            fuente.setBold(true);
            fuente.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuente);
            estiloEncabezado.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);

            Row filaEncabezado = sheet.createRow(0);
            for (int i = 0; i < columnas.length; i++) {
                Cell celda = filaEncabezado.createCell(i);
                celda.setCellValue(columnas[i]);
                celda.setCellStyle(estiloEncabezado);
            }

            int numeroFila = 1;
            for (String[] fila : filas) {
                Row row = sheet.createRow(numeroFila++);
                for (int i = 0; i < columnas.length; i++) {
                    row.createCell(i).setCellValue(i < fila.length && fila[i] != null ? fila[i] : "");
                }
            }

            // Ancho estimado de columnas
            for (int i = 0; i < columnas.length; i++) {
                int anchoMax = columnas[i].length();
                for (String[] fila : filas) {
                    if (i < fila.length && fila[i] != null) {
                        anchoMax = Math.max(anchoMax, fila[i].length());
                    }
                }
                sheet.setColumnWidth(i, Math.min(anchoMax + 4, 60) * 256);
            }

            ByteArrayOutputStream salida = new ByteArrayOutputStream();
            workbook.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            log.error("Error al generar Excel: {}", e.getMessage());
            throw new RuntimeException("Error al generar el archivo Excel", e);
        }
    }

    /**
     * Genera un archivo CSV codificado en UTF-8 con BOM (compatible con Excel).
     */
    public byte[] exportarCsv(String[] columnas, List<String[]> filas) {
        try (StringWriter escritor = new StringWriter()) {
            CSVPrinter printer = new CSVPrinter(escritor,
                    CSVFormat.DEFAULT.builder().setHeader(columnas).build());
            for (String[] fila : filas) {
                printer.printRecord((Object[]) fila);
            }
            printer.flush();
            // BOM UTF-8 para que Excel reconozca acentos y ñ
            String contenido = "\uFEFF" + escritor;
            return contenido.getBytes(StandardCharsets.UTF_8);
        } catch (IOException e) {
            log.error("Error al generar CSV: {}", e.getMessage());
            throw new RuntimeException("Error al generar el archivo CSV", e);
        }
    }

    /**
     * Nombre de archivo estándar: reporte_{tipo}_{fecha}.{ext}
     */
    public String nombreArchivo(String tipo, String extension) {
        return "reporte_" + tipo + "_" + LocalDateTime.now().format(FECHA_ARCHIVO) + "." + extension;
    }

    private String construirHtml(String titulo, String[] columnas, List<String[]> filas) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n");
        sb.append("<!DOCTYPE html>\n");
        sb.append("<html xmlns=\"http://www.w3.org/1999/xhtml\">\n");
        sb.append("<head><title>").append(escapar(titulo)).append("</title>\n");
        sb.append("<style>\n");
        sb.append("body { font-family: Helvetica, sans-serif; font-size: 12px; color: #333333; }\n");
        sb.append("h1 { color: #1e4a7a; font-size: 20px; }\n");
        sb.append(".subtitulo { color: #666666; font-size: 11px; margin-bottom: 16px; }\n");
        sb.append("table { width: 100%; border-collapse: collapse; }\n");
        sb.append("th { background-color: #1e4a7a; color: #ffffff; padding: 8px 6px; font-size: 10px; text-align: left; }\n");
        sb.append("td { border-bottom: 1px solid #cccccc; padding: 6px; font-size: 10px; }\n");
        sb.append("</style></head>\n<body>\n");
        sb.append("<h1>").append(escapar(titulo)).append("</h1>\n");
        sb.append("<div class=\"subtitulo\">Biblioteca Escolar - Generado el ")
          .append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")))
          .append("</div>\n");
        sb.append("<table>\n<thead><tr>");
        for (String columna : columnas) {
            sb.append("<th>").append(escapar(columna)).append("</th>");
        }
        sb.append("</tr></thead>\n<tbody>\n");
        for (String[] fila : filas) {
            sb.append("<tr>");
            for (int i = 0; i < columnas.length; i++) {
                String valor = i < fila.length && fila[i] != null ? fila[i] : "";
                sb.append("<td>").append(escapar(valor)).append("</td>");
            }
            sb.append("</tr>\n");
        }
        sb.append("</tbody>\n</table>\n</body>\n</html>");
        return sb.toString();
    }

    private String escapar(String texto) {
        if (texto == null) {
            return "";
        }
        return texto.replace("&", "&amp;")
                    .replace("<", "&lt;")
                    .replace(">", "&gt;")
                    .replace("\"", "&quot;");
    }
}
