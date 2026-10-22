package synapseforge.crud.DTO.User;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Schema(description = "Usuário")
public class UserResponseDTO {

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

}