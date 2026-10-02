package com.biblioteca.escolarbiblioteca.repository;

import com.biblioteca.escolarbiblioteca.config.JpaAuditingConfig;
import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.model.Prestamo;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;

import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de PrestamoRepository sobre H2 en memoria.
 * Incluye queries JPQL de reportes y actualización masiva de vencidos.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class PrestamoRepositoryTest {

    @Autowired
    private PrestamoRepository prestamoRepository;

    @Autowired
    private LibroRepository libroRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RolRepository rolRepository;

    @PersistenceContext
    private EntityManager em;

    private Rol rol;
    private Usuario ana;
    private Usuario luis;
    private Libro quijote;
    private Libro popeye;

    @BeforeEach
    void setUp() {
        rol = rolRepository.save(new Rol(null, Rol.ERol.ALUMNO));

        ana = usuarioRepository.save(Usuario.builder()
                .nombre("Ana").apellido("Torres").email("ana@test.com")
                .password("$2a$hash").rol(rol)
                .nivel(Usuario.Nivel.PRIMARIA).grado(3).seccion("A")
                .build());

        luis = usuarioRepository.save(Usuario.builder()
                .nombre("Luis").apellido("García").email("luis@test.com")
                .password("$2a$hash").rol(rol)
                .nivel(Usuario.Nivel.SECUNDARIA).grado(2).seccion("B")
                .build());

        quijote = libroRepository.save(Libro.builder()
                .nombre("Don Quijote").categoria("Novela")
                .codigoLibro("LIB-001").stockTotal(3).stockDisponible(2)
                .build());

        popeye = libroRepository.save(Libro.builder()
                .nombre("Popeye").categoria("Historieta")
                .codigoLibro("LIB-002").stockTotal(2).stockDisponible(2)
                .build());

        // prestamo1: Ana, activo, pendiente
        prestamoRepository.save(Prestamo.builder()
                .usuario(ana).libro(quijote)
                .fechaDevolucionEstimada(LocalDateTime.now().plusDays(7))
                .estado(Prestamo.EstadoPrestamo.ACTIVO)
                .build());

        // prestamo2: Luis, ya devuelto
        prestamoRepository.save(Prestamo.builder()
                .usuario(luis).libro(quijote)
                .fechaDevolucionEstimada(LocalDateTime.now().minusDays(1))
                .fechaDevolucionReal(LocalDateTime.now().minusDays(1))
                .estado(Prestamo.EstadoPrestamo.DEVUELTO)
                .build());

        // prestamo3: Ana, activo pero YA vencido
        prestamoRepository.save(Prestamo.builder()
                .usuario(ana).libro(popeye)
                .fechaDevolucionEstimada(LocalDateTime.now().minusDays(2))
                .estado(Prestamo.EstadoPrestamo.ACTIVO)
                .build());
    }

    @Test
    void findByUsuario_devuelveSoloSusPrestamos() {
        List<Prestamo> deAna = prestamoRepository.findByUsuario(ana);
        List<Prestamo> deLuis = prestamoRepository.findByUsuario(luis);

        assertEquals(2, deAna.size());
        assertEquals(1, deLuis.size());
        assertTrue(deAna.stream().allMatch(p -> p.getUsuario().getId().equals(ana.getId())));
    }

    @Test
    void findByUsuarioAndEstado_filtraPorUsuarioYEstado() {
        List<Prestamo> activosDeAna =
                prestamoRepository.findByUsuarioAndEstado(ana, Prestamo.EstadoPrestamo.ACTIVO);

        assertEquals(2, activosDeAna.size());
    }

    @Test
    void findByEstado_devuelveTodosConEseEstado() {
        List<Prestamo> devueltos =
                prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.DEVUELTO);

        assertEquals(1, devueltos.size());
        assertEquals(luis.getId(), devueltos.get(0).getUsuario().getId());
    }

    @Test
    void findByEstadoYFechaAnterior_devuelveVencidos() {
        List<Prestamo> vencidos = prestamoRepository.findByEstadoAndFechaDevolucionEstimadaBefore(
                Prestamo.EstadoPrestamo.ACTIVO, LocalDateTime.now());

        assertEquals(1, vencidos.size());
        assertEquals("Popeye", vencidos.get(0).getLibro().getNombre());
    }

    @Test
    void countByUsuarioAndEstado_cuentaCorrectamente() {
        assertEquals(2, prestamoRepository.countByUsuarioAndEstado(ana, Prestamo.EstadoPrestamo.ACTIVO));
        assertEquals(0, prestamoRepository.countByUsuarioAndEstado(luis, Prestamo.EstadoPrestamo.ACTIVO));
        assertEquals(1, prestamoRepository.countByUsuarioAndEstado(luis, Prestamo.EstadoPrestamo.DEVUELTO));
    }

    @Test
    void actualizarPrestamosVencidos_cambiaSoloALosVencidos() {
        int actualizados = prestamoRepository.actualizarPrestamosVencidos(
                Prestamo.EstadoPrestamo.ACTIVO,
                Prestamo.EstadoPrestamo.VENCIDO,
                LocalDateTime.now());

        assertEquals(1, actualizados);

        em.clear();
        Prestamo exActivo = prestamoRepository.findById(
                prestamoRepository.findByUsuario(ana).stream()
                        .filter(p -> p.getLibro().getId().equals(quijote.getId()))
                        .findFirst().orElseThrow().getId()).orElseThrow();
        Prestamo exVencido = prestamoRepository.findById(
                prestamoRepository.findByUsuario(ana).stream()
                        .filter(p -> p.getLibro().getId().equals(popeye.getId()))
                        .findFirst().orElseThrow().getId()).orElseThrow();

        assertEquals(Prestamo.EstadoPrestamo.ACTIVO, exActivo.getEstado());
        assertEquals(Prestamo.EstadoPrestamo.VENCIDO, exVencido.getEstado());
    }

    @Test
    void findTopLibrosPrestados_ordenadoPorTotalDesc() {
        List<Object[]> top = prestamoRepository.findTopLibrosPrestados();

        assertEquals(2, top.size());
        assertEquals(quijote.getId(), top.get(0)[0]);
        assertEquals(2L, ((Number) top.get(0)[1]).longValue());
        assertEquals(popeye.getId(), top.get(1)[0]);
        assertEquals(1L, ((Number) top.get(1)[1]).longValue());
    }

    @Test
    void contarPrestamosPorMes_agrupaPorAnioYMes() {
        List<Object[]> filas = prestamoRepository.contarPrestamosPorMes(
                LocalDateTime.now().minusDays(1));

        assertEquals(1, filas.size());
        assertEquals(3L, ((Number) filas.get(0)[2]).longValue());
    }
}
