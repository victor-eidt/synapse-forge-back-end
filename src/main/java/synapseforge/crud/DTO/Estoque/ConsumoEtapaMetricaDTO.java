package synapseforge.crud.DTO.Estoque;

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
public class ConsumoEtapaMetricaDTO {

    private StatusPedido etapa;
    private BigDecimal totalConsumido;
    private BigDecimal custoTotal;
}
