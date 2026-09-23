package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import synapseforge.crud.infrastructure.entity.Mistura;

import java.util.List;
import java.util.Optional;

@Repository
public interface MisturaRepository extends MongoRepository<Mistura, String> {

    Optional<Mistura> findByIdAndEquipeId(String id, String equipeId);

    List<Mistura> findByEquipeId(String equipeId);
}
