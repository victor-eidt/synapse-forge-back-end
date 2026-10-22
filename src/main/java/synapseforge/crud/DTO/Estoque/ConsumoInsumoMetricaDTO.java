package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

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
@Schema(description = "Consumo agregado por insumo no período")
public class ConsumoInsumoMetricaDTO {

    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;
    @Schema(description = "Quantidade total consumida", example = "1840.500")
    private BigDecimal totalConsumido;
    @Schema(description = "Custo total consumido, em R$", example = "220.86")
    private BigDecimal custoTotal;
}
