package synapseforge.crud.exception;

/**
 * Registro inexistente (ou de outro usuário, o que para quem pediu dá no mesmo).
 * Mapeada para HTTP 404; sem ela cairia no handler de RuntimeException e viraria 400.
 */
public class RecursoNaoEncontradoException extends RuntimeException {

    public RecursoNaoEncontradoException(String mensagem) {
        super(mensagem);
    }
}
