package synapseforge.crud.DTO.Pedido;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.math.BigDecimal;

// mapeado diretamente do resultado da aggregation, por isso precisa de setters e construtor vazio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedidos da equipe agrupados por etapa")
public class PedidoStatusMetricaDTO {

    @Schema(description = "Etapa do pedido", example = "IMPRESSAO")
    private StatusPedido status;
    @Schema(description = "Quantidade de pedidos na etapa", example = "7")
    private long quantidade;
    @Schema(description = "Soma do preço final dos pedidos, em R$", example = "1350.00")
    private BigDecimal valorTotal;
    @Schema(description = "Pedidos com prazo vencido que ainda não foram finalizados nem cancelados", example = "2")
    private long atrasados;
}
