package com.botofholding.api.ExceptionHandling;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidSearchScopeException extends RuntimeException{
    public InvalidSearchScopeException(String message) {
        super(message);
    }

    public InvalidSearchScopeException(String message, Throwable cause) {
        super(message, cause);
    }
}
