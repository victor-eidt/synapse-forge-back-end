package synapseforge.crud.DTO.Admin;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "Usuário visto pelo painel de administração")
public class AdminUserResponseDTO {

    @Schema(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02")
    private String id;
    @Schema(description = "Nome completo", example = "Mariana Costa")
    private String nome;
    @Schema(description = "E-mail do usuário", example = "mariana.costa@email.com")
    private String email;
    @Schema(description = "CPF do usuário", example = "529.982.247-25")
    private String cpf;
    @Schema(description = "Telefone com DDD", example = "(41) 99876-5432")
    private String telefone;
    @Schema(description = "Papel do usuário", example = "CLIENTE")
    private String role;
    @Schema(description = "ID da equipe (oficina) do usuário", example = "6704a1c2e4b0f81a2c3d4e06")
    private String equipeId;
    @Schema(description = "Função exibida para o integrante na página da equipe", example = "Pintora")
    private String funcaoVisual;
    @Schema(description = "Conta ativa; false significa desativada pelo admin", example = "true")
    private boolean ativo;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora da última atualização", example = "2026-10-08T09:15:42")
    private LocalDateTime atualizadoEm;
}

