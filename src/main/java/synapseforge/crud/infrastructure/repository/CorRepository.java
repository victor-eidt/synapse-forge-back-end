package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Cor;

import java.util.List;

public interface CorRepository extends MongoRepository<Cor, String> {

    List<Cor> findByUsuarioId(String usuarioId);
}
