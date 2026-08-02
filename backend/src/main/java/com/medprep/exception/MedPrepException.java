package com.medprep.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class MedPrepException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;

    public MedPrepException(String message, HttpStatus status, String errorCode) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
    }

    public static MedPrepException notFound(String resource, String id) {
        return new MedPrepException(
                resource + " con id '" + id + "' no encontrado",
                HttpStatus.NOT_FOUND,
                "RESOURCE_NOT_FOUND"
        );
    }

    public static MedPrepException forbidden() {
        return new MedPrepException(
                "No tienes permisos para realizar esta acción",
                HttpStatus.FORBIDDEN,
                "FORBIDDEN"
        );
    }

    public static MedPrepException badRequest(String message) {
        return new MedPrepException(message, HttpStatus.BAD_REQUEST, "BAD_REQUEST");
    }

    public static MedPrepException conflict(String message) {
        return new MedPrepException(message, HttpStatus.CONFLICT, "CONFLICT");
    }
}
