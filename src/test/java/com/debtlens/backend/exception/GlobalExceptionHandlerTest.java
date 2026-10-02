package com.debtlens.backend.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class GlobalExceptionHandlerTest {
    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void missingEndpointIsNotReportedAsAnInternalServerError() {
        var response = handler.handleMissingEndpoint(mock(NoResourceFoundException.class));
        assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
        assertTrue(response.getBody().get("message").toString().contains("backend is updated"));
    }

    @Test
    void unsupportedDeleteMethodHasAnAccurateStatus() {
        var response = handler.handleUnsupportedMethod(new HttpRequestMethodNotSupportedException("DELETE"));
        assertEquals(HttpStatus.METHOD_NOT_ALLOWED, response.getStatusCode());
    }
}
