package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.LibroDTO;
import com.biblioteca.escolarbiblioteca.service.LibroService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/libros")
@RequiredArgsConstructor
public class LibroController {

    private final LibroService libroService;

    /**
     * Obtener todos los libros
     */
    @GetMapping
    public ResponseEntity<List<LibroDTO>> getAllLibros() {
        // CORREGIDO: libroService.findAll() ya retorna List<LibroDTO>
        return ResponseEntity.ok(libroService.findAll());
    }

    /**
     * Obtener libro por ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<LibroDTO> getLibroById(@PathVariable Long id) {
        // CORREGIDO: libroService.findById(id) ya retorna LibroDTO
        return ResponseEntity.ok(libroService.findById(id));
    }

    /**
     * Buscar libros por término
     */
    @GetMapping("/buscar")
    public ResponseEntity<List<LibroDTO>> buscarLibros(@RequestParam String termino) {
        // CORREGIDO: libroService.buscarPorTermino(termino) ya retorna List<LibroDTO>
        return ResponseEntity.ok(libroService.buscarPorTermino(termino));
    }

    /**
     * Obtener libros por categoría
     */
    @GetMapping("/categoria/{categoria}")
    public ResponseEntity<List<LibroDTO>> getLibrosByCategoria(@PathVariable String categoria) {
        // CORREGIDO: libroService.buscarPorCategoria(categoria) ya retorna List<LibroDTO>
        return ResponseEntity.ok(libroService.buscarPorCategoria(categoria));
    }

    /**
     * Crear nuevo libro
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibroDTO> createLibro(@Valid @RequestBody LibroDTO libroDTO) {
        // CORREGIDO: libroService.save(libroDTO) ya retorna LibroDTO
        LibroDTO nuevoLibro = libroService.save(libroDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(nuevoLibro);
    }

    /**
     * Actualizar libro
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<LibroDTO> updateLibro(@PathVariable Long id, @Valid @RequestBody LibroDTO libroDTO) {
        // CORREGIDO: libroService.update(id, libroDTO) ya retorna LibroDTO
        LibroDTO libroActualizado = libroService.update(id, libroDTO);
        return ResponseEntity.ok(libroActualizado);
    }

    /**
     * Eliminar libro
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<?> deleteLibro(@PathVariable Long id) {
        libroService.deleteById(id);
        return ResponseEntity.ok()
                .body(java.util.Map.of("message", "Libro eliminado correctamente"));
    }
}