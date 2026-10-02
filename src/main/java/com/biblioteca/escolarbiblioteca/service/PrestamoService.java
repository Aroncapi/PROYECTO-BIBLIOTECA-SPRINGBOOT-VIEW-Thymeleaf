package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.exception.ResourceNotFoundException;
import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.model.Prestamo;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import com.biblioteca.escolarbiblioteca.repository.PrestamoRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrestamoService {

    private final PrestamoRepository prestamoRepository;
    private final LibroRepository libroRepository;  // Agregar esto
    private final UsuarioService usuarioService;

    public List<PrestamoDTO> getAllPrestamos() {
        return prestamoRepository.findAll().stream()
                .map(prestamo -> {
                    PrestamoDTO dto = new PrestamoDTO(prestamo);
                    if (prestamo.getEstado() == Prestamo.EstadoPrestamo.ACTIVO) {
                        long diasRestantes = ChronoUnit.DAYS.between(
                            LocalDateTime.now(), 
                            prestamo.getFechaDevolucionEstimada()
                        );
                        dto.setDiasRestantes((int) diasRestantes);
                    } else if (prestamo.getEstado() == Prestamo.EstadoPrestamo.VENCIDO) {
                        long diasRetraso = ChronoUnit.DAYS.between(
                            prestamo.getFechaDevolucionEstimada(),
                            LocalDateTime.now()
                        );
                        dto.setDiasRetraso((int) diasRetraso);
                    }
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public PrestamoDTO solicitarPrestamo(Usuario usuario, Long libroId) {
        // Validar reglas de negocio
        validarReglasPrestamo(usuario);
        
        // CORREGIDO: Obtener la entidad Libro directamente del repositorio
        Libro libro = libroRepository.findById(libroId)
                .orElseThrow(() -> new ResourceNotFoundException("Libro no encontrado con id: " + libroId));
        
        // Verificar disponibilidad
        if (libro.getStockDisponible() <= 0) {
            throw new RuntimeException("El libro no está disponible para préstamo");
        }
        
        // Crear préstamo
        Prestamo prestamo = Prestamo.builder()
                .usuario(usuario)
                .libro(libro)
                .fechaPrestamo(LocalDateTime.now())
                .fechaDevolucionEstimada(LocalDateTime.now().plusDays(7))
                .estado(Prestamo.EstadoPrestamo.ACTIVO)
                .build();
        
        prestamoRepository.save(prestamo);
        
        // Actualizar stock directamente
        libro.setStockDisponible(libro.getStockDisponible() - 1);
        libroRepository.save(libro);
        
        log.info("Préstamo creado: {} - {} - {}", usuario.getEmail(), libro.getNombre(), prestamo.getId());
        
        return new PrestamoDTO(prestamo);
    }

    private void validarReglasPrestamo(Usuario usuario) {
        // Verificar si el usuario es alumno
        if (usuario.getRol().getNombre() != Rol.ERol.ALUMNO) {
            throw new RuntimeException("Solo los alumnos pueden solicitar préstamos");
        }
        
        // Verificar si tiene préstamos vencidos
        long prestamosVencidos = prestamoRepository.countByUsuarioAndEstado(
                usuario, Prestamo.EstadoPrestamo.VENCIDO);
        if (prestamosVencidos > 0) {
            throw new RuntimeException("No puede solicitar préstamos porque tiene préstamos vencidos");
        }
        
        // Verificar límite de préstamos activos (máximo 3)
        long prestamosActivos = prestamoRepository.countByUsuarioAndEstado(
                usuario, Prestamo.EstadoPrestamo.ACTIVO);
        if (prestamosActivos >= 3) {
            throw new RuntimeException("Ha alcanzado el límite máximo de 3 préstamos activos");
        }
    }

    @Transactional
    public PrestamoDTO devolverLibro(Usuario usuario, Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + prestamoId));
        
        // Verificar que el préstamo pertenezca al usuario
        if (!prestamo.getUsuario().getId().equals(usuario.getId())) {
            throw new RuntimeException("No puede devolver un préstamo que no le pertenece");
        }
        
        // Verificar que el préstamo esté activo
        if (prestamo.getEstado() != Prestamo.EstadoPrestamo.ACTIVO) {
            throw new RuntimeException("El préstamo no está activo");
        }
        
        // Actualizar préstamo
        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstado(Prestamo.EstadoPrestamo.DEVUELTO);
        prestamoRepository.save(prestamo);
        
        // Actualizar stock
        Libro libro = prestamo.getLibro();
        libro.setStockDisponible(libro.getStockDisponible() + 1);
        libroRepository.save(libro);
        
        log.info("Libro devuelto: {}", prestamoId);
        
        return new PrestamoDTO(prestamo);
    }

    @Transactional
    public PrestamoDTO devolverLibroAdmin(Long prestamoId) {
        Prestamo prestamo = prestamoRepository.findById(prestamoId)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + prestamoId));
        
        if (prestamo.getEstado() != Prestamo.EstadoPrestamo.ACTIVO && 
            prestamo.getEstado() != Prestamo.EstadoPrestamo.VENCIDO) {
            throw new RuntimeException("El préstamo no puede ser devuelto");
        }
        
        prestamo.setFechaDevolucionReal(LocalDateTime.now());
        prestamo.setEstado(Prestamo.EstadoPrestamo.DEVUELTO);
        prestamoRepository.save(prestamo);
        
        Libro libro = prestamo.getLibro();
        libro.setStockDisponible(libro.getStockDisponible() + 1);
        libroRepository.save(libro);
        
        log.info("Admin devolvió libro: {}", prestamoId);
        
        return new PrestamoDTO(prestamo);
    }

    public List<PrestamoDTO> getPrestamosByUsuario(Usuario usuario) {
        return prestamoRepository.findByUsuario(usuario).stream()
                .map(PrestamoDTO::new)
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> getPrestamosActivosByUsuario(Usuario usuario) {
        return prestamoRepository.findByUsuarioAndEstado(usuario, Prestamo.EstadoPrestamo.ACTIVO).stream()
                .map(PrestamoDTO::new)
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> getPrestamosActivos() {
        return prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.ACTIVO).stream()
                .map(prestamo -> {
                    PrestamoDTO dto = new PrestamoDTO(prestamo);
                    long diasRestantes = ChronoUnit.DAYS.between(
                        LocalDateTime.now(), 
                        prestamo.getFechaDevolucionEstimada()
                    );
                    dto.setDiasRestantes((int) diasRestantes);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> getPrestamosVencidos() {
        return prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.VENCIDO).stream()
                .map(prestamo -> {
                    PrestamoDTO dto = new PrestamoDTO(prestamo);
                    long diasRetraso = ChronoUnit.DAYS.between(
                        prestamo.getFechaDevolucionEstimada(),
                        LocalDateTime.now()
                    );
                    dto.setDiasRetraso((int) diasRetraso);
                    return dto;
                })
                .collect(Collectors.toList());
    }

    public PrestamoDTO getPrestamoById(Long id) {
        Prestamo prestamo = prestamoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Préstamo no encontrado con id: " + id));
        return new PrestamoDTO(prestamo);
    }

    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void actualizarPrestamosVencidos() {
        LocalDateTime ahora = LocalDateTime.now();
        int actualizados = prestamoRepository.actualizarPrestamosVencidos(
                Prestamo.EstadoPrestamo.ACTIVO,
                Prestamo.EstadoPrestamo.VENCIDO,
                ahora);
        
        if (actualizados > 0) {
            log.info("Se actualizaron {} préstamos a estado VENCIDO", actualizados);
        }
    }
}