package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.LibroDTO;
import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.dto.UsuarioDTO;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.RolRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import com.biblioteca.escolarbiblioteca.service.LibroService;
import com.biblioteca.escolarbiblioteca.service.PrestamoService;
import com.biblioteca.escolarbiblioteca.service.ReporteService;
import com.biblioteca.escolarbiblioteca.service.UsuarioService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
@RequiredArgsConstructor
@Slf4j
public class AdminController {

    private final UsuarioService usuarioService;
    private final LibroService libroService;
    private final PrestamoService prestamoService;
    private final ReporteService reporteService;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;

    // ============================
    // GESTIÓN DE USUARIOS
    // ============================

    /**
     * Obtener todos los usuarios
     */
    @GetMapping("/usuarios")
    public ResponseEntity<List<UsuarioDTO>> getAllUsuarios() {
        log.info("Solicitando lista de todos los usuarios");
        List<UsuarioDTO> usuarios = usuarioService.findAll().stream()
                .map(UsuarioDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Obtener usuario por ID
     */
    @GetMapping("/usuarios/{id}")
    public ResponseEntity<UsuarioDTO> getUsuarioById(@PathVariable Long id) {
        log.info("Solicitando usuario con ID: {}", id);
        Usuario usuario = usuarioService.findById(id);
        return ResponseEntity.ok(new UsuarioDTO(usuario));
    }

    /**
     * Obtener usuarios por rol
     */
    @GetMapping("/usuarios/rol/{rol}")
    public ResponseEntity<List<UsuarioDTO>> getUsuariosByRol(@PathVariable String rol) {
        log.info("Solicitando usuarios con rol: {}", rol);
        Rol.ERol rolEnum = Rol.ERol.valueOf(rol);
        Rol rolEntity = rolRepository.findByNombre(rolEnum)
                .orElseThrow(() -> new RuntimeException("Rol no encontrado"));
        
        List<UsuarioDTO> usuarios = usuarioRepository.findByRol(rolEntity).stream()
                .map(UsuarioDTO::new)
                .collect(Collectors.toList());
        return ResponseEntity.ok(usuarios);
    }

    /**
     * Crear nuevo administrador
     */
    @PostMapping("/usuarios/admin")
    public ResponseEntity<?> createAdmin(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("Creando nuevo administrador con email: {}", usuarioDTO.getEmail());
        
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El email ya está registrado"));
        }

        Rol adminRol = rolRepository.findByNombre(Rol.ERol.ADMIN)
                .orElseThrow(() -> new RuntimeException("Error: Rol ADMIN no encontrado"));

        Usuario admin = Usuario.builder()
                .nombre(usuarioDTO.getNombre())
                .apellido(usuarioDTO.getApellido())
                .email(usuarioDTO.getEmail())
                .password(passwordEncoder.encode(usuarioDTO.getPassword()))
                .rol(adminRol)
                .build();

        Usuario savedUser = usuarioService.save(admin);
        log.info("Administrador creado exitosamente con ID: {}", savedUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsuarioDTO(savedUser));
    }

    /**
     * Crear nuevo alumno
     */
    @PostMapping("/usuarios/alumno")
    public ResponseEntity<?> createAlumno(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("Creando nuevo alumno con email: {}", usuarioDTO.getEmail());
        
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El email ya está registrado"));
        }

        Rol alumnoRol = rolRepository.findByNombre(Rol.ERol.ALUMNO)
                .orElseThrow(() -> new RuntimeException("Error: Rol ALUMNO no encontrado"));

        Usuario alumno = Usuario.builder()
                .nombre(usuarioDTO.getNombre())
                .apellido(usuarioDTO.getApellido())
                .email(usuarioDTO.getEmail())
                .password(passwordEncoder.encode(usuarioDTO.getPassword()))
                .rol(alumnoRol)
                .nivel(usuarioDTO.getNivel() != null ? Usuario.Nivel.valueOf(usuarioDTO.getNivel()) : null)
                .grado(usuarioDTO.getGrado())
                .seccion(usuarioDTO.getSeccion())
                .build();

        Usuario savedUser = usuarioService.save(alumno);
        log.info("Alumno creado exitosamente con ID: {}", savedUser.getId());
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new UsuarioDTO(savedUser));
    }

    /**
     * Actualizar usuario
     */
    @PutMapping("/usuarios/{id}")
    public ResponseEntity<?> updateUsuario(@PathVariable Long id, @Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("Actualizando usuario con ID: {}", id);
        
        Usuario usuario = usuarioService.findById(id);
        
        usuario.setNombre(usuarioDTO.getNombre());
        usuario.setApellido(usuarioDTO.getApellido());
        
        // Actualizar email solo si es diferente
        if (!usuario.getEmail().equals(usuarioDTO.getEmail())) {
            if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "El email ya está registrado por otro usuario"));
            }
            usuario.setEmail(usuarioDTO.getEmail());
        }
        
        // Actualizar contraseña si se proporciona
        if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
            usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
        }
        
        // Actualizar campos específicos de alumno
        if (usuario.getRol().getNombre() == Rol.ERol.ALUMNO) {
            usuario.setNivel(usuarioDTO.getNivel() != null ? 
                    Usuario.Nivel.valueOf(usuarioDTO.getNivel()) : null);
            usuario.setGrado(usuarioDTO.getGrado());
            usuario.setSeccion(usuarioDTO.getSeccion());
        }
        
        Usuario updatedUser = usuarioService.save(usuario);
        log.info("Usuario actualizado exitosamente con ID: {}", id);
        
        return ResponseEntity.ok(new UsuarioDTO(updatedUser));
    }

    /**
     * Eliminar usuario
     */
    @DeleteMapping("/usuarios/{id}")
    public ResponseEntity<?> deleteUsuario(@PathVariable Long id) {
        log.info("Eliminando usuario con ID: {}", id);
        
        Usuario usuario = usuarioService.findById(id);
        
        // Verificar si el usuario tiene préstamos activos
        long prestamosActivos = prestamoService.getPrestamosActivosByUsuario(usuario).size();
        if (prestamosActivos > 0) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "No se puede eliminar el usuario porque tiene préstamos activos"));
        }
        
        usuarioService.deleteById(id);
        log.info("Usuario eliminado exitosamente con ID: {}", id);
        
        return ResponseEntity.ok(Map.of("message", "Usuario eliminado correctamente"));
    }

    // ============================
    // GESTIÓN DE LIBROS
    // ============================

    /**
     * Crear nuevo libro
     */
    @PostMapping("/libros")
    public ResponseEntity<?> createLibro(@Valid @RequestBody LibroDTO libroDTO) {
        log.info("Creando nuevo libro con código: {}", libroDTO.getCodigoLibro());
        
        try {
            LibroDTO savedLibro = libroService.save(libroDTO);
            return ResponseEntity.status(HttpStatus.CREATED).body(savedLibro);
        } catch (Exception e) {
            log.error("Error al crear libro: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualizar libro
     */
    @PutMapping("/libros/{id}")
    public ResponseEntity<?> updateLibro(@PathVariable Long id, @Valid @RequestBody LibroDTO libroDTO) {
        log.info("Actualizando libro con ID: {}", id);
        
        try {
            LibroDTO updatedLibro = libroService.update(id, libroDTO);
            return ResponseEntity.ok(updatedLibro);
        } catch (Exception e) {
            log.error("Error al actualizar libro: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Eliminar libro
     */
    @DeleteMapping("/libros/{id}")
    public ResponseEntity<?> deleteLibro(@PathVariable Long id) {
        log.info("Eliminando libro con ID: {}", id);
        
        try {
            libroService.deleteById(id);
            return ResponseEntity.ok(Map.of("message", "Libro eliminado correctamente"));
        } catch (Exception e) {
            log.error("Error al eliminar libro: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================
    // GESTIÓN DE PRÉSTAMOS
    // ============================

    /**
     * Obtener todos los préstamos
     */
    @GetMapping("/prestamos")
    public ResponseEntity<List<PrestamoDTO>> getAllPrestamos() {
        log.info("Solicitando todos los préstamos");
        return ResponseEntity.ok(prestamoService.getAllPrestamos());
    }

    /**
     * Obtener préstamos activos
     */
    @GetMapping("/prestamos/activos")
    public ResponseEntity<List<PrestamoDTO>> getPrestamosActivos() {
        log.info("Solicitando préstamos activos");
        return ResponseEntity.ok(prestamoService.getPrestamosActivos());
    }

    /**
     * Obtener préstamos vencidos
     */
    @GetMapping("/prestamos/vencidos")
    public ResponseEntity<List<PrestamoDTO>> getPrestamosVencidos() {
        log.info("Solicitando préstamos vencidos");
        return ResponseEntity.ok(prestamoService.getPrestamosVencidos());
    }

    /**
     * Obtener préstamos por rango de fechas
     */
    @GetMapping("/prestamos/rango")
    public ResponseEntity<List<PrestamoDTO>> getPrestamosByRangoFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fechaFin) {
        log.info("Solicitando préstamos entre {} y {}", fechaInicio, fechaFin);
        // Implementar método en servicio
        return ResponseEntity.ok(prestamoService.getAllPrestamos().stream()
                .filter(p -> {
                    LocalDateTime fechaPrestamo = LocalDateTime.parse(p.getFechaPrestamoFormatted(), 
                            java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
                    return !fechaPrestamo.isBefore(fechaInicio) && !fechaPrestamo.isAfter(fechaFin);
                })
                .collect(Collectors.toList()));
    }

    /**
     * Obtener préstamos por usuario
     */
    @GetMapping("/prestamos/usuario/{usuarioId}")
    public ResponseEntity<List<PrestamoDTO>> getPrestamosByUsuario(@PathVariable Long usuarioId) {
        log.info("Solicitando préstamos del usuario: {}", usuarioId);
        Usuario usuario = usuarioService.findById(usuarioId);
        return ResponseEntity.ok(prestamoService.getPrestamosByUsuario(usuario));
    }

    /**
     * Obtener detalle de préstamo
     */
    @GetMapping("/prestamos/{id}")
    public ResponseEntity<PrestamoDTO> getPrestamoById(@PathVariable Long id) {
        log.info("Solicitando detalle del préstamo: {}", id);
        return ResponseEntity.ok(prestamoService.getPrestamoById(id));
    }

    /**
     * Registrar devolución de préstamo (admin)
     */
    @PutMapping("/prestamos/devolver/{id}")
    public ResponseEntity<?> devolverPrestamo(@PathVariable Long id) {
        log.info("Registrando devolución del préstamo: {}", id);
        
        try {
            PrestamoDTO prestamo = prestamoService.devolverLibroAdmin(id);
            return ResponseEntity.ok(Map.of(
                "message", "Devolución registrada correctamente",
                "prestamo", prestamo
            ));
        } catch (Exception e) {
            log.error("Error al registrar devolución: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Actualizar préstamos vencidos (tarea manual)
     */
    @PostMapping("/prestamos/actualizar-vencidos")
    public ResponseEntity<?> actualizarPrestamosVencidos() {
        log.info("Ejecutando actualización manual de préstamos vencidos");
        
        try {
            prestamoService.actualizarPrestamosVencidos();
            return ResponseEntity.ok(Map.of("message", "Préstamos vencidos actualizados correctamente"));
        } catch (Exception e) {
            log.error("Error al actualizar préstamos vencidos: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    // ============================
    // DASHBOARD Y ESTADÍSTICAS
    // ============================

    /**
     * Obtener estadísticas del dashboard
     */
    @GetMapping("/dashboard")
    public ResponseEntity<Map<String, Object>> getDashboardStats() {
        log.info("Solicitando estadísticas del dashboard");
        
        Map<String, Object> stats = new HashMap<>();
        
        try {
            stats.put("totalLibros", libroService.findAll().size());
            stats.put("totalUsuarios", usuarioService.findAll().size());
            stats.put("totalAlumnos", usuarioRepository.findByRol(
                    rolRepository.findByNombre(Rol.ERol.ALUMNO).orElseThrow()).size());
            stats.put("totalAdmins", usuarioRepository.findByRol(
                    rolRepository.findByNombre(Rol.ERol.ADMIN).orElseThrow()).size());
            
            List<PrestamoDTO> prestamosActivos = prestamoService.getPrestamosActivos();
            List<PrestamoDTO> prestamosVencidos = prestamoService.getPrestamosVencidos();
            
            stats.put("prestamosActivos", prestamosActivos.size());
            stats.put("prestamosVencidos", prestamosVencidos.size());
            stats.put("totalPrestamos", prestamoService.getAllPrestamos().size());
            
            // Calcular porcentajes
            int totalPrestamos = prestamoService.getAllPrestamos().size();
            stats.put("porcentajeActivos", totalPrestamos > 0 ? 
                    (prestamosActivos.size() * 100 / totalPrestamos) : 0);
            stats.put("porcentajeVencidos", totalPrestamos > 0 ? 
                    (prestamosVencidos.size() * 100 / totalPrestamos) : 0);
            
            // Top libros prestados
            stats.put("topLibros", reporteService.getTopLibrosPrestados());
            
            // Últimos 10 préstamos
            stats.put("ultimosPrestamos", prestamoService.getAllPrestamos().stream()
                    .limit(10)
                    .collect(Collectors.toList()));
            
        } catch (Exception e) {
            log.error("Error al obtener estadísticas del dashboard: {}", e.getMessage());
            stats.put("error", e.getMessage());
        }
        
        return ResponseEntity.ok(stats);
    }

    // ============================
    // REPORTES
    // ============================

    /**
     * Obtener reporte de préstamos activos
     */
    @GetMapping("/reportes/prestamos-activos")
    public ResponseEntity<Map<String, Object>> getReportePrestamosActivos() {
        log.info("Generando reporte de préstamos activos");
        
        Map<String, Object> reporte = new HashMap<>();
        List<PrestamoDTO> activos = prestamoService.getPrestamosActivos();
        
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("totalRegistros", activos.size());
        reporte.put("prestamos", activos);
        reporte.put("titulo", "Reporte de Préstamos Activos");
        
        // Agrupar por usuario
        Map<String, Long> prestamosPorUsuario = activos.stream()
                .collect(Collectors.groupingBy(
                    PrestamoDTO::getUsuarioNombre, 
                    Collectors.counting()
                ));
        reporte.put("prestamosPorUsuario", prestamosPorUsuario);
        
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtener reporte de préstamos vencidos
     */
    @GetMapping("/reportes/prestamos-vencidos")
    public ResponseEntity<Map<String, Object>> getReportePrestamosVencidos() {
        log.info("Generando reporte de préstamos vencidos");
        
        Map<String, Object> reporte = new HashMap<>();
        List<PrestamoDTO> vencidos = prestamoService.getPrestamosVencidos();
        
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("totalRegistros", vencidos.size());
        reporte.put("prestamos", vencidos);
        reporte.put("titulo", "Reporte de Préstamos Vencidos");
        
        // Calcular días de retraso promedio
        double promedioRetraso = vencidos.stream()
                .mapToInt(PrestamoDTO::getDiasRetraso)
                .average()
                .orElse(0.0);
        reporte.put("promedioDiasRetraso", Math.round(promedioRetraso * 100.0) / 100.0);
        
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtener reporte de préstamos por rango de fechas
     */
    @GetMapping("/reportes/historial")
    public ResponseEntity<Map<String, Object>> getReporteHistorial(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String fechaInicio,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) String fechaFin) {
        
        log.info("Generando reporte histórico del {} al {}", fechaInicio, fechaFin);
        
        LocalDateTime inicio = LocalDateTime.parse(fechaInicio + "T00:00:00");
        LocalDateTime fin = LocalDateTime.parse(fechaFin + "T23:59:59");
        
        List<PrestamoDTO> prestamos = prestamoService.getAllPrestamos().stream()
                .filter(p -> {
                    // CORREGIDO: usar el objeto PrestamoDTO directamente
                    LocalDateTime fechaPrestamo = p.getFechaPrestamo();
                    return !fechaPrestamo.isBefore(inicio) && !fechaPrestamo.isAfter(fin);
                })
                .collect(Collectors.toList());
        
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("fechaInicio", fechaInicio);
        reporte.put("fechaFin", fechaFin);
        reporte.put("totalRegistros", prestamos.size());
        reporte.put("prestamos", prestamos);
        reporte.put("titulo", "Reporte Histórico de Préstamos");
        
        long activos = prestamos.stream()
                .filter(p -> "ACTIVO".equals(p.getEstado()))
                .count();
        long devueltos = prestamos.stream()
                .filter(p -> "DEVUELTO".equals(p.getEstado()))
                .count();
        long vencidos = prestamos.stream()
                .filter(p -> "VENCIDO".equals(p.getEstado()))
                .count();
        
        reporte.put("estadisticas", Map.of(
            "activos", activos,
            "devueltos", devueltos,
            "vencidos", vencidos
        ));
        
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtener top libros más prestados
     */
    @GetMapping("/reportes/top-libros")
    public ResponseEntity<Map<String, Object>> getTopLibros(
            @RequestParam(defaultValue = "10") int limite) {
        
        log.info("Generando top {} libros más prestados", limite);
        
        List<Map<String, Object>> topLibros = reporteService.getTopLibrosPrestados().stream()
                .limit(limite)
                .collect(Collectors.toList());
        
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("limite", limite);
        reporte.put("topLibros", topLibros);
        reporte.put("titulo", "Top Libros Más Prestados");
        
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtener reporte de usuarios más activos
     */
    @GetMapping("/reportes/usuarios-activos")
    public ResponseEntity<Map<String, Object>> getUsuariosMasActivos(
            @RequestParam(defaultValue = "10") int limite) {
        
        log.info("Generando top {} usuarios más activos", limite);
        
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("limite", limite);
        reporte.put("titulo", "Usuarios Más Activos");
        
        List<PrestamoDTO> todosPrestamos = prestamoService.getAllPrestamos();
        
        Map<String, Long> prestamosPorUsuario = todosPrestamos.stream()
                .collect(Collectors.groupingBy(
                    PrestamoDTO::getUsuarioNombre,
                    Collectors.counting()
                ));
        
        List<Map<String, Object>> usuariosTop = prestamosPorUsuario.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(limite)
                .map(entry -> {
                    Map<String, Object> item = new HashMap<>();
                    item.put("usuario", entry.getKey());
                    item.put("totalPrestamos", entry.getValue());
                    return item;
                })
                .collect(Collectors.toList());
        
        reporte.put("usuarios", usuariosTop);
        
        return ResponseEntity.ok(reporte);
    }

    /**
     * Obtener estadísticas por categoría
     */
    @GetMapping("/reportes/estadisticas-categorias")
    public ResponseEntity<Map<String, Object>> getEstadisticasPorCategoria() {
        log.info("Generando estadísticas por categoría");
        
        // CORREGIDO: libroService.findAll() ya retorna List<LibroDTO>
        List<LibroDTO> libros = libroService.findAll();
        
        // Agrupar libros por categoría
        Map<String, Long> librosPorCategoria = libros.stream()
                .collect(Collectors.groupingBy(
                    LibroDTO::getCategoria,
                    Collectors.counting()
                ));
        
        // Calcular préstamos por categoría
        Map<String, Long> prestamosPorCategoria = new HashMap<>();
        List<PrestamoDTO> todosPrestamos = prestamoService.getAllPrestamos();
        
        for (PrestamoDTO prestamo : todosPrestamos) {
            String categoria = libros.stream()
                    .filter(l -> l.getId().equals(prestamo.getLibroId()))
                    .findFirst()
                    .map(LibroDTO::getCategoria)
                    .orElse("Sin categoría");
            
            prestamosPorCategoria.merge(categoria, 1L, Long::sum);
        }
        
        Map<String, Object> reporte = new HashMap<>();
        reporte.put("fechaGeneracion", LocalDateTime.now());
        reporte.put("titulo", "Estadísticas por Categoría");
        reporte.put("librosPorCategoria", librosPorCategoria);
        reporte.put("prestamosPorCategoria", prestamosPorCategoria);
        
        return ResponseEntity.ok(reporte);
    }

    /**
     * Préstamos registrados por mes (últimos N meses) para gráficos
     */
    @GetMapping("/reportes/prestamos-por-mes")
    public ResponseEntity<Map<String, Object>> getPrestamosPorMes(
            @RequestParam(name = "meses", defaultValue = "6") int meses) {
        log.info("Generando gráfico de préstamos por mes (últimos {} meses)", meses);
        Map<String, Object> resultado = reporteService.getPrestamosPorMes(meses);
        resultado.put("fechaGeneracion", LocalDateTime.now());
        resultado.put("titulo", "Préstamos por Mes");
        return ResponseEntity.ok(resultado);
    }

    /**
     * Endpoint de salud (health check)
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "timestamp", LocalDateTime.now().toString(),
            "service", "Admin API"
        ));
    }
}