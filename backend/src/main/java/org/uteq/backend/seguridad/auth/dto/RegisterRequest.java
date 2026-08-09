package org.uteq.backend.seguridad.auth.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Datos para dar de alta una cuenta.
 *
 * cedula, correo y fechaNacimiento son obligatorios porque la
 * reestructuracion los convirtio en columnas NOT NULL de
 * seguridad.personas. Mientras no estuvieron en este DTO, /api/auth/registro
 * fallaba siempre con 500 al violar la restriccion de la base de datos.
 */
@Schema(name = "RegisterRequest", description = "Datos para crear una cuenta de usuario con rol USER")
public record RegisterRequest(
        @NotBlank @Size(min = 2, max = 100)
        @Schema(example = "Juan", description = "Nombre de la persona")
        String nombre,
        @NotBlank @Size(min = 2, max = 100)
        @Schema(example = "Perez", description = "Apellido de la persona")
        String apellido,
        @NotBlank @Pattern(regexp = "\\d{10}", message = "La cedula debe tener 10 digitos")
        @Schema(example = "1723456789", description = "Cedula de identidad (10 digitos)")
        String cedula,
        @NotBlank @Email @Size(max = 200)
        @Schema(example = "juan.perez@example.com")
        String correo,
        @NotNull @Past @JsonFormat(pattern = "yyyy-MM-dd")
        @Schema(example = "2005-04-12", format = "date", description = "Fecha de nacimiento (yyyy-MM-dd)")
        LocalDate fechaNacimiento,
        @NotBlank @Email @Size(max = 50)
        @Schema(example = "jperez")
        String username,
        @NotBlank @Size(min = 6)
        @Schema(example = "Clave123456*", format = "password", writeOnly = true)
        String password

) {}
