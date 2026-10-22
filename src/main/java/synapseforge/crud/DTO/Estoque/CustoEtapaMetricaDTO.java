package synapseforge.crud.DTO.Estoque;

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
@Schema(description = "Custo de insumos de um pedido em uma etapa")
public class CustoEtapaMetricaDTO {

    @Schema(description = "Etapa de produção", example = "IMPRESSAO")
    private StatusPedido etapa;
    @Schema(description = "Custo na etapa, em R$", example = "12.72")
    private BigDecimal custoTotal;
}
