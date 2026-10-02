package com.biblioteca.escolarbiblioteca.repository;

import com.biblioteca.escolarbiblioteca.config.JpaAuditingConfig;
import com.biblioteca.escolarbiblioteca.model.Libro;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.OptimisticLockingFailureException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Pruebas de integración de LibroRepository sobre H2 en memoria.
 * Verificación de queries derivadas, JPQL y control de concurrencia optimista.
 */
@DataJpaTest
@Import(JpaAuditingConfig.class)
class LibroRepositoryTest {

    @Autowired
    private LibroRepository libroRepository;

    @PersistenceContext
    private EntityManager em;

    private Libro nuevoLibro(String codigo, String nombre, String categoria) {
        return Libro.builder()
                .nombre(nombre)
                .categoria(categoria)
                .codigoLibro(codigo)
                .stockTotal(5)
                .stockDisponible(5)
                .build();
    }

    @Test
    void saveYFindById_devuelveElLibroGuardado() {
        Libro guardado = libroRepository.save(nuevoLibro("LIB-001", "Don Quijote", "Novela"));

        assertNotNull(guardado.getId());
        Libro encontrado = libroRepository.findById(guardado.getId()).orElseThrow();
        assertEquals("Don Quijote", encontrado.getNombre());
        assertEquals("LIB-001", encontrado.getCodigoLibro());
        assertEquals(0, encontrado.getVersion());
    }

    @Test
    void findByNombreContainingIgnoreCase_ignoraMayusculas() {
        libroRepository.save(nuevoLibro("LIB-002", "El Principito", "Cuento"));
        libroRepository.save(nuevoLibro("LIB-003", "Cien años de soledad", "Novela"));

        List<Libro> resultado = libroRepository.findByNombreContainingIgnoreCase("PRINCIPI");

        assertEquals(1, resultado.size());
        assertEquals("El Principito", resultado.get(0).getNombre());
    }

    @Test
    void findByCategoriaIgnoreCase_filtraPorCategoria() {
        libroRepository.save(nuevoLibro("LIB-004", "Poema del Cid", "Poesia"));
        libroRepository.save(nuevoLibro("LIB-005", "La Iliada", "Poesia"));
        libroRepository.save(nuevoLibro("LIB-006", "Frankenstein", "Novela"));

        List<Libro> poesia = libroRepository.findByCategoriaIgnoreCase("poesia");

        assertEquals(2, poesia.size());
    }

    @Test
    void buscarPorTermino_coincideEnNombreOCategoria() {
        libroRepository.save(nuevoLibro("LIB-007", "Romancero gitano", "Poesia"));
        libroRepository.save(nuevoLibro("LIB-008", "La casa de Bernarda Alba", "Teatro"));

        List<Libro> porNombre = libroRepository.buscarPorTermino("romancero");
        assertEquals(1, porNombre.size());

        List<Libro> porCategoria = libroRepository.buscarPorTermino("teatro");
        assertEquals(1, porCategoria.size());

        List<Libro> sinResultados = libroRepository.buscarPorTermino("matematica");
        assertTrue(sinResultados.isEmpty());
    }

    @Test
    void existsByCodigoLibro_detectaDuplicados() {
        libroRepository.save(nuevoLibro("LIB-009", "Libro Unico", "Novela"));

        assertTrue(libroRepository.existsByCodigoLibro("LIB-009"));
        assertFalse(libroRepository.existsByCodigoLibro("LIB-NO-EXISTE"));
    }

    @Test
    void auditoria_rellenaCreatedByYCreatedDate() {
        Libro libro = libroRepository.save(nuevoLibro("LIB-010", "Libro Auditado", "Novela"));

        assertEquals("sistema", libro.getCreatedBy());
        assertNotNull(libro.getCreatedDate());
    }

    @Test
    void guardarConVersionObsoleta_lanzaConflictoOptimista() {
        Libro libro = libroRepository.save(nuevoLibro("LIB-011", "Version 0", "Novela"));
        Long id = libro.getId();
        em.detach(libro); // copia "vieja" con versión 0

        // otra "sesión" edita el libro primero -> versión pasa a 1
        Libro otro = libroRepository.findById(id).orElseThrow();
        otro.setNombre("Editado por otra sesión");
        libroRepository.saveAndFlush(otro);

        // la copia obsoleta intenta guardarse -> conflicto optimista
        libro.setNombre("Edición conflictiva");
        assertThrows(OptimisticLockingFailureException.class, () -> {
            libroRepository.save(libro);
            libroRepository.flush();
        });
    }
}
