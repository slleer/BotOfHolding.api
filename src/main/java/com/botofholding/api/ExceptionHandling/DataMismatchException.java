package com.botofholding.api.ExceptionHandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class DataMismatchException extends RuntimeException{



    public DataMismatchException(String message) {
        super(message);
    }

    public DataMismatchException(String message, Throwable cause) {
        super(message, cause);
    }

    public DataMismatchException(Object suppliedObject, String fieldName, String suppliedValue) {
        super(String.format("Data Mismatch with supplied %s for %s object : '%s' for Class: {}: '%s'", fieldName, suppliedValue, suppliedObject.getClass(), suppliedObject));
    }
}
