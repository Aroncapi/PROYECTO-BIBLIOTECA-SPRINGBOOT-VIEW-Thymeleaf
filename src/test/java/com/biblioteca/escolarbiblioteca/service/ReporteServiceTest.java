package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.model.Prestamo;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import com.biblioteca.escolarbiblioteca.repository.PrestamoRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Pruebas unitarias de ReporteService (datos del dashboard y reportes gráficos).
 */
@ExtendWith(MockitoExtension.class)
class ReporteServiceTest {

    @Mock
    private PrestamoRepository prestamoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @Mock
    private LibroRepository libroRepository;

    @InjectMocks
    private ReporteService reporteService;

    private Usuario usuario(Long id, String nombre, Rol.ERol rol) {
        return Usuario.builder()
                .id(id).nombre(nombre).apellido("García")
                .email(nombre.toLowerCase() + "@biblioteca.com")
                .rol(new Rol(1L, rol))
                .build();
    }

    private Libro libro(Long id, String categoria) {
        return Libro.builder()
                .id(id).nombre("Libro " + id).categoria(categoria)
                .codigoLibro("LIB-" + id).stockTotal(3).stockDisponible(3)
                .build();
    }

    private Prestamo prestamo(Usuario u, Libro l) {
        return Prestamo.builder()
                .id(1L).usuario(u).libro(l)
                .estado(Prestamo.EstadoPrestamo.ACTIVO)
                .fechaPrestamo(LocalDateTime.now())
                .fechaDevolucionEstimada(LocalDateTime.now().plusDays(7))
                .build();
    }

    @Test
    void dashboardStats_contieneLos6Indicadores() {
        Usuario alumno = usuario(1L, "Ana", Rol.ERol.ALUMNO);
        Usuario admin = usuario(2L, "Luis", Rol.ERol.ADMIN);
        when(libroRepository.count()).thenReturn(10L);
        when(usuarioRepository.findAll()).thenReturn(List.of(alumno, admin));
        when(prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.ACTIVO))
                .thenReturn(List.of(prestamo(alumno, libro(1L, "Novela"))));
        when(prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.VENCIDO))
                .thenReturn(List.of(prestamo(alumno, libro(2L, "Novela")),
                        prestamo(alumno, libro(3L, "Ciencia"))));
        when(prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.DEVUELTO))
                .thenReturn(List.of());
        when(prestamoRepository.findTopLibrosPrestados()).thenReturn(List.of());

        Map<String, Object> stats = reporteService.getDashboardStats();

        assertEquals(10L, stats.get("totalLibros"));
        assertEquals(1L, stats.get("totalAlumnos")); // solo cuenta rol ALUMNO
        assertEquals(1, stats.get("prestamosActivos"));
        assertEquals(2, stats.get("prestamosVencidos"));
        assertEquals(0L, stats.get("prestamosDevueltos"));
        assertNotNull(stats.get("topLibros"));
    }

    @Test
    void topLibrosPrestados_limitaA5YEnriqueceConDatosDelLibro() {
        Usuario alumno = usuario(1L, "Ana", Rol.ERol.ALUMNO);
        // 6 filas (libro_id, total) — solo deben quedar 5
        when(prestamoRepository.findTopLibrosPrestados()).thenReturn(List.of(
                new Object[]{1L, 10L}, new Object[]{2L, 8L}, new Object[]{3L, 6L},
                new Object[]{4L, 4L}, new Object[]{5L, 2L}, new Object[]{6L, 1L}));
        // Solo se consultan los 5 primeros (el .limit(5) descarta el 6to)
        for (long id = 1; id <= 5; id++) {
            when(libroRepository.findById(id)).thenReturn(Optional.of(libro(id, "Novela")));
        }

        List<Map<String, Object>> top = reporteService.getTopLibrosPrestados();

        assertEquals(5, top.size());
        assertEquals(1L, top.get(0).get("libroId"));
        assertEquals(10L, top.get(0).get("totalPrestamos"));
        assertEquals("Libro 1", top.get(0).get("libroNombre"));
    }

    @Test
    void prestamosPorMes_rellenaMesesSinDatosConCero() {
        int anio = YearMonth.now().getYear();
        int mes = YearMonth.now().getMonthValue();
        // Solo el mes actual tiene datos
        when(prestamoRepository.contarPrestamosPorMes(any()))
                .thenReturn(List.<Object[]>of(new Object[]{anio, mes, 4L}));

        Map<String, Object> resultado = reporteService.getPrestamosPorMes(6);

        @SuppressWarnings("unchecked")
        List<String> labels = (List<String>) resultado.get("labels");
        @SuppressWarnings("unchecked")
        List<Long> data = (List<Long>) resultado.get("data");

        assertEquals(6, labels.size());
        assertEquals(6, data.size());
        assertEquals(4L, data.get(5));       // último mes = mes actual
        assertEquals(0L, data.get(0));       // primer mes sin datos = 0
    }

    @Test
    void topUsuariosActivos_ordenaDeMayorAMenorYRespetaElLimite() {
        Usuario ana = usuario(1L, "Ana", Rol.ERol.ALUMNO);
        Usuario luis = usuario(2L, "Luis", Rol.ERol.ALUMNO);
        Libro libro = libro(1L, "Novela");
        when(prestamoRepository.findAll()).thenReturn(List.of(
                prestamo(ana, libro), prestamo(ana, libro), prestamo(luis, libro)));

        List<Map<String, Object>> top = reporteService.getTopUsuariosActivos(1);

        assertEquals(1, top.size());
        assertEquals(1L, top.get(0).get("usuarioId"));
        assertEquals(2L, top.get(0).get("totalPrestamos"));
        assertEquals("Ana García", top.get(0).get("usuario"));
    }

    @Test
    void estadisticasCategorias_agrupaLibrosYPrestamos() {
        Libro novela = libro(1L, "Novela");
        Libro ciencia = libro(2L, "Ciencia ficción");
        when(libroRepository.findAll()).thenReturn(List.of(novela, ciencia));
        when(prestamoRepository.findAll()).thenReturn(List.of(prestamo(usuario(1L, "Ana", Rol.ERol.ALUMNO), novela)));

        Map<String, Object> resultado = reporteService.getEstadisticasCategorias();

        @SuppressWarnings("unchecked")
        Map<String, Long> librosPorCategoria = (Map<String, Long>) resultado.get("librosPorCategoria");
        @SuppressWarnings("unchecked")
        Map<String, Long> prestamosPorCategoria = (Map<String, Long>) resultado.get("prestamosPorCategoria");

        assertEquals(1L, librosPorCategoria.get("Novela"));
        assertEquals(1L, librosPorCategoria.get("Ciencia ficción"));
        assertEquals(1L, prestamosPorCategoria.get("Novela"));
    }
}
