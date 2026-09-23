package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Cor;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CorRepository extends MongoRepository<Cor, String> {

    Optional<Cor> findByIdAndEquipeId(String id, String equipeId);

    List<Cor> findByEquipeId(String equipeId);

    List<Cor> findByIdInAndEquipeId(Collection<String> ids, String equipeId);
}
