package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.OrdemPintura;

import java.util.List;
import java.util.Optional;

public interface OrdemPinturaRepository extends MongoRepository<OrdemPintura, String> {

    Optional<OrdemPintura> findByIdAndEquipeId(String id, String equipeId);

    List<OrdemPintura> findByEquipeIdOrderByCriadoEmDesc(String equipeId);
}
