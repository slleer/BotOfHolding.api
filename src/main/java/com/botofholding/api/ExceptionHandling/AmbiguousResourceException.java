package com.botofholding.api.ExceptionHandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT) // 409: The request is valid, but can't be processed due to the state of the resources.
public class AmbiguousResourceException extends RuntimeException {
    public AmbiguousResourceException(String message) {
        super(message);
    }
}