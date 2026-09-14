package synapseforge.crud.DTO.Material;

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
public class MaterialRequestDTO {

    @NotBlank
    private String nome;

    @NotBlank
    private String tipo;

    @NotNull
    @Positive
    private Double densidadeGcm3;

    @NotNull
    @Positive
    private BigDecimal precoPorGrama;

    private Boolean ativo;

    private UnidadeMedida unidade;

    @PositiveOrZero
    private BigDecimal saldo;

    @PositiveOrZero
    private BigDecimal estoqueMinimo;
}
