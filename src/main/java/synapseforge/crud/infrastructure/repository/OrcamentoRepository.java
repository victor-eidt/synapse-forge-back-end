package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Orcamento;

import java.util.List;

public interface OrcamentoRepository extends MongoRepository<Orcamento, String> {

    List<Orcamento> findAllByOrderByCriadoEmDesc();
}
