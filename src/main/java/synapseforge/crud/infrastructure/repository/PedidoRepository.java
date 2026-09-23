package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
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

    boolean existsByEquipeIdAndClienteId(
            String equipeId,
            String clienteId
    );

    // Só o clienteId dos pedidos da equipe: base da lista "clientes da equipe"
    @Query(
            value = "{ 'equipeId': ?0, 'clienteId': { $ne: null } }",
            fields = "{ 'clienteId': 1 }"
    )
    List<Pedido> findClienteIdsByEquipeId(String equipeId);

    // Cliente não pertence a equipe: enxerga os próprios pedidos de
    // qualquer oficina, sempre pelo clienteId.

    List<Pedido> findByClienteId(String clienteId);

    List<Pedido> findByClienteIdAndStatus(
            String clienteId,
            StatusPedido status
    );

    boolean existsByEquipeIdAndClienteIdAndMaterialId(
            String equipeId,
            String clienteId,
            String materialId
    );
}
