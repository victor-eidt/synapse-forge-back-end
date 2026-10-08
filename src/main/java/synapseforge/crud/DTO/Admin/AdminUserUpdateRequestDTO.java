package synapseforge.crud.DTO.Admin;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.Role;

@Getter
@Setter
@Schema(description = "Edição parcial de usuário pelo admin: campos omitidos (null) não são alterados")
public class AdminUserUpdateRequestDTO {

    @Schema(description = "Nome completo", example = "Carlos Mendes")
    private String nome;
    @Schema(description = "E-mail", example = "carlos.mendes@email.com")
    private String email;
    @Schema(description = "CPF do usuário", example = "529.982.247-25")
    private String cpf;
    @Schema(description = "Telefone com DDD", example = "(41) 99876-5432")
    private String telefone;
    @Schema(description = "Novo papel do usuário", example = "TECNICO")
    private Role role;
    @Schema(description = "ID da equipe (oficina) do usuário", example = "6704a1c2e4b0f81a2c3d4e06")
    private String equipeId;
    @Schema(description = "Função exibida para o integrante na página da equipe", example = "Pintora")
    private String funcaoVisual;
    @Schema(description = "Conta ativa; false significa desativada pelo admin", example = "true")
    private Boolean ativo;
    @Schema(description = "Nova senha (opcional)", example = "NovaSenha@2026")
    private String senha;
}