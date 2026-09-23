package synapseforge.crud.DTO.User;

import lombok.Getter;
import lombok.Setter;

/**
 * Edição do próprio perfil (PUT /users/me). Não tem e-mail nem papel: e-mail só muda pelo fluxo
 * com confirmação (solicitar-mudanca-email). Campo nulo = não mexer.
 */
@Getter
@Setter
public class PerfilUpdateRequestDTO {

    private String nome;

    private String cpf;

    private String telefone;

    // Obrigatória quando `senha` (a nova) vier preenchida.
    private String senhaAtual;

    private String senha;
}
