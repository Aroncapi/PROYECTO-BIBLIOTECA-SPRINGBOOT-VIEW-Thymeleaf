package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.security.UserDetailsImpl;
import com.biblioteca.escolarbiblioteca.service.PrestamoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/alumno")
@PreAuthorize("hasRole('ALUMNO')")
@RequiredArgsConstructor
public class AlumnoController {

    private final PrestamoService prestamoService;

    @GetMapping("/mis-prestamos")
    public ResponseEntity<List<PrestamoDTO>> getMisPrestamos(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Usuario usuario = userDetails.getUsuario();
        List<PrestamoDTO> prestamos = prestamoService.getPrestamosByUsuario(usuario);
        return ResponseEntity.ok(prestamos);
    }

    @GetMapping("/mis-prestamos/activos")
    public ResponseEntity<List<PrestamoDTO>> getMisPrestamosActivos(
            @AuthenticationPrincipal UserDetailsImpl userDetails) {
        Usuario usuario = userDetails.getUsuario();
        List<PrestamoDTO> prestamos = prestamoService.getPrestamosActivosByUsuario(usuario);
        return ResponseEntity.ok(prestamos);
    }

    @PostMapping("/prestamos/solicitar/{libroId}")
    public ResponseEntity<?> solicitarPrestamo(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long libroId) {
        Usuario usuario = userDetails.getUsuario();
        PrestamoDTO prestamo = prestamoService.solicitarPrestamo(usuario, libroId);
        return ResponseEntity.ok(prestamo);
    }

    @PutMapping("/prestamos/devolver/{prestamoId}")
    public ResponseEntity<?> devolverLibro(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @PathVariable Long prestamoId) {
        Usuario usuario = userDetails.getUsuario();
        PrestamoDTO prestamo = prestamoService.devolverLibro(usuario, prestamoId);
        return ResponseEntity.ok(prestamo);
    }
}