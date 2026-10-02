package com.biblioteca.escolarbiblioteca.repository;

import com.biblioteca.escolarbiblioteca.model.Libro;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LibroRepository extends JpaRepository<Libro, Long> {
    
    List<Libro> findByNombreContainingIgnoreCase(String nombre);
    
    List<Libro> findByCategoriaIgnoreCase(String categoria);
    
    @Query("SELECT l FROM Libro l WHERE " +
           "LOWER(l.nombre) LIKE LOWER(CONCAT('%', :termino, '%')) OR " +
           "LOWER(l.categoria) LIKE LOWER(CONCAT('%', :termino, '%'))")
    List<Libro> buscarPorTermino(@Param("termino") String termino);
    
    boolean existsByCodigoLibro(String codigoLibro);
}