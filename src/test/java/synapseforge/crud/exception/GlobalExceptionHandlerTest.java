package synapseforge.crud.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.bson.BsonDocument;
import org.bson.BsonDouble;
import org.bson.BsonInt32;
import org.bson.BsonString;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import com.mongodb.MongoCommandException;
import com.mongodb.ServerAddress;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void semEquipeViraForbiddenComCodigoEstavelNoCabecalho() {
        ResponseEntity<String> resposta = handler.handleSemEquipeException(new SemEquipeException());

        assertEquals(HttpStatus.FORBIDDEN, resposta.getStatusCode());
        assertEquals("SEM_EQUIPE", resposta.getHeaders().getFirst("X-Codigo-Erro"));
        assertEquals("Crie ou entre em uma equipe para acessar estes dados.", resposta.getBody());
    }

    @Test
    void recursoNaoEncontradoDevolveAMensagem() {
        String corpo = handler.handleRecursoNaoEncontradoException(
                new RecursoNaoEncontradoException("Notificação não encontrada"));

        assertEquals("Notificação não encontrada", corpo);
    }

    @Test
    void documentoRejeitadoPeloJsonSchemaViraUnprocessableSemExporOMongo() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "Document failed validation", erroMongo(121, "Document failed validation"));

        ResponseEntity<String> resposta = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, resposta.getStatusCode());
        assertEquals("Os dados não respeitam as regras de validação do banco.", resposta.getBody());
    }

    @Test
    void outraViolacaoDeIntegridadeContinuaBadRequest() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException(
                "outro erro", erroMongo(2, "outro erro"));

        ResponseEntity<String> resposta = handler.handleDataIntegrityViolationException(ex);

        assertEquals(HttpStatus.BAD_REQUEST, resposta.getStatusCode());
        assertEquals("outro erro", resposta.getBody());
    }

    private static MongoCommandException erroMongo(int codigo, String mensagem) {
        BsonDocument resposta = new BsonDocument("ok", new BsonDouble(0))
                .append("code", new BsonInt32(codigo))
                .append("errmsg", new BsonString(mensagem));
        return new MongoCommandException(resposta, new ServerAddress());
    }
}
