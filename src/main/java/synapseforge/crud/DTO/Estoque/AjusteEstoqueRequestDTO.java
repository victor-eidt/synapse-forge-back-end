package synapseforge.crud.DTO.Estoque;

import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@Setter
@Schema(description = "Ajuste manual de estoque (inventário, perda); quantidade negativa reduz o saldo")
public class AjusteEstoqueRequestDTO {

    @NotNull
    @Schema(description = "Tipo do insumo: MATERIAL (filamento/resina) ou COR (tinta)", example = "MATERIAL")
    private TipoInsumo tipoInsumo;

    @NotBlank
    @Schema(description = "ID do material ou da cor, conforme tipoInsumo", example = "6704a1c2e4b0f81a2c3d4e03")
    private String insumoId;

    // positiva acrescenta, negativa retira; zero é rejeitado no serviço
    @NotNull
    @Schema(description = "Quantidade do ajuste; negativa para reduzir", example = "-50")
    private BigDecimal quantidade;

    @NotNull
    @Schema(description = "Unidade de medida do insumo", example = "G")
    private UnidadeMedida unidade;

    @Schema(description = "Motivo do ajuste", example = "Perda por falha de impressão (warping)")
    private String motivo;
}
