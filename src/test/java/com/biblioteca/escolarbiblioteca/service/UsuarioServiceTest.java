package com.biblioteca.escolarbiblioteca.service;

import com.biblioteca.escolarbiblioteca.exception.ResourceNotFoundException;
import com.biblioteca.escolarbiblioteca.model.Rol;
import com.biblioteca.escolarbiblioteca.model.Usuario;
import com.biblioteca.escolarbiblioteca.repository.UsuarioRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * Pruebas unitarias de UsuarioService.
 */
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    private Usuario alumno() {
        return Usuario.builder()
                .id(1L)
                .nombre("Ana")
                .apellido("Torres")
                .email("ana@biblioteca.com")
                .password("123456")
                .rol(new Rol(2L, Rol.ERol.ALUMNO))
                .nivel(Usuario.Nivel.PRIMARIA)
                .grado(3)
                .seccion("A")
                .build();
    }

    @Test
    void findById_inexistente_lanzaResourceNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.findById(99L));
    }

    @Test
    void findByEmail_inexistente_lanzaResourceNotFound() {
        when(usuarioRepository.findByEmail("no@existe.com")).thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> usuarioService.findByEmail("no@existe.com"));

        assertTrue(ex.getMessage().contains("no@existe.com"));
    }

    @Test
    void save_guardaYDevuelveElUsuario() {
        Usuario alumno = alumno();
        when(usuarioRepository.save(any(Usuario.class))).thenReturn(alumno);

        Usuario guardado = usuarioService.save(alumno);

        assertEquals("ana@biblioteca.com", guardado.getEmail());
        verify(usuarioRepository).save(alumno);
    }

    @Test
    void update_alumno_actualizaDatosYCamposDeAlumno() {
        Usuario existente = alumno();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario cambios = Usuario.builder()
                .nombre("Ana Maria")
                .apellido("Torres Ruiz")
                .email("anamaria@biblioteca.com")
                .password("nuevaPass")
                .nivel(Usuario.Nivel.SECUNDARIA)
                .grado(5)
                .seccion("B")
                .build();

        Usuario actualizado = usuarioService.update(1L, cambios);

        assertEquals("Ana Maria", actualizado.getNombre());
        assertEquals("anamaria@biblioteca.com", actualizado.getEmail());
        assertEquals("nuevaPass", actualizado.getPassword());
        assertEquals(Usuario.Nivel.SECUNDARIA, actualizado.getNivel());
        assertEquals(5, actualizado.getGrado());
        assertEquals("B", actualizado.getSeccion());
    }

    @Test
    void update_passwordVacia_noCambiaLaPassword() {
        Usuario existente = alumno();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(existente));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario cambios = Usuario.builder()
                .nombre("Ana")
                .apellido("Torres")
                .email("ana@biblioteca.com")
                .password("") // vacía: no debe reemplazar
                .build();

        Usuario actualizado = usuarioService.update(1L, cambios);

        assertEquals("123456", actualizado.getPassword());
    }

    @Test
    void update_noModificaCamposDeAlumnoSiEsAdmin() {
        Usuario admin = Usuario.builder()
                .id(2L)
                .nombre("Admin")
                .apellido("Sistema")
                .email("admin@biblioteca.com")
                .password("admin123")
                .rol(new Rol(1L, Rol.ERol.ADMIN))
                .build();
        when(usuarioRepository.findById(2L)).thenReturn(Optional.of(admin));
        when(usuarioRepository.save(any(Usuario.class))).thenAnswer(inv -> inv.getArgument(0));

        Usuario cambios = Usuario.builder()
                .nombre("Admin Editado")
                .apellido("Sistema")
                .email("admin@biblioteca.com")
                .nivel(Usuario.Nivel.SECUNDARIA) // no debe copiarse a un ADMIN
                .grado(2)
                .seccion("Z")
                .build();

        Usuario actualizado = usuarioService.update(2L, cambios);

        assertEquals("Admin Editado", actualizado.getNombre());
        assertNull(actualizado.getNivel());
        assertNull(actualizado.getGrado());
    }

    @Test
    void deleteById_usuarioInexistente_lanzaResourceNotFound() {
        when(usuarioRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> usuarioService.deleteById(99L));
        verify(usuarioRepository, never()).delete(any());
    }

    @Test
    void deleteById_existente_loElimina() {
        Usuario alumno = alumno();
        when(usuarioRepository.findById(1L)).thenReturn(Optional.of(alumno));

        usuarioService.deleteById(1L);

        verify(usuarioRepository).delete(alumno);
    }

    @Test
    void existsByEmail_delegaEnElRepositorio() {
        when(usuarioRepository.existsByEmail("ana@biblioteca.com")).thenReturn(true);

        assertTrue(usuarioService.existsByEmail("ana@biblioteca.com"));
    }
}
