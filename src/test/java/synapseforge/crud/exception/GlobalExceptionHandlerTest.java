package synapseforge.crud.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void semEquipeViraForbiddenComCodigoEstavelNoCabecalho() {
        ResponseEntity<String> resposta = handler.handleSemEquipeException(new SemEquipeException());

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertEquals("SEM_EQUIPE", resposta.getHeaders().getFirst("X-Codigo-Erro"));
        assertEquals("Crie ou entre em uma equipe para acessar estes dados.", resposta.getBody());
    }
}
