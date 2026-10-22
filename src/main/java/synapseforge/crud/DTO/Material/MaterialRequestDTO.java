package synapseforge.crud.DTO.Material;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Cadastro ou edição de material de impressão")
public class MaterialRequestDTO {

    @NotBlank
    @Schema(description = "Nome do material", example = "PLA Vermelho 1,75 mm")
    private String nome;

    @NotBlank
    @Schema(description = "Tipo de material", example = "PLA")
    private String tipo;

    @NotNull
    @Positive
    @Schema(description = "Densidade, em g/cm³ (usada para calcular a massa)", example = "1.24")
    private Double densidadeGcm3;

    @NotNull
    @Positive
    @Schema(description = "Preço por grama, em R$", example = "0.12")
    private BigDecimal precoPorGrama;

    @Schema(description = "Material disponível para novos orçamentos", example = "true")
    private Boolean ativo;

    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;

    // saldo não entra no request: só muda por movimentação de estoque
    // (entrada, ajuste, baixa, estorno), nunca por edição de cadastro
    @PositiveOrZero
    @Schema(description = "Estoque mínimo; abaixo disso o insumo entra em alerta", example = "500")
    private BigDecimal estoqueMinimo;
}
