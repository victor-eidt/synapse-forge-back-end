package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.ConsumoPedido;

import java.util.Optional;

public interface ConsumoPedidoRepository extends MongoRepository<ConsumoPedido, String> {

    Optional<ConsumoPedido> findByPedidoId(String pedidoId);
}
