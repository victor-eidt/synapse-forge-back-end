package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
@Schema(description = "Custo real de insumos de um pedido, total e por etapa")
public class CustoPedidoResponseDTO {

    @Schema(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01")
    private String pedidoId;
    @Schema(description = "Custo total de insumos, em R$", example = "18.32")
    private BigDecimal custoTotal;
    @Schema(description = "Custo detalhado por etapa")
    private List<CustoEtapaMetricaDTO> porEtapa;
}
