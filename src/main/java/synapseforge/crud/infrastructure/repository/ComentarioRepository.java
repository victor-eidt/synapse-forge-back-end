package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Comentario;

import java.util.List;

public interface ComentarioRepository extends MongoRepository<Comentario, String> {

    List<Comentario> findByPedidoIdOrderByCriadoEmAsc(String pedidoId);
}