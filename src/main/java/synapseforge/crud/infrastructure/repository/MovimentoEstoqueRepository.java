package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.TipoInsumo;

import java.util.List;
import java.util.Optional;

public interface MovimentoEstoqueRepository extends MongoRepository<MovimentoEstoque, String> {

    // A chave de idempotência é global (índice único): começa pelo pedidoId (ObjectId único)
    // ou é um UUID, então não colide entre equipes.
    Optional<MovimentoEstoque> findByChaveIdempotencia(String chave);

    List<MovimentoEstoque> findByEquipeIdAndTipoInsumoAndInsumoIdOrderByCriadoEmDesc(
            String equipeId, TipoInsumo tipoInsumo, String insumoId);

    List<MovimentoEstoque> findByEquipeIdAndPedidoId(String equipeId, String pedidoId);
}
