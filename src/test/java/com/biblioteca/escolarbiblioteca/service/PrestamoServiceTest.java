package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.exception.ResourceNotFoundException;
import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.model.Prestamo;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import com.biblioteca.escolarbiblioteca.repository.PrestamoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de las reglas de negocio de PrestamoService:
 * límites de préstamos, roles, stock y devoluciones.
 */
@ExtendWith(MockitoExtension.class)
class PrestamoServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private LibroRepository libroRepository;

    @Mock
    private UsuarioService usuarioService;

    @InjectMocks
    private PrestamoService prestamoService;

    private Usuario usuario(Long id, Rol.ERol rol) {
        return Usuario.builder()
                .id(id)
                .nombre("Juan")
                .apellido("Pérez")
                .email("juan@biblioteca.com")
                .rol(new Rol(1L, rol))
                .build();
    }

    private Libro libro(Integer stockDisponible) {
        return Libro.builder()
                .id(1L)
                .nombre("El principito")
                .categoria("Novela")
                .codigoLibro("LIB-002")
                .stockTotal(3)
                .stockDisponible(stockDisponible)
                .build();
    }

    private void sinReglasDeBloqueo(Usuario usuario) {
        when(prestamoRepository.countByUsuarioAndEstado(usuario, Prestamo.EstadoPrestamo.VENCIDO))
                .thenReturn(0L);
        when(prestamoRepository.countByUsuarioAndEstado(usuario, Prestamo.EstadoPrestamo.ACTIVO))
                .thenReturn(0L);
    }

    // ---------- solicitarPrestamo ----------

    @Test
    void solicitarPrestamo_comoAdmin_lanzaError() {
        Usuario admin = usuario(1L, Rol.ERol.ADMIN);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.solicitarPrestamo(admin, 1L));

        assertTrue(ex.getMessage().contains("Solo los alumnos pueden solicitar préstamos"));
    }

    @Test
    void solicitarPrestamo_conPrestamoVencido_lanzaError() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        when(prestamoRepository.countByUsuarioAndEstado(alumno, Prestamo.EstadoPrestamo.VENCIDO))
                .thenReturn(1L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.solicitarPrestamo(alumno, 1L));

        assertTrue(ex.getMessage().contains("préstamos vencidos"));
    }

    @Test
    void solicitarPrestamo_alcanzoLimiteDe3_lanzaError() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        when(prestamoRepository.countByUsuarioAndEstado(alumno, Prestamo.EstadoPrestamo.VENCIDO))
                .thenReturn(0L);
        when(prestamoRepository.countByUsuarioAndEstado(alumno, Prestamo.EstadoPrestamo.ACTIVO))
                .thenReturn(3L);

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.solicitarPrestamo(alumno, 1L));

        assertTrue(ex.getMessage().contains("límite máximo de 3"));
        verify(prestamoRepository, never()).save(any());
    }

    @Test
    void solicitarPrestamo_libroSinStock_lanzaError() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        sinReglasDeBloqueo(alumno);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libro(0)));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.solicitarPrestamo(alumno, 1L));

        assertTrue(ex.getMessage().contains("no está disponible"));
    }

    @Test
    void solicitarPrestamo_libroInexistente_lanzaResourceNotFound() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        sinReglasDeBloqueo(alumno);
        when(libroRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class,
                () -> prestamoService.solicitarPrestamo(alumno, 99L));
    }

    @Test
    void solicitarPrestamo_exitoso_creaPrestamoYBajaStock() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        Libro libro = libro(3);
        sinReglasDeBloqueo(alumno);
        when(libroRepository.findById(1L)).thenReturn(Optional.of(libro));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = prestamoService.solicitarPrestamo(alumno, 1L);

        assertEquals(Prestamo.EstadoPrestamo.ACTIVO.name(), resultado.getEstado());
        assertEquals(2, libro.getStockDisponible()); // bajó de 3 a 2
        ArgumentCaptor<Prestamo> captor = ArgumentCaptor.forClass(Prestamo.class);
        verify(prestamoRepository).save(captor.capture());
        assertNotNull(captor.getValue().getFechaDevolucionEstimada());
        assertEquals(Prestamo.EstadoPrestamo.ACTIVO, captor.getValue().getEstado());
    }

    // ---------- devolverLibro ----------

    @Test
    void devolverPrestamo_deOtroUsuario_lanzaError() {
        Usuario dueno = usuario(1L, Rol.ERol.ALUMNO);
        Usuario otro = usuario(2L, Rol.ERol.ALUMNO);
        Prestamo prestamo = Prestamo.builder()
                .id(1L).usuario(dueno).libro(libro(2))
                .estado(Prestamo.EstadoPrestamo.ACTIVO)
                .fechaPrestamo(LocalDateTime.now())
                .fechaDevolucionEstimada(LocalDateTime.now().plusDays(7))
                .build();
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.devolverLibro(otro, 1L));

        assertTrue(ex.getMessage().contains("no le pertenece"));
    }

    @Test
    void devolverPrestamo_yaDevuelto_lanzaError() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        Prestamo prestamo = Prestamo.builder()
                .id(1L).usuario(alumno).libro(libro(2))
                .estado(Prestamo.EstadoPrestamo.DEVUELTO)
                .fechaPrestamo(LocalDateTime.now())
                .fechaDevolucionEstimada(LocalDateTime.now().minusDays(2))
                .build();
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.devolverLibro(alumno, 1L));

        assertTrue(ex.getMessage().contains("no está activo"));
    }

    @Test
    void devolverPrestamo_exitoso_subeStockYMarcaDevuelto() {
        Usuario alumno = usuario(1L, Rol.ERol.ALUMNO);
        Libro libro = libro(1);
        Prestamo prestamo = Prestamo.builder()
                .id(1L).usuario(alumno).libro(libro)
                .estado(Prestamo.EstadoPrestamo.ACTIVO)
                .fechaPrestamo(LocalDateTime.now())
                .fechaDevolucionEstimada(LocalDateTime.now().plusDays(7))
                .build();
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = prestamoService.devolverLibro(alumno, 1L);

        assertEquals(Prestamo.EstadoPrestamo.DEVUELTO.name(), resultado.getEstado());
        assertNotNull(prestamo.getFechaDevolucionReal());
        assertEquals(2, libro.getStockDisponible()); // subió de 1 a 2
    }

    // ---------- devolverLibroAdmin ----------

    @Test
    void devolverPrestamo_admin_prestamoYaDevuelto_lanzaError() {
        Prestamo prestamo = Prestamo.builder()
                .id(1L).usuario(usuario(1L, Rol.ERol.ALUMNO)).libro(libro(2))
                .estado(Prestamo.EstadoPrestamo.DEVUELTO)
                .fechaPrestamo(LocalDateTime.now())
                .fechaDevolucionEstimada(LocalDateTime.now().minusDays(3))
                .fechaDevolucionReal(LocalDateTime.now().minusDays(1))
                .build();
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> prestamoService.devolverLibroAdmin(1L));

        assertTrue(ex.getMessage().contains("no puede ser devuelto"));
    }

    @Test
    void devolverPrestamo_admin_prestamoVencido_loDevuelve() {
        Libro libro = libro(0);
        Prestamo prestamo = Prestamo.builder()
                .id(1L).usuario(usuario(1L, Rol.ERol.ALUMNO)).libro(libro)
                .estado(Prestamo.EstadoPrestamo.VENCIDO)
                .fechaPrestamo(LocalDateTime.now().minusDays(10))
                .fechaDevolucionEstimada(LocalDateTime.now().minusDays(3))
                .build();
        when(prestamoRepository.findById(1L)).thenReturn(Optional.of(prestamo));
        when(prestamoRepository.save(any(Prestamo.class))).thenAnswer(inv -> inv.getArgument(0));
        when(libroRepository.save(any(Libro.class))).thenAnswer(inv -> inv.getArgument(0));

        var resultado = prestamoService.devolverLibroAdmin(1L);

        assertEquals(Prestamo.EstadoPrestamo.DEVUELTO.name(), resultado.getEstado());
        assertEquals(1, libro.getStockDisponible()); // subió de 0 a 1
    }

    @Test
    void getPrestamoById_inexistente_lanzaResourceNotFound() {
        when(prestamoRepository.findById(42L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> prestamoService.getPrestamoById(42L));
    }
}
