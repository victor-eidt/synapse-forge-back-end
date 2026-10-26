package synapseforge.crud.DTO.Pedido;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

// mapeado diretamente do resultado da aggregation, por isso precisa de setters e construtor vazio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Pedidos da equipe criados em um mês")
public class PedidoMesMetricaDTO {

    @Schema(description = "Mês de criação dos pedidos (yyyy-MM)", example = "2026-08")
    private String mes;
    @Schema(description = "Pedidos criados no mês que não foram cancelados", example = "7")
    private long quantidade;
    @Schema(description = "Pedidos criados no mês que foram cancelados", example = "1")
    private long cancelados;
    @Schema(description = "Soma do preço final dos pedidos não cancelados, em R$ (null se nenhum tem preço)", example = "1830.50")
    private BigDecimal valorTotal;
}
