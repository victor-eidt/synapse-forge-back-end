package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Entrada de insumo no estoque (compra, reposição)")
public class EntradaEstoqueRequestDTO {

    @NotNull
    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;

    @NotBlank
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;

    @NotNull
    @Positive
    @Schema(description = "Quantidade que entrou, na unidade informada", example = "1000")
    private BigDecimal quantidade;

    @NotNull
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;

    @Schema(description = "Motivo da movimentação", example = "Compra de 1 kg de PLA Vermelho (NF 4521)")
    private String motivo;
}
