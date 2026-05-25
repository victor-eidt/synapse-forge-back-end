package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Material;

import java.util.List;

public interface MaterialRepository extends MongoRepository<Material, String> {

    List<Material> findByAtivoTrue();
}
