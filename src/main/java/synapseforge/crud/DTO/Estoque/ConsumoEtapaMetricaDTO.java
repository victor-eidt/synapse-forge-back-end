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
@Schema(description = "Consumo agregado por etapa no período")
public class ConsumoEtapaMetricaDTO {

    @Schema(description = "Etapa de produção", example = "IMPRESSAO")
    private StatusPedido etapa;
    @Schema(description = "Quantidade total consumida", example = "1840.500")
    private BigDecimal totalConsumido;
    @Schema(description = "Custo total consumido, em R$", example = "220.86")
    private BigDecimal custoTotal;
}
