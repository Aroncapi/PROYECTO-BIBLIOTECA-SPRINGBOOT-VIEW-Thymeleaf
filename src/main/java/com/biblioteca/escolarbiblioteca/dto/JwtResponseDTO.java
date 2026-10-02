package com.biblioteca.escolarbiblioteca.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class JwtResponseDTO {
    private String token;
    private String type = "Bearer";
    private Long id;
    private String email;
    private String rol;
    
    public JwtResponseDTO(String token, Long id, String email, String rol) {
        this.token = token;
        this.id = id;
        this.email = email;
        this.rol = rol;
    }
}