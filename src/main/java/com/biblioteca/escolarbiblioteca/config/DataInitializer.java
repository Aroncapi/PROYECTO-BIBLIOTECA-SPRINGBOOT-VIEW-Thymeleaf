package com.biblioteca.escolarbiblioteca.config;

import com.biblioteca.escolarbiblioteca.model.Libro;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.LibroRepository;
import com.biblioteca.escolarbiblioteca.repository.RolRepository;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final RolRepository rolRepository;
    private final UsuarioRepository usuarioRepository;
    private final LibroRepository libroRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("========== INICIANDO INICIALIZACIÓN DE DATOS ==========");
        
        // 1. Crear roles primero
        crearRoles();
        
        // 2. Crear usuario admin
        crearUsuarioAdmin();
        
        // 3. Crear usuario alumno de prueba
        crearUsuarioAlumnoPrueba();
        
        // 4. Crear libros de ejemplo
        crearLibrosEjemplo();
        
        // 5. Verificar todo
        verificarDatos();
        
        log.info("========== INICIALIZACIÓN DE DATOS COMPLETADA ==========");
    }

    private void crearRoles() {
        if (rolRepository.count() == 0) {
            log.info("Creando roles...");
            Rol adminRol = new Rol(null, Rol.ERol.ADMIN);
            Rol alumnoRol = new Rol(null, Rol.ERol.ALUMNO);
            
            rolRepository.saveAll(Arrays.asList(adminRol, alumnoRol));
            log.info("Roles creados: ADMIN y ALUMNO");
        } else {
            log.info("Los roles ya existen, total: {}", rolRepository.count());
        }
    }

    private void crearUsuarioAdmin() {
        Rol adminRol = rolRepository.findByNombre(Rol.ERol.ADMIN)
            .orElseThrow(() -> new RuntimeException("Error: Rol ADMIN no encontrado"));
        
        // Verificar si ya existe el admin
        if (!usuarioRepository.existsByEmail("admin@biblioteca.com")) {
            log.info("Creando usuario administrador...");
            
            String passwordEncriptada = passwordEncoder.encode("admin123");
            log.info("Contraseña 'admin123' encriptada: {}", passwordEncriptada);
            
            Usuario admin = Usuario.builder()
                    .nombre("Admin")
                    .apellido("Principal")
                    .email("admin@biblioteca.com")
                    .password(passwordEncriptada)
                    .rol(adminRol)
                    .build();
            
            usuarioRepository.save(admin);
            log.info("✅ Usuario ADMIN creado: admin@biblioteca.com / admin123");
        } else {
            log.info("El usuario ADMIN ya existe, verificando contraseña...");
            Usuario admin = usuarioRepository.findByEmail("admin@biblioteca.com").get();
            
            // Verificar si la contraseña es correcta
            boolean passwordCorrecta = passwordEncoder.matches("admin123", admin.getPassword());
            
            if (!passwordCorrecta) {
                log.warn("⚠️ La contraseña del admin es incorrecta, actualizando...");
                admin.setPassword(passwordEncoder.encode("admin123"));
                usuarioRepository.save(admin);
                log.info("✅ Contraseña de ADMIN actualizada correctamente");
            } else {
                log.info("✅ La contraseña del ADMIN es correcta");
            }
        }
    }

    private void crearUsuarioAlumnoPrueba() {
        Rol alumnoRol = rolRepository.findByNombre(Rol.ERol.ALUMNO)
            .orElseThrow(() -> new RuntimeException("Error: Rol ALUMNO no encontrado"));
        
        if (!usuarioRepository.existsByEmail("alumno.prueba@biblioteca.com")) {
            log.info("Creando usuario alumno de prueba...");
            
            Usuario alumno = Usuario.builder()
                    .nombre("Alumno")
                    .apellido("Prueba")
                    .email("alumno.prueba@biblioteca.com")
                    .password(passwordEncoder.encode("alumno123"))
                    .rol(alumnoRol)
                    .nivel(Usuario.Nivel.SECUNDARIA)
                    .grado(3)
                    .seccion("A")
                    .build();
            
            usuarioRepository.save(alumno);
            log.info("✅ Usuario ALUMNO creado: alumno.prueba@biblioteca.com / alumno123");
        }
    }

    private void crearLibrosEjemplo() {
        if (libroRepository.count() == 0) {
            log.info("Cargando libros al sistema...");
            
            List<Libro> libros = Arrays.asList(
                Libro.builder()
                    .nombre("Cien años de soledad")
                    .categoria("Novela")
                    .codigoLibro("LIB-001")
                    .stockTotal(5)
                    .stockDisponible(5)
                    .build(),
                Libro.builder()
                    .nombre("El principito")
                    .categoria("Infantil")
                    .codigoLibro("LIB-002")
                    .stockTotal(3)
                    .stockDisponible(3)
                    .build(),
                Libro.builder()
                    .nombre("Don Quijote de la Mancha")
                    .categoria("Clásico")
                    .codigoLibro("LIB-003")
                    .stockTotal(2)
                    .stockDisponible(2)
                    .build(),
                Libro.builder()
                    .nombre("Harry Potter y la piedra filosofal")
                    .categoria("Fantasía")
                    .codigoLibro("LIB-004")
                    .stockTotal(4)
                    .stockDisponible(4)
                    .build(),
                Libro.builder()
                    .nombre("1984")
                    .categoria("Ciencia ficción")
                    .codigoLibro("LIB-005")
                    .stockTotal(3)
                    .stockDisponible(3)
                    .build(),
                Libro.builder()
                    .nombre("Rayuela")
                    .categoria("Novela")
                    .codigoLibro("LIB-006")
                    .stockTotal(2)
                    .stockDisponible(2)
                    .build(),
                Libro.builder()
                    .nombre("El hobbit")
                    .categoria("Fantasía")
                    .codigoLibro("LIB-007")
                    .stockTotal(3)
                    .stockDisponible(3)
                    .build()
            );
            
            libroRepository.saveAll(libros);
            log.info("✅ {} libros de ejemplo creados", libros.size());
        } else {
            log.info("Los libros ya existen, total: {}", libroRepository.count());
        }
    }

    private void verificarDatos() {
        log.info("========== VERIFICACIÓN DE DATOS ==========");
        
        // Verificar roles
        long totalRoles = rolRepository.count();
        log.info("Roles en BD: {}", totalRoles);
        rolRepository.findAll().forEach(rol -> 
            log.info("  - Rol: {}", rol.getNombre()));
        
        // Verificar usuarios
        long totalUsuarios = usuarioRepository.count();
        log.info("Usuarios en BD: {}", totalUsuarios);
        usuarioRepository.findAll().forEach(usuario -> {
            boolean passwordValida = passwordEncoder.matches("admin123", usuario.getPassword()) ||
                                    passwordEncoder.matches("alumno123", usuario.getPassword());
            log.info("  - Usuario: {}, Rol: {}, Password válida: {}", 
                usuario.getEmail(), 
                usuario.getRol().getNombre(),
                passwordValida ? "✅" : "❌");
        });
        
        // Verificar libros
        long totalLibros = libroRepository.count();
        log.info("Libros en BD: {}", totalLibros);
        
        // Verificar admin específicamente
        usuarioRepository.findByEmail("admin@biblioteca.com").ifPresentOrElse(
            admin -> {
                boolean adminPasswordValida = passwordEncoder.matches("admin123", admin.getPassword());
                log.info("✅ ADMIN encontrado: {}", admin.getEmail());
                log.info("   - Contraseña admin123: {}", adminPasswordValida ? "✅ CORRECTA" : "❌ INCORRECTA");
                
                if (!adminPasswordValida) {
                    log.error("⚠️ ¡URGENTE! La contraseña del admin es incorrecta. Actualizando...");
                    admin.setPassword(passwordEncoder.encode("admin123"));
                    usuarioRepository.save(admin);
                    log.info("✅ Contraseña de ADMIN actualizada");
                }
            },
            () -> log.error("❌ ADMIN no encontrado en la base de datos")
        );
        
        log.info("========== VERIFICACIÓN COMPLETADA ==========");
    }
}