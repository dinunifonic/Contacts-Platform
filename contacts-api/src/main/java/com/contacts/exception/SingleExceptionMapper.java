package com.contacts.exception;

import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

import java.sql.SQLIntegrityConstraintViolationException;
import java.util.stream.Collectors;


//Provides special REST behavior
@Provider
public class SingleExceptionMapper implements ExceptionMapper<Exception> {

    @Context
    UriInfo uriInfo;

    @Override
    public Response toResponse(Exception exception) {
        int statusCode;
        String title;

        Throwable cause = exception;
        boolean isDuplicate = false;
        while (cause != null) {
            if (cause instanceof SQLIntegrityConstraintViolationException) {
                isDuplicate = true;
                break;
            }
            cause = cause.getCause();
        }

        if (exception instanceof NotFoundException) {
            statusCode = 404;
            title = "Not Found";

        } else if (exception instanceof IllegalArgumentException) {
            statusCode = 400;
            title = "Invalid Request";

        } else if (exception instanceof ConstraintViolationException) {
            statusCode = 400;
            title = "Validation Failed";

        } else if (isDuplicate) {
                statusCode = 409;
                title = "Conflict: Duplicate Entry";
        } else{
            statusCode = 500;
            title = "Internal Server Error";
        }

        String detail = exception.getMessage();
        if (exception instanceof ConstraintViolationException constraintViolationException) {
            detail = constraintViolationException.getConstraintViolations()
                    .stream()
                    .map(violation -> {
                        String field = violation.getPropertyPath().toString();
                        field = field.substring(field.lastIndexOf('.') + 1);

                        return field + ": " + violation.getMessage();
                    })
                    .collect(Collectors.joining(", "));
        }
        if (isDuplicate) {
            detail = "A resource with the same unique value already exists";
        }

        ErrorDescription error = new ErrorDescription();
        error.setType("about:blank");
        error.setTitle(title);
        error.setStatus(statusCode);
        error.setDetail(detail);
        error.setInstance(uriInfo.getRequestUri().getPath());

        return Response.status(statusCode)
                .type("application/problem+json")
                .entity(error)
                .build();
    }


}
