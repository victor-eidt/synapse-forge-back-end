package synapseforge.crud.DTO.OrdemPintura;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Opção do select de técnico na ordem de pintura: só o necessário para exibir e escolher.
@Getter
@AllArgsConstructor
@Schema(description = "Opção do select de responsável da ordem")
public class TecnicoResumoDTO {
    @Schema(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e05")
    private String id;
    @Schema(description = "Nome do usuário", example = "Carlos Mendes")
    private String nome;
}
