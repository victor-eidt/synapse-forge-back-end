package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "Insumo em risco de acabar, com a cobertura em dias")
public class InsumoCriticoResponseDTO {

    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;
    @Schema(description = "Nome do insumo", example = "PLA Vermelho 1,75 mm")
    private String nome;
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;
    @Schema(description = "Saldo atual", example = "320.000")
    private BigDecimal saldo;
    @Schema(description = "Estoque mínimo; abaixo disso o insumo entra em alerta", example = "500")
    private BigDecimal estoqueMinimo;
    @Schema(description = "Consumo médio por dia", example = "45.200")
    private BigDecimal consumoMedioDiario;

    // null quando não houve consumo recente: cobertura indeterminada, ordenado por último
    @Schema(description = "Dias até acabar no ritmo atual", example = "7.1")
    private BigDecimal diasCobertura;
}
