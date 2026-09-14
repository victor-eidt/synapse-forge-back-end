package synapseforge.crud.DTO.Estoque;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@Setter
public class AjusteEstoqueRequestDTO {

    @NotNull
    private TipoInsumo tipoInsumo;

    @NotBlank
    private String insumoId;

    // positiva acrescenta, negativa retira; zero é rejeitado no serviço
    @NotNull
    private BigDecimal quantidade;

    @NotNull
    private UnidadeMedida unidade;

    private String motivo;
}
