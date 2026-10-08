package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Dados mínimos de um cliente para a equipe vincular a um pedido (sem cpf/telefone/role).
@Getter
@AllArgsConstructor
@Schema(description = "Dados mínimos de um cliente para vincular a um pedido")
public class ClienteResumoDTO {

    @Schema(description = "ID do cliente", example = "6704a1c2e4b0f81a2c3d4e02")
    private String id;
    @Schema(description = "Nome do cliente", example = "Mariana Costa")
    private String nome;
    @Schema(description = "E-mail do usuário", example = "mariana.costa@email.com")
    private String email;
}
