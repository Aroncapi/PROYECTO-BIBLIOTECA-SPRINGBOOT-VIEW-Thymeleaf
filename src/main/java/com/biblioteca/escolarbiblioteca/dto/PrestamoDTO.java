package com.biblioteca.escolarbiblioteca.dto;

import com.biblioteca.escolarbiblioteca.model.Prestamo;
import lombok.Data;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Data
public class PrestamoDTO {
    
    private Long id;
    private Long usuarioId;
    private String usuarioNombre;
    private String usuarioEmail;
    private Long libroId;
    private String libroNombre;
    private String libroCodigo;
    private LocalDateTime fechaPrestamo;
    private LocalDateTime fechaDevolucionEstimada;
    private LocalDateTime fechaDevolucionReal;
    private String estado;
    
    // Nuevos campos
    private Integer diasRestantes;
    private Integer diasRetraso;
    
    public PrestamoDTO() {}
    
    public PrestamoDTO(Prestamo prestamo) {
        this.id = prestamo.getId();
        this.usuarioId = prestamo.getUsuario().getId();
        this.usuarioNombre = prestamo.getUsuario().getNombre() + " " + prestamo.getUsuario().getApellido();
        this.usuarioEmail = prestamo.getUsuario().getEmail();
        this.libroId = prestamo.getLibro().getId();
        this.libroNombre = prestamo.getLibro().getNombre();
        this.libroCodigo = prestamo.getLibro().getCodigoLibro();
        this.fechaPrestamo = prestamo.getFechaPrestamo();
        this.fechaDevolucionEstimada = prestamo.getFechaDevolucionEstimada();
        this.fechaDevolucionReal = prestamo.getFechaDevolucionReal();
        this.estado = prestamo.getEstado().name();
    }
    
    public String getFechaPrestamoFormatted() {
        return fechaPrestamo != null ? 
            fechaPrestamo.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }
    
    public String getFechaDevolucionEstimadaFormatted() {
        return fechaDevolucionEstimada != null ? 
            fechaDevolucionEstimada.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }
    
    public String getFechaDevolucionRealFormatted() {
        return fechaDevolucionReal != null ? 
            fechaDevolucionReal.format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")) : "";
    }
}