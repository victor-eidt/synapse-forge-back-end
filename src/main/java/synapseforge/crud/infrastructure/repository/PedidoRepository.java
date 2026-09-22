package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.util.List;
import java.util.Optional;

public interface PedidoRepository extends MongoRepository<Pedido, String> {

    // Consultas sempre filtradas pela equipe (isolamento entre oficinas)

    Optional<Pedido> findByIdAndEquipeId(String id, String equipeId);

    List<Pedido> findByEquipeId(String equipeId);

    List<Pedido> findByEquipeIdAndStatus(
            String equipeId,
            StatusPedido status
    );

    List<Pedido> findByEquipeIdAndClienteId(
            String equipeId,
            String clienteId
    );

    List<Pedido> findByEquipeIdAndClienteIdAndStatus(
            String equipeId,
            String clienteId,
            StatusPedido status
    );
}
