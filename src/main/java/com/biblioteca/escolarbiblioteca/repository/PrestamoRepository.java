package com.biblioteca.escolarbiblioteca.repository;

import com.biblioteca.escolarbiblioteca.model.Prestamo;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface PrestamoRepository extends JpaRepository<Prestamo, Long> {
    
    List<Prestamo> findByUsuario(Usuario usuario);
    
    List<Prestamo> findByUsuarioAndEstado(Usuario usuario, Prestamo.EstadoPrestamo estado);
    
    List<Prestamo> findByEstado(Prestamo.EstadoPrestamo estado);
    
    @Query("SELECT p FROM Prestamo p WHERE p.estado = :estado AND p.fechaDevolucionEstimada < :fecha")
    List<Prestamo> findByEstadoAndFechaDevolucionEstimadaBefore(
            @Param("estado") Prestamo.EstadoPrestamo estado, 
            @Param("fecha") LocalDateTime fecha);
    
    @Query("SELECT COUNT(p) FROM Prestamo p WHERE p.usuario = :usuario AND p.estado = :estado")
    long countByUsuarioAndEstado(@Param("usuario") Usuario usuario, 
                                  @Param("estado") Prestamo.EstadoPrestamo estado);
    
    @Modifying
    @Query("UPDATE Prestamo p SET p.estado = :nuevoEstado WHERE p.estado = :estadoActual AND p.fechaDevolucionEstimada < :fecha")
    int actualizarPrestamosVencidos(
            @Param("estadoActual") Prestamo.EstadoPrestamo estadoActual,
            @Param("nuevoEstado") Prestamo.EstadoPrestamo nuevoEstado,
            @Param("fecha") LocalDateTime fecha);
    
    @Query("SELECT p.libro.id, COUNT(p) as total FROM Prestamo p GROUP BY p.libro.id ORDER BY total DESC")
    List<Object[]> findTopLibrosPrestados();

    @Query("SELECT YEAR(p.fechaPrestamo), MONTH(p.fechaPrestamo), COUNT(p) FROM Prestamo p " +
           "WHERE p.fechaPrestamo >= :desde " +
           "GROUP BY YEAR(p.fechaPrestamo), MONTH(p.fechaPrestamo) " +
           "ORDER BY YEAR(p.fechaPrestamo), MONTH(p.fechaPrestamo)")
    List<Object[]> contarPrestamosPorMes(@Param("desde") LocalDateTime desde);
}