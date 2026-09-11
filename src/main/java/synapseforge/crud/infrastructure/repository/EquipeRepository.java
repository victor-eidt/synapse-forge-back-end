package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Equipe;

import java.util.Optional;

public interface EquipeRepository extends MongoRepository<Equipe, String> {

    Optional<Equipe> findByGerenteId(String gerenteId);
}