package com.biblioteca.escolarbiblioteca.dto;

import com.biblioteca.escolarbiblioteca.model.Usuario;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class UsuarioDTO {
    
    private Long id;
    
    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 50, message = "El nombre debe tener entre 2 y 50 caracteres")
    private String nombre;
    
    @NotBlank(message = "El apellido es obligatorio")
    @Size(min = 2, max = 50, message = "El apellido debe tener entre 2 y 50 caracteres")
    private String apellido;
    
    @NotBlank(message = "El email es obligatorio")
    @Email(message = "Formato de email inválido")
    private String email;
    
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    private String password;
    
    // El rol ahora es opcional porque en registro de alumno no se envía
    private String rol;
    
    // Campos para alumnos
    private String nivel;
    private Integer grado;
    private String seccion;
    
    public UsuarioDTO() {}
    
    public UsuarioDTO(Usuario usuario) {
        this.id = usuario.getId();
        this.nombre = usuario.getNombre();
        this.apellido = usuario.getApellido();
        this.email = usuario.getEmail();
        this.rol = usuario.getRol().getNombre().name();
        
        if (usuario.getNivel() != null) {
            this.nivel = usuario.getNivel().name();
            this.grado = usuario.getGrado();
            this.seccion = usuario.getSeccion();
        }
    }
}