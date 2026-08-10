package org.uteq.backend.common.exception;

import org.springframework.http.HttpStatus;

/**
 * Fallo al consumir un proveedor externo (Bloque 5.1: football-data.org).
 * GlobalExceptionHandler ya captura {@link ApiException} y por herencia
 * tambien esta clase, asi que no requiere un @ExceptionHandler propio.
 * En la practica, FutbolService la atrapa antes de que llegue al handler
 * global y degrada a una respuesta 200 con datos de referencia (fallback),
 * el {@link Motivo} solo se registra en el log.
 */
public class ApiExternaException extends ApiException {

    public enum Motivo {
        TIMEOUT,
        RED,
        RATE_LIMIT,
        NO_AUTORIZADO,
        NO_ENCONTRADO,
        SERVIDOR
    }

    private final Motivo motivo;

    public ApiExternaException(Motivo motivo, HttpStatus status, String mensaje) {
        super(status, mensaje);
        this.motivo = motivo;
    }

    public Motivo getMotivo() {
        return motivo;
    }
}
