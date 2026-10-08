package synapseforge.crud.DTO.ConsumoPedido;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Insumo consumido em uma etapa do pedido")
public class ItemConsumoRequestDTO {

    @NotNull
    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;

    @NotBlank
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;

    @NotNull
    @Positive
    @Schema(description = "Quantidade consumida, na unidade informada", example = "106.02")
    private BigDecimal quantidade;

    @NotNull
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;

    @NotNull
    @Schema(description = "Etapa em que a baixa de estoque acontece", example = "IMPRESSAO")
    private StatusPedido etapaConsumo;
}
