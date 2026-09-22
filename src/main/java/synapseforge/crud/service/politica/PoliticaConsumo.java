package synapseforge.crud.service.politica;

import synapseforge.crud.infrastructure.entity.ConsumoPedido;
import synapseforge.crud.infrastructure.entity.ItemConsumo;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.util.List;

/**
 * Padrão Strategy: cada implementação encapsula a regra de consumo de uma etapa do pedido,
 * eliminando if/else espalhado sobre StatusPedido.
 */
public interface PoliticaConsumo {

    StatusPedido etapa();

    List<ItemConsumo> itensDe(ConsumoPedido consumoPedido);
}
