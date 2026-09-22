package synapseforge.crud.DTO.Estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@Setter
public class EntradaEstoqueRequestDTO {

    @NotNull
    private TipoInsumo tipoInsumo;

    @NotBlank
    private String insumoId;

    @NotNull
    @Positive
    private BigDecimal quantidade;

    @NotNull
    private UnidadeMedida unidade;

    private String motivo;
}
