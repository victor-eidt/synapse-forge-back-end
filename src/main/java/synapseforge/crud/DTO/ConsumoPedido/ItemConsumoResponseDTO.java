package synapseforge.crud.DTO.ConsumoPedido;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "Insumo consumido em uma etapa do pedido")
public class ItemConsumoResponseDTO {

    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;
    @Schema(description = "Quantidade consumida, na unidade informada", example = "106.02")
    private BigDecimal quantidade;
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;
    @Schema(description = "Etapa em que a baixa de estoque acontece", example = "IMPRESSAO")
    private StatusPedido etapaConsumo;
}
