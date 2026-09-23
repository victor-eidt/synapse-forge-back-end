package synapseforge.crud.exception;

import org.junit.jupiter.api.Test;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void bancoIndisponivelDeveRetornar503() throws NoSuchMethodException {
        DataAccessResourceFailureException exception =
                new DataAccessResourceFailureException("MongoDB indisponível");

        assertEquals(
                "Serviço temporariamente indisponível. Tente novamente mais tarde.",
                handler.handleDataAccessException(exception)
        );
        assertEquals(
                HttpStatus.SERVICE_UNAVAILABLE,
                GlobalExceptionHandler.class
                        .getMethod("handleDataAccessException",
                                org.springframework.dao.DataAccessException.class)
                        .getAnnotation(ResponseStatus.class)
                        .value()
        );
    }
}
