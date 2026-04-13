package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.util.List;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

    List<Pedido> findByUsuarioId(String usuarioId);

    List<Pedido> findByUsuarioIdAndStatus(String usuarioId, StatusPedido status);
}
