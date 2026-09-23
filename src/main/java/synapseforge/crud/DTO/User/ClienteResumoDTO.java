package synapseforge.crud.DTO.User;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Dados mínimos de um cliente para a equipe vincular a um pedido (sem cpf/telefone/role).
@Getter
@AllArgsConstructor
public class ClienteResumoDTO {

    private String id;
    private String nome;
    private String email;
}
