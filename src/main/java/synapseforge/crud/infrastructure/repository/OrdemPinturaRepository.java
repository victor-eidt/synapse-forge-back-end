package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.OrdemPintura;

import java.util.List;

public interface OrdemPinturaRepository extends MongoRepository<OrdemPintura, String> {
    List<OrdemPintura> findByUsuarioIdOrderByCriadoEmDesc(String usuarioId);
}
