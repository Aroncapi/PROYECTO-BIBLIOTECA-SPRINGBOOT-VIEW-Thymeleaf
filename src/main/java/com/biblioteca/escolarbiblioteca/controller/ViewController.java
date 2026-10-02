package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.LibroDTO;
import com.biblioteca.escolarbiblioteca.dto.PrestamoDTO;
import com.biblioteca.escolarbiblioteca.dto.UsuarioDTO;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.security.UserDetailsImpl;
import com.biblioteca.escolarbiblioteca.service.LibroService;
import com.biblioteca.escolarbiblioteca.service.PrestamoService;
import com.biblioteca.escolarbiblioteca.service.ReporteService;
import com.biblioteca.escolarbiblioteca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequiredArgsConstructor
@Slf4j
public class ViewController {

    private final UsuarioService usuarioService;
    private final LibroService libroService;
    private final PrestamoService prestamoService;
    private final ReporteService reporteService;

    // ========== PÁGINA PRINCIPAL ==========
    
    @GetMapping("/")
    public String home() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Home - Authentication: {}", auth);
        
        if (auth != null && auth.isAuthenticated() && 
            !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            log.debug("Usuario autenticado: {} con roles: {}", userDetails.getUsername(), userDetails.getAuthorities());
            
            if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                log.debug("Redirigiendo a admin dashboard");
                return "redirect:/admin/dashboard";
            } else {
                log.debug("Redirigiendo a alumno catalogo");
                return "redirect:/alumno/catalogo";
            }
        }
        
        log.debug("Usuario no autenticado, redirigiendo a login");
        return "redirect:/login";
    }

    // ========== PÁGINAS PÚBLICAS ==========
    
    @GetMapping("/login")
    public String login() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        log.debug("Login - Authentication: {}", auth);
        
        if (auth != null && auth.isAuthenticated() && 
            !(auth.getPrincipal() instanceof String && auth.getPrincipal().equals("anonymousUser"))) {
            
            UserDetailsImpl userDetails = (UserDetailsImpl) auth.getPrincipal();
            
            if (userDetails.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
                return "redirect:/admin/dashboard";
            } else {
                return "redirect:/alumno/catalogo";
            }
        }
        
        return "auth/login";
    }

    @GetMapping("/registro")
    public String registro() {
        return "auth/register";
    }

    // ========== PÁGINAS DE ADMINISTRADOR ==========
    
    @GetMapping("/admin/dashboard")
    public String dashboard(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        log.debug("Dashboard - UserDetails: {}", userDetails);
        
        if (userDetails == null) {
            log.debug("UserDetails es null, redirigiendo a login");
            return "redirect:/login";
        }
        
        Map<String, Object> stats = reporteService.getDashboardStats();
        model.addAttribute("totalLibros", stats.get("totalLibros"));
        model.addAttribute("totalAlumnos", stats.get("totalAlumnos"));
        model.addAttribute("prestamosActivos", stats.get("prestamosActivos"));
        model.addAttribute("prestamosVencidos", stats.get("prestamosVencidos"));
        model.addAttribute("topLibros", stats.get("topLibros"));
        
        List<PrestamoDTO> ultimosPrestamos = prestamoService.getPrestamosActivos().stream()
                .limit(10)
                .collect(Collectors.toList());
        model.addAttribute("ultimosPrestamos", ultimosPrestamos);
        
        return "admin/dashboard";
    }

    @GetMapping("/admin/usuarios")
    public String usuarios(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        List<UsuarioDTO> usuarios = usuarioService.findAll().stream()
                .map(UsuarioDTO::new)
                .collect(Collectors.toList());
        
        List<UsuarioDTO> alumnos = usuarios.stream()
                .filter(u -> "ALUMNO".equals(u.getRol()))
                .collect(Collectors.toList());
        
        List<UsuarioDTO> admins = usuarios.stream()
                .filter(u -> "ADMIN".equals(u.getRol()))
                .collect(Collectors.toList());
        
        model.addAttribute("alumnos", alumnos);
        model.addAttribute("administradores", admins);
        
        return "admin/usuarios";
    }

    @GetMapping("/admin/libros")
    public String adminLibros(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        model.addAttribute("libros", libroService.findAll());
        return "admin/libros";
    }

    @GetMapping("/admin/prestamos")
    public String adminPrestamos(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null) {
            return "redirect:/login";
        }
        
        List<PrestamoDTO> todos = prestamoService.getAllPrestamos();
        List<PrestamoDTO> activos = prestamoService.getPrestamosActivos();
        List<PrestamoDTO> vencidos = prestamoService.getPrestamosVencidos();
        List<PrestamoDTO> devueltos = todos.stream()
                .filter(p -> "DEVUELTO".equals(p.getEstado()))
                .collect(Collectors.toList());
        
        model.addAttribute("prestamosActivos", activos);
        model.addAttribute("prestamosVencidos", vencidos);
        model.addAttribute("prestamosDevueltos", devueltos);
        
        return "admin/prestamos";
    }

    @GetMapping("/admin/reportes")
    public String reportes(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/login";
        }
        return "admin/reportes";
    }

 // ========== PERFIL DE ADMINISTRADOR ==========
    @GetMapping("/admin/perfil")
    public String adminPerfil(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ADMIN"))) {
            return "redirect:/login";
        }
        
        Usuario usuario = userDetails.getUsuario();
        model.addAttribute("usuario", new UsuarioDTO(usuario));
        
        return "admin/perfil";
    }

    // ========== PÁGINAS DE ALUMNO ==========
    
    @GetMapping("/alumno/catalogo")
    public String catalogo(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ALUMNO"))) {
            return "redirect:/login";
        }
        
        model.addAttribute("libros", libroService.findAll());
        
        Usuario usuario = userDetails.getUsuario();
        List<PrestamoDTO> activos = prestamoService.getPrestamosActivosByUsuario(usuario);
        model.addAttribute("prestamosActivos", activos.size());
        
        return "alumno/catalogo";
    }

    @GetMapping("/alumno/mis-prestamos")
    public String misPrestamos(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ALUMNO"))) {
            return "redirect:/login";
        }
        
        Usuario usuario = userDetails.getUsuario();
        
        List<PrestamoDTO> todos = prestamoService.getPrestamosByUsuario(usuario);
        List<PrestamoDTO> activos = prestamoService.getPrestamosActivosByUsuario(usuario);
        List<PrestamoDTO> devueltos = todos.stream()
                .filter(p -> "DEVUELTO".equals(p.getEstado()))
                .collect(Collectors.toList());
        List<PrestamoDTO> vencidos = todos.stream()
                .filter(p -> "VENCIDO".equals(p.getEstado()))
                .collect(Collectors.toList());
        
        model.addAttribute("todos", todos);
        model.addAttribute("activos", activos);
        model.addAttribute("devueltos", devueltos);
        model.addAttribute("vencidos", vencidos);
        
        return "alumno/mis-prestamos";
    }

    @GetMapping("/alumno/perfil")
    public String alumnoPerfil(Model model, @AuthenticationPrincipal UserDetailsImpl userDetails) {
        if (userDetails == null || userDetails.getAuthorities().stream()
                .noneMatch(a -> a.getAuthority().equals("ROLE_ALUMNO"))) {
            return "redirect:/login";
        }
        
        Usuario usuario = userDetails.getUsuario();
        UsuarioDTO usuarioDTO = new UsuarioDTO(usuario);
        model.addAttribute("usuario", usuarioDTO);
        
        // Estadísticas del alumno
        List<PrestamoDTO> todosPrestamos = prestamoService.getPrestamosByUsuario(usuario);
        List<PrestamoDTO> activos = prestamoService.getPrestamosActivosByUsuario(usuario);
        
        model.addAttribute("totalPrestamos", todosPrestamos.size());
        model.addAttribute("prestamosActivos", activos.size());
        model.addAttribute("prestamosDevueltos", 
                todosPrestamos.stream().filter(p -> "DEVUELTO".equals(p.getEstado())).count());
        
        // Últimos 5 préstamos
        model.addAttribute("ultimosPrestamos", 
                todosPrestamos.stream().limit(5).collect(Collectors.toList()));
        
        return "alumno/perfil";
    }

    // ========== PÁGINAS DE ERROR ==========
    
    @GetMapping("/error/404")
    public String error404() {
        return "error/404";
    }

    @GetMapping("/error/403")
    public String error403() {
        return "error/403";
    }

    @GetMapping("/error/500")
    public String error500() {
        return "error/500";
    }
}