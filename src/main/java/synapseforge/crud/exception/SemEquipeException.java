package synapseforge.crud.exception;

import lombok.Getter;

/**
 * Lançada quando um usuário sem equipe tenta criar, alterar ou excluir dados de negócio.
 * Mapeada para HTTP 403 com o código estável SEM_EQUIPE no cabeçalho X-Codigo-Erro,
 * para o front distinguir "sem equipe" de sessão expirada (que também chega como 403).
 */
@Getter
public class SemEquipeException extends RuntimeException {

    public static final String CODIGO = "SEM_EQUIPE";
    public static final String CABECALHO_CODIGO = "X-Codigo-Erro";

    private final String codigo = CODIGO;

    public SemEquipeException() {
        super("Crie ou entre em uma equipe para acessar estes dados.");
    }
}
