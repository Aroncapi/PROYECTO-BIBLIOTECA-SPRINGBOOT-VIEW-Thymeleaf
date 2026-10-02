package com.biblioteca.escolarbiblioteca.dto;

import com.biblioteca.escolarbiblioteca.model.Libro;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class LibroDTO {
    
    private Long id;
    
    @NotBlank(message = "El nombre del libro es obligatorio")
    @Size(max = 200, message = "El nombre no puede exceder los 200 caracteres")
    private String nombre;
    
    @NotBlank(message = "La categoría es obligatoria")
    private String categoria;
    
    @NotBlank(message = "El código del libro es obligatorio")
    @Pattern(regexp = "^[A-Z0-9-]+$", message = "El código debe contener solo mayúsculas, números y guiones")
    private String codigoLibro;
    
    @NotNull(message = "El stock total es obligatorio")
    @Min(value = 0, message = "El stock no puede ser negativo")
    private Integer stockTotal;
    
    private Integer stockDisponible;
    
    public LibroDTO() {}
    
    public LibroDTO(Libro libro) {
        this.id = libro.getId();
        this.nombre = libro.getNombre();
        this.categoria = libro.getCategoria();
        this.codigoLibro = libro.getCodigoLibro();
        this.stockTotal = libro.getStockTotal();
        this.stockDisponible = libro.getStockDisponible();
    }
}