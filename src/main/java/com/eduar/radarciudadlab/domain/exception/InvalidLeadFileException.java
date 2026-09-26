package com.eduar.radarciudadlab.domain.exception;

public class InvalidLeadFileException extends RuntimeException {

    public InvalidLeadFileException(String message) {
        super(message);
    }

    public InvalidLeadFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
