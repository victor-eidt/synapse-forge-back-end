package synapseforge.crud.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
//Deals with Exceptions
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(RuntimeException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleRuntimeException(RuntimeException ex) {
        logger.error("Bad Request - RuntimeException: {}", ex.getMessage(), ex);
        return ex.getMessage();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleValidationException(MethodArgumentNotValidException ex) {
        StringBuilder errors = new StringBuilder("Validation failed: ");
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            errors.append(error.getField()).append(" - ").append(error.getDefaultMessage()).append("; ");
        }
        logger.error("Bad Request - Validation errors: {}", errors.toString(), ex);
        return errors.toString();
    }

    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String handleIllegalArgumentException(IllegalArgumentException ex) {
        logger.error("Bad Request - IllegalArgumentException: {}", ex.getMessage(), ex);
        return ex.getMessage();
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGeneralException(Exception ex) {
        logger.error("Internal Server Error: {}", ex.getMessage(), ex);
        return "An unexpected error occurred";
    }
}