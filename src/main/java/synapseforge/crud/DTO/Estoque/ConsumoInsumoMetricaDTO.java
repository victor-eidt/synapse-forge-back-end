package synapseforge.crud.DTO.Estoque;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;

import java.math.BigDecimal;

// mapeado diretamente do resultado da aggregation, por isso precisa de setters e construtor vazio
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConsumoInsumoMetricaDTO {

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private BigDecimal totalConsumido;
    private BigDecimal custoTotal;
}
