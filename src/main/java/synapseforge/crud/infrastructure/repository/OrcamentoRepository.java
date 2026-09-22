package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Orcamento;

import java.util.List;
import java.util.Optional;

public interface OrcamentoRepository extends MongoRepository<Orcamento, String> {

    Optional<Orcamento> findByIdAndEquipeId(String id, String equipeId);

    List<Orcamento> findByEquipeIdOrderByCriadoEmDesc(String equipeId);
}
