package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;

/**
 * Edição do próprio perfil (PUT /users/me). Não tem e-mail nem papel: e-mail só muda pelo fluxo
 * com confirmação (solicitar-mudanca-email). Campo nulo = não mexer.
 */
@Getter
@Setter
@Schema(description = "Edição do próprio perfil; para trocar a senha informe senhaAtual e senha")
public class PerfilUpdateRequestDTO {

    @Schema(description = "Nome completo", example = "Mariana Costa Silva")
    private String nome;

    @Schema(description = "CPF do usuário", example = "529.982.247-25")
    private String cpf;

    @Schema(description = "Telefone com DDD", example = "(41) 99876-5432")
    private String telefone;

    // Obrigatória quando `senha` (a nova) vier preenchida.
    @Schema(description = "Senha atual (obrigatória para trocar a senha)", example = "Senha@2026")
    private String senhaAtual;

    @Schema(description = "Nova senha", example = "NovaSenha@2026")
    private String senha;
}
