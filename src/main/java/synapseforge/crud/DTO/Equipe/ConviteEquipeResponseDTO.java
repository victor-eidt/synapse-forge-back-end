package synapseforge.crud.DTO.Equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusConviteEquipe;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "Convite para um usuário entrar na equipe")
public class ConviteEquipeResponseDTO {

    @Schema(description = "ID do convite", example = "6704a1c2e4b0f81a2c3d4e10")
    private String id;
    @Schema(description = "ID da equipe (oficina) do usuário", example = "6704a1c2e4b0f81a2c3d4e06")
    private String equipeId;
    @Schema(description = "Nome da equipe", example = "Oficina Synapse Curitiba")
    private String equipeNome;
    @Schema(description = "ID do gerente que convidou", example = "6704a1c2e4b0f81a2c3d4e0b")
    private String gerenteId;
    @Schema(description = "Nome do gerente que convidou", example = "Fernanda Lima")
    private String gerenteNome;
    @Schema(description = "ID do usuário convidado", example = "6704a1c2e4b0f81a2c3d4e02")
    private String usuarioId;
    @Schema(description = "Nome do usuário convidado", example = "Mariana Costa")
    private String usuarioNome;
    @Schema(description = "Situação do convite", example = "PENDENTE")
    private StatusConviteEquipe status;
    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora em que o convite expira", example = "2026-10-14T14:32:10")
    private LocalDateTime expiraEm;
}