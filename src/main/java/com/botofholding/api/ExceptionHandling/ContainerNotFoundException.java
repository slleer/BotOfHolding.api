package com.botofholding.api.ExceptionHandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ContainerNotFoundException extends RuntimeException {

    public ContainerNotFoundException(String message) { super(message); }

    public ContainerNotFoundException(String message, Throwable cause) { super(message, cause); }

    // A helpful constructor for common "not found" scenarios
    public ContainerNotFoundException(String fieldName, Object fieldValue, String ownerId, String ownerType) {
        super(String.format("Container not found with %s : '%s' for owner type: '%s' and owner id: '%s'",
                fieldName,
                fieldValue,
                ownerType,
                ownerId));
    }
}
