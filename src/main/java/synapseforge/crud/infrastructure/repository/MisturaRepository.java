package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;
import synapseforge.crud.infrastructure.entity.Mistura;

import java.util.List;

@Repository
public interface MisturaRepository extends MongoRepository<Mistura, String> {
    List<Mistura> findByUsuarioId(String usuarioId);
}
