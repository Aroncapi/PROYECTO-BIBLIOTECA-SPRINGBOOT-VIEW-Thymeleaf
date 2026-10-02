package com.biblioteca.escolarbiblioteca.controller;

import com.biblioteca.escolarbiblioteca.dto.JwtResponseDTO;
import com.biblioteca.escolarbiblioteca.dto.LoginRequestDTO;
import com.biblioteca.escolarbiblioteca.dto.UsuarioDTO;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.RolRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import com.biblioteca.escolarbiblioteca.security.JwtUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UsuarioRepository usuarioRepository;
    private final RolRepository rolRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    @PostMapping("/login")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequestDTO loginRequest) {
        log.info("Intento de login para email: {}", loginRequest.getEmail());
        
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(loginRequest.getEmail(), loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String jwt = jwtUtils.generateJwtToken(authentication);

        Usuario usuario = usuarioRepository.findByEmail(loginRequest.getEmail())
                .orElseThrow();

        log.info("Login exitoso para: {}", loginRequest.getEmail());

        return ResponseEntity.ok(new JwtResponseDTO(
                jwt,
                usuario.getId(),
                usuario.getEmail(),
                usuario.getRol().getNombre().name()
        ));
    }

    @PostMapping("/register/alumno")
    public ResponseEntity<?> registerAlumno(@Valid @RequestBody UsuarioDTO usuarioDTO) {
        log.info("Intentando registrar alumno con email: {}", usuarioDTO.getEmail());
        
        // Validar que el email no exista
        if (usuarioRepository.existsByEmail(usuarioDTO.getEmail())) {
            log.warn("Email ya registrado: {}", usuarioDTO.getEmail());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "El email ya está registrado"));
        }

        // Validar que los campos obligatorios para alumno estén presentes
        if (usuarioDTO.getNivel() == null || usuarioDTO.getGrado() == null || usuarioDTO.getSeccion() == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Los campos nivel, grado y sección son obligatorios para alumnos"));
        }

        try {
            // Obtener el rol ALUMNO
            Rol alumnoRol = rolRepository.findByNombre(Rol.ERol.ALUMNO)
                    .orElseThrow(() -> new RuntimeException("Error: Rol ALUMNO no encontrado"));

            // Crear el alumno (el rol se asigna automáticamente, no viene del DTO)
            Usuario alumno = Usuario.builder()
                    .nombre(usuarioDTO.getNombre())
                    .apellido(usuarioDTO.getApellido())
                    .email(usuarioDTO.getEmail())
                    .password(passwordEncoder.encode(usuarioDTO.getPassword()))
                    .rol(alumnoRol)  // Asignamos el rol ALUMNO explícitamente
                    .nivel(Usuario.Nivel.valueOf(usuarioDTO.getNivel()))
                    .grado(usuarioDTO.getGrado())
                    .seccion(usuarioDTO.getSeccion())
                    .build();

            Usuario savedUser = usuarioRepository.save(alumno);
            log.info("Alumno registrado exitosamente con ID: {}", savedUser.getId());

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(new UsuarioDTO(savedUser));

        } catch (IllegalArgumentException e) {
            log.error("Error en valores del alumno: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Valor inválido para nivel (debe ser PRIMARIA o SECUNDARIA)"));
        } catch (Exception e) {
            log.error("Error al registrar alumno: {}", e.getMessage());
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Error al registrar: " + e.getMessage()));
        }
    }

    @GetMapping("/check-email")
    public ResponseEntity<?> checkEmail(@RequestParam String email) {
        boolean exists = usuarioRepository.existsByEmail(email);
        return ResponseEntity.ok(Map.of(
            "email", email,
            "disponible", !exists
        ));
    }
}