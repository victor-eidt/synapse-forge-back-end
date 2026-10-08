package synapseforge.crud.DTO.OrdemPintura;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;

import java.time.LocalDate;

@Getter
@Setter
@Schema(description = "Criação ou edição de ordem de pintura")
public class OrdemPinturaRequestDTO {

    @NotBlank
    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;

    @NotBlank
    @Schema(description = "ID da cor (tinta)", example = "6704a1c2e4b0f81a2c3d4e04")
    private String corId;

    // id do usuário técnico (não mais o nome digitado): o service valida que é da equipe
    @NotBlank
    @Schema(description = "ID do responsável: técnico ativo ou gerente da mesma equipe (veja GET /ordens-pintura/tecnicos)", example = "6704a1c2e4b0f81a2c3d4e05")
    private String tecnicoId;

    @NotNull
    @Schema(description = "Prioridade da ordem", example = "ALTA")
    private PrioridadeOrdemPintura prioridade;

    @NotNull
    @Schema(description = "Data de entrega (yyyy-MM-dd)", example = "2026-10-20")
    private LocalDate prazo;
}
