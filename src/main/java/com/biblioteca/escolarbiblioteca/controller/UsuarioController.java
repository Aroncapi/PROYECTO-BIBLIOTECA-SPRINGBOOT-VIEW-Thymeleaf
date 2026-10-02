package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.UsuarioDTO;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.security.UserDetailsImpl;
import com.biblioteca.escolarbiblioteca.service.UsuarioService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/usuarios")
@RequiredArgsConstructor
@Slf4j
public class UsuarioController {

    private final UsuarioService usuarioService;
    private final PasswordEncoder passwordEncoder;

    /**
     * Obtener perfil del usuario autenticado
     */
    @GetMapping("/perfil")
    public ResponseEntity<?> getPerfil(@AuthenticationPrincipal UserDetailsImpl userDetails) {
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            
            Usuario usuario = userDetails.getUsuario();
            UsuarioDTO usuarioDTO = new UsuarioDTO(usuario);
            
            log.info("Perfil consultado para usuario: {}", usuario.getEmail());
            
            return ResponseEntity.ok(usuarioDTO);
            
        } catch (Exception e) {
            log.error("Error al obtener perfil: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al obtener perfil: " + e.getMessage()));
        }
    }

    /**
     * Actualizar perfil del usuario autenticado
     */
    @PutMapping("/perfil")
    public ResponseEntity<?> actualizarPerfil(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody UsuarioDTO usuarioDTO) {
        
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            
            Usuario usuario = userDetails.getUsuario();
            log.info("Actualizando perfil para usuario: {}", usuario.getEmail());
            
            // Validar email único si cambió
            if (!usuario.getEmail().equals(usuarioDTO.getEmail())) {
                if (usuarioService.existsByEmail(usuarioDTO.getEmail())) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "El email ya está registrado por otro usuario"));
                }
            }
            
            // Actualizar datos básicos
            usuario.setNombre(usuarioDTO.getNombre());
            usuario.setApellido(usuarioDTO.getApellido());
            usuario.setEmail(usuarioDTO.getEmail());
            
            // Actualizar contraseña si se proporciona
            if (usuarioDTO.getPassword() != null && !usuarioDTO.getPassword().isEmpty()) {
                usuario.setPassword(passwordEncoder.encode(usuarioDTO.getPassword()));
                log.info("Contraseña actualizada para usuario: {}", usuario.getEmail());
            }
            
            // Si es alumno, actualizar campos específicos
            if ("ALUMNO".equals(usuarioDTO.getRol()) && usuarioDTO.getNivel() != null) {
                usuario.setNivel(Usuario.Nivel.valueOf(usuarioDTO.getNivel()));
                usuario.setGrado(usuarioDTO.getGrado());
                usuario.setSeccion(usuarioDTO.getSeccion());
            }
            
            Usuario usuarioActualizado = usuarioService.save(usuario);
            log.info("Perfil actualizado exitosamente para usuario: {}", usuario.getEmail());
            
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Perfil actualizado correctamente");
            response.put("usuario", new UsuarioDTO(usuarioActualizado));
            
            return ResponseEntity.ok(response);
            
        } catch (IllegalArgumentException e) {
            log.error("Error de validación al actualizar perfil: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Datos inválidos: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error al actualizar perfil: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al actualizar perfil: " + e.getMessage()));
        }
    }

    /**
     * Cambiar contraseña
     */
    @PostMapping("/cambiar-password")
    public ResponseEntity<?> cambiarPassword(
            @AuthenticationPrincipal UserDetailsImpl userDetails,
            @RequestBody Map<String, String> passwords) {
        
        try {
            if (userDetails == null) {
                return ResponseEntity.status(401)
                        .body(Map.of("error", "Usuario no autenticado"));
            }
            
            String passwordActual = passwords.get("passwordActual");
            String passwordNueva = passwords.get("passwordNueva");
            
            if (passwordActual == null || passwordNueva == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Debe proporcionar contraseña actual y nueva"));
            }
            
            Usuario usuario = userDetails.getUsuario();
            
            // Verificar contraseña actual
            if (!passwordEncoder.matches(passwordActual, usuario.getPassword())) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "La contraseña actual es incorrecta"));
            }
            
            // Actualizar contraseña
            usuario.setPassword(passwordEncoder.encode(passwordNueva));
            usuarioService.save(usuario);
            
            log.info("Contraseña cambiada para usuario: {}", usuario.getEmail());
            
            return ResponseEntity.ok(Map.of("message", "Contraseña cambiada correctamente"));
            
        } catch (Exception e) {
            log.error("Error al cambiar contraseña: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al cambiar contraseña: " + e.getMessage()));
        }
    }

    /**
     * Verificar disponibilidad de email
     */
    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        try {
            boolean exists = usuarioService.existsByEmail(email);
            return ResponseEntity.ok(Map.of(
                "email", email,
                "disponible", !exists
            ));
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint de salud
     */
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        return ResponseEntity.ok(Map.of(
            "status", "OK",
            "timestamp", java.time.LocalDateTime.now().toString()
        ));
    }
}