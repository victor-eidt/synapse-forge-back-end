package synapseforge.crud.DTO.Material;

import io.swagger.v3.oas.annotations.media.Schema;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Schema(description = "Material de impressão da equipe")
public class MaterialResponseDTO {

    @Schema(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03")
    private String id;
    @Schema(description = "Nome do material", example = "PLA Vermelho 1,75 mm")
    private String nome;
    @Schema(description = "Tipo de material", example = "PLA")
    private String tipo;
    @Schema(description = "Densidade, em g/cm³", example = "1.24")
    private Double densidadeGcm3;
    @Schema(description = "Preço por grama, em R$", example = "0.12")
    private BigDecimal precoPorGrama;
    @Schema(description = "Material disponível para novos orçamentos", example = "true")
    private Boolean ativo;
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;
    @Schema(description = "Saldo atual em estoque, na unidade do insumo", example = "1250.000")
    private BigDecimal saldo;
    @Schema(description = "Estoque mínimo; abaixo disso o insumo entra em alerta", example = "500")
    private BigDecimal estoqueMinimo;
}
