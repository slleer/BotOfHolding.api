package com.botofholding.api.ExceptionHandling;


import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpStatus;

import java.util.Date;

@Getter
@Setter
public class ErrorResponse {

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "MM-dd-yyyy hh:mm:ss")
    private Date timestamp;

    private String message;

    private String status;

    private int code;

    private Object data;

    public ErrorResponse() {timestamp = new Date(); }

    public ErrorResponse(HttpStatus httpStatus, String message) {
        this();
        this.message = message;
        this.status = httpStatus.name();
        this.code = httpStatus.value();
    }

    public ErrorResponse(HttpStatus httpStatus, String message, Object data){
        this(httpStatus, message);
        this.data = data;
    }

}
