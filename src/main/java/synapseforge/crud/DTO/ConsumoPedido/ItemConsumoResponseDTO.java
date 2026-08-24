package synapseforge.crud.DTO.ConsumoPedido;

import lombok.AllArgsConstructor;
import lombok.Getter;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
public class ItemConsumoResponseDTO {

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private BigDecimal quantidade;
    private UnidadeMedida unidade;
    private StatusPedido etapaConsumo;
}
