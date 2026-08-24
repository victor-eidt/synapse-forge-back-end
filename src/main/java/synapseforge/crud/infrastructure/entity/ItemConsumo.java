package synapseforge.crud.infrastructure.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ItemConsumo {

    private TipoInsumo tipoInsumo;
    private String insumoId;
    private BigDecimal quantidade;
    private UnidadeMedida unidade;
    private StatusPedido etapaConsumo;
}
