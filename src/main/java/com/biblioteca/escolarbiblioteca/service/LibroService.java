package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.dto.LibroDTO;
import com.biblioteca.escolarbiblioteca.exception.ResourceNotFoundException;
import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class LibroService {

    private final LibroRepository libroRepository;

    public List<LibroDTO> findAll() {
        return libroRepository.findAll().stream()
                .map(LibroDTO::new)
                .collect(Collectors.toList());
    }

    public LibroDTO findById(Long id) {
        Libro libro = libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
        return new LibroDTO(libro);
    }

    public List<LibroDTO> buscarPorTermino(String termino) {
        return libroRepository.buscarPorTermino(termino).stream()
                .map(LibroDTO::new)
                .collect(Collectors.toList());
    }

    public List<LibroDTO> buscarPorCategoria(String categoria) {
        return libroRepository.findByCategoriaIgnoreCase(categoria).stream()
                .map(LibroDTO::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public LibroDTO save(LibroDTO libroDTO) {
        if (libroRepository.existsByCodigoLibro(libroDTO.getCodigoLibro())) {
            throw new RuntimeException("Ya existe un libro con el código: " + libroDTO.getCodigoLibro());
        }

        Libro libro = Libro.builder()
                .nombre(libroDTO.getNombre())
                .categoria(libroDTO.getCategoria())
                .codigoLibro(libroDTO.getCodigoLibro())
                .stockTotal(libroDTO.getStockTotal())
                .stockDisponible(libroDTO.getStockTotal()) // Inicialmente igual al stock total
                .build();

        Libro savedLibro = libroRepository.save(libro);
        return new LibroDTO(savedLibro);
    }

    @Transactional
    public LibroDTO update(Long id, LibroDTO libroDTO) {
        Libro libro = libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
        
        libro.setNombre(libroDTO.getNombre());
        libro.setCategoria(libroDTO.getCategoria());
        
        // Si el código cambia, verificar que no exista
        if (!libro.getCodigoLibro().equals(libroDTO.getCodigoLibro())) {
            if (libroRepository.existsByCodigoLibro(libroDTO.getCodigoLibro())) {
                throw new RuntimeException("Ya existe un libro con el código: " + libroDTO.getCodigoLibro());
            }
            libro.setCodigoLibro(libroDTO.getCodigoLibro());
        }
        
        // Actualizar stock
        int diferencia = libroDTO.getStockTotal() - libro.getStockTotal();
        libro.setStockTotal(libroDTO.getStockTotal());
        libro.setStockDisponible(libro.getStockDisponible() + diferencia);
        
        // Asegurar que el stock disponible no sea negativo
        if (libro.getStockDisponible() < 0) {
            throw new RuntimeException("No se puede reducir el stock por debajo de los libros prestados actualmente");
        }
        
        Libro updatedLibro = libroRepository.save(libro);
        return new LibroDTO(updatedLibro);
    }

    @Transactional
    public void deleteById(Long id) {
        Libro libro = libroRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + id));
        
        // Verificar que no tenga préstamos activos
        if (libro.getStockDisponible() < libro.getStockTotal()) {
            throw new RuntimeException("No se puede eliminar el libro porque tiene préstamos activos");
        }
        
        libroRepository.delete(libro);
    }

    @Transactional
    public void incrementarStockDisponible(Long libroId) {
        Libro libro = libroRepository.findById(libroId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + libroId));
        libro.setStockDisponible(libro.getStockDisponible() + 1);
        libroRepository.save(libro);
    }

    @Transactional
    public void decrementarStockDisponible(Long libroId) {
        Libro libro = libroRepository.findById(libroId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + libroId));
        if (libro.getStockDisponible() <= 0) {
            throw new RuntimeException("No hay stock disponible para este libro");
        }
        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);
    }
}