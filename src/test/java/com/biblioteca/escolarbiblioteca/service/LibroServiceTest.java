package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.dto.LibroDTO;
import com.biblioteca.escolarbiblioteca.exception.ResourceNotFoundException;
import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de las reglas de negocio de LibroService.
 */
@ExtendWith(MockitoExtension.class)
class LibroServiceTest {

    @Mock
    private LibroRepository libroRepository;

    @InjectMocks
    private LibroService libroService;

    private Libro libro(Long id, Integer stockTotal, Integer stockDisponible) {
        return Libro.builder()
                .id(id)
                .nombre("Don Quijote de la Mancha")
                .categoria("Novela")
                .codigoLibro("LIB-001")
                .stockTotal(stockTotal)
                .stockDisponible(stockDisponible)
                .build();
    }

    private LibroDTO dto(String codigo, Integer stockTotal) {
        LibroDTO dto = new LibroDTO();
        dto.setNombre("Don Quijote de la Mancha");
        dto.setCategoria("Novela");
        dto.setCodigoLibro(codigo);
        dto.setStockTotal(stockTotal);
        return dto;
    }

    @Test
    void guardarLibroNuevo_stockDisponibleIgualAlTotal() {
        when(libroRepository.existsByCodigoLibro("LIB-001")).thenReturn(false);
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        LibroDTO resultado = libroService.save(dto("LIB-001", 5));

        ArgumentCaptor<Libro> captor = ArgumentCaptor.forClass(Libro.class);
        verify(libroRepository).save(captor.capture());
        assertEquals(5, captor.getValue().getStockDisponible());
        assertEquals(5, resultado.getStockDisponible());
    }

    @Test
    void guardarLibro_conCodigoRepetido_lanzaError() {
        when(libroRepository.existsByCodigoLibro("LIB-001")).thenReturn(true);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> libroService.save(dto("LIB-001", 5)));

        assertTrue(ex.getMessage().contains("Ya existe un libro con el código"));
        verify(libroRepository, never()).save(any());
    }

    @Test
    void buscarPorId_inexistente_lanzaResourceNotFound() {
        when(libroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> libroService.findById(99L));
    }

    @Test
    void actualizarStock_porDebajoDeLosPrestados_lanzaError() {
        // 5 en total, solo 1 disponible => 4 libros prestados
        Libro existente = libro(1L, 5, 1);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(existente));

        // Si piden bajar el total a 2, disponible quedaría en -2 => error
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> libroService.update(1L, dto("LIB-001", 2)));

        assertTrue(ex.getMessage().contains("No se puede reducir el stock"));
        verify(libroRepository, never()).save(any());
    }

    @Test
    void eliminarLibro_conPrestamosActivos_lanzaError() {
        // stockDisponible < stockTotal => hay libros prestados
        Libro prestado = libro(1L, 5, 3);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(prestado));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> libroService.deleteById(1L));

        assertTrue(ex.getMessage().contains("tiene préstamos activos"));
        verify(libroRepository, never()).delete(any());
    }

    @Test
    void eliminarLibro_sinPrestamos_loElimina() {
        Libro disponible = libro(1L, 5, 5);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(disponible));

        libroService.deleteById(1L);

        verify(libroRepository).delete(disponible);
    }

    @Test
    void decrementarStock_sinDisponible_lanzaError() {
        Libro sinStock = libro(1L, 5, 0);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(sinStock));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> libroService.decrementarStockDisponible(1L));

        assertTrue(ex.getMessage().contains("No hay stock disponible"));
    }

    @Test
    void incrementarStock_aumentaUnaUnidad() {
        Libro libro = libro(1L, 5, 3);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libro));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        libroService.incrementarStockDisponible(1L);

        assertEquals(4, libro.getStockDisponible());
        verify(libroRepository).save(libro);
    }

    @Test
    void buscarPorTermino_devuelveListaDeDTOs() {
        when(libroRepository.buscarPorTermino("quijote")).thenReturn(java.util.List.of(libro(1L, 5, 5)));

        var resultados = libroService.buscarPorTermino("quijote");

        assertEquals(1, resultados.size());
        assertEquals("Don Quijote de la Mancha", resultados.get(0).getNombre());
    }
}
