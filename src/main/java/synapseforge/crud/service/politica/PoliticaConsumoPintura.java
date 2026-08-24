package synapseforge.crud.service.politica;

import org.springframework.stereotype.Component;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.util.List;

/**
 * Strategy da etapa PINTURA: seleciona os insumos consumidos ao pintar (primer e tintas),
 * escolhido para que novas etapas entrem como novas classes e não como if/else no serviço.
 */
@Component
public class PoliticaConsumoPintura implements PoliticaConsumo {

    @Override
    public StatusPedido etapa() {
        return StatusPedido.PINTURA;
    }

    @Override
    public List<ItemConsumo> itensDe(ConsumoPedido consumoPedido) {
        if (consumoPedido.getItens() == null) {
            return List.of();
        }
        return consumoPedido.getItens().stream()
                .filter(item -> item.getEtapaConsumo() == StatusPedido.PINTURA)
                .toList();
    }
}
