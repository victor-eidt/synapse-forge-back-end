package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Material;

import java.util.List;
import java.util.Optional;

public interface MaterialRepository extends MongoRepository<Material, String> {

    Optional<Material> findByIdAndEquipeId(String id, String equipeId);

    List<Material> findByEquipeIdAndAtivoTrue(String equipeId);
}
