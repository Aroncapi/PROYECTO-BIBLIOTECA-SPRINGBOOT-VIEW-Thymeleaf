package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.model.Prestamo;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import com.biblioteca.escolarbiblioteca.repository.PrestamoRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReporteService {

    private static final String[] MESES = {
            "Ene", "Feb", "Mar", "Abr", "May", "Jun",
            "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"
    };

    private final PrestamoRepository prestamoRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;

    public Map<String, Object> getDashboardStats() {
        Map<String, Object> stats = new HashMap<>();
        
        // Totales
        stats.put("totalLibros", libroRepository.count());
        
        // Obtener el rol ALUMNO de manera segura
        long totalAlumnos = usuarioRepository.findAll().stream()
                .filter(u -> u.getRol() != null && u.getRol().getNombre() == Rol.ERol.ALUMNO)
                .count();
        stats.put("totalAlumnos", totalAlumnos);
        
        // Préstamos activos
        List<Prestamo> prestamosActivos = prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.ACTIVO);
        stats.put("prestamosActivos", prestamosActivos.size());
        
        // Préstamos vencidos
        List<Prestamo> prestamosVencidos = prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.VENCIDO);
        stats.put("prestamosVencidos", prestamosVencidos.size());
        
        // Préstamos devueltos
        long prestamosDevueltos = prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.DEVUELTO).size();
        stats.put("prestamosDevueltos", prestamosDevueltos);
        
        // Top 5 libros más prestados
        stats.put("topLibros", getTopLibrosPrestados());
        
        return stats;
    }

    public List<Map<String, Object>> getTopLibrosPrestados() {
        List<Object[]> results = prestamoRepository.findTopLibrosPrestados();
        
        return results.stream()
                .limit(5)
                .map(result -> {
                    Map<String, Object> item = new HashMap<>();
                    Long libroId = (Long) result[0];
                    Long total = (Long) result[1];
                    
                    Libro libro = libroRepository.findById(libroId).orElse(null);
                    if (libro != null) {
                        item.put("libroId", libroId);
                        item.put("libroNombre", libro.getNombre());
                        item.put("libroCodigo", libro.getCodigoLibro());
                        item.put("categoria", libro.getCategoria());
                        item.put("totalPrestamos", total);
                    }
                    return item;
                })
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> getPrestamosActivos() {
        return prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.ACTIVO).stream()
                .map(PrestamoDTO::new)
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> getPrestamosVencidos() {
        return prestamoRepository.findByEstado(Prestamo.EstadoPrestamo.VENCIDO).stream()
                .map(PrestamoDTO::new)
                .collect(Collectors.toList());
    }

    public List<PrestamoDTO> getPrestamosPorRangoFechas(String fechaInicioStr, String fechaFinStr) {
        LocalDateTime fechaInicio = LocalDateTime.parse(fechaInicioStr + "T00:00:00");
        LocalDateTime fechaFin = LocalDateTime.parse(fechaFinStr + "T23:59:59");
        
        return prestamoRepository.findAll().stream()
                .filter(p -> !p.getFechaPrestamo().isBefore(fechaInicio) && 
                            !p.getFechaPrestamo().isAfter(fechaFin))
                .map(PrestamoDTO::new)
                .collect(Collectors.toList());
    }

    /**
     * Cantidad de préstamos registrados por mes en los últimos N meses.
     * Devuelve labels (nombres de mes) y data (conteos), rellenando con 0 los meses sin datos.
     */
    public Map<String, Object> getPrestamosPorMes(int meses) {
        int limite = Math.max(1, Math.min(meses, 24));
        YearMonth actual = YearMonth.now();
        YearMonth desde = actual.minusMonths(limite - 1L);

        List<Object[]> filas = prestamoRepository.contarPrestamosPorMes(desde.atDay(1).atStartOfDay());
        Map<String, Long> porClave = new HashMap<>();
        for (Object[] fila : filas) {
            int anio = ((Number) fila[0]).intValue();
            int mes = ((Number) fila[1]).intValue();
            long total = ((Number) fila[2]).longValue();
            porClave.put(anio + "-" + mes, total);
        }

        List<String> labels = new ArrayList<>();
        List<Long> data = new ArrayList<>();
        for (int i = limite - 1; i >= 0; i--) {
            YearMonth ym = actual.minusMonths(i);
            labels.add(MESES[ym.getMonthValue() - 1]);
            data.add(porClave.getOrDefault(ym.getYear() + "-" + ym.getMonthValue(), 0L));
        }

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("labels", labels);
        resultado.put("data", data);
        return resultado;
    }

    /**
     * Usuarios con más préstamos, ordenados de mayor a menor.
     */
    public List<Map<String, Object>> getTopUsuariosActivos(int limite) {
        Map<Long, Map<String, Object>> porUsuario = new java.util.LinkedHashMap<>();

        prestamoRepository.findAll().forEach(prestamo -> {
            Long id = prestamo.getUsuario().getId();
            Map<String, Object> item = porUsuario.computeIfAbsent(id, k -> {
                Map<String, Object> nuevo = new HashMap<>();
                nuevo.put("usuarioId", id);
                nuevo.put("usuario", prestamo.getUsuario().getNombre() + " " + prestamo.getUsuario().getApellido());
                nuevo.put("email", prestamo.getUsuario().getEmail());
                nuevo.put("totalPrestamos", 0L);
                return nuevo;
            });
            item.put("totalPrestamos", (Long) item.get("totalPrestamos") + 1);
        });

        int max = Math.max(1, limite);
        return porUsuario.values().stream()
                .sorted((a, b) -> Long.compare((Long) b.get("totalPrestamos"), (Long) a.get("totalPrestamos")))
                .limit(max)
                .collect(Collectors.toList());
    }

    /**
     * Estadísticas de libros y préstamos agrupados por categoría.
     */
    public Map<String, Object> getEstadisticasCategorias() {
        Map<String, Long> librosPorCategoria = libroRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        libro -> libro.getCategoria() != null ? libro.getCategoria() : "Sin categoría",
                        Collectors.counting()));

        Map<String, Long> prestamosPorCategoria = prestamoRepository.findAll().stream()
                .collect(Collectors.groupingBy(
                        prestamo -> prestamo.getLibro().getCategoria() != null
                                ? prestamo.getLibro().getCategoria() : "Sin categoría",
                        Collectors.counting()));

        Map<String, Object> resultado = new HashMap<>();
        resultado.put("librosPorCategoria", librosPorCategoria);
        resultado.put("prestamosPorCategoria", prestamosPorCategoria);
        return resultado;
    }
}