package synapseforge.crud.DTO.Equipe;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Schema(description = "Equipe (oficina)")
public class EquipeResponseDTO {

    @Schema(description = "ID da equipe", example = "6704a1c2e4b0f81a2c3d4e06")
    private String id;
    @Schema(description = "Nome da equipe", example = "Oficina Synapse Curitiba")
    private String nome;
    @Schema(description = "ID do gerente dono da equipe", example = "6704a1c2e4b0f81a2c3d4e0b")
    private String gerenteId;

    // Imagens convertidas para Base64
    @Schema(description = "Foto da equipe em base64 (data URL)", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
    private String fotoBase64;
    @Schema(description = "Banner da equipe em base64 (data URL)", example = "data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg==")
    private String bannerBase64;

    @Schema(description = "Data e hora de criação", example = "2026-10-07T14:32:10")
    private LocalDateTime criadoEm;
    @Schema(description = "Data e hora da última atualização", example = "2026-10-08T09:15:42")
    private LocalDateTime atualizadoEm;
}