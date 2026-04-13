package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import synapseforge.crud.infrastructure.entity.Evento;

import java.util.List;

public interface EventoRepository extends MongoRepository<Evento, String> {

    List<Evento> findByUserId(String userId);

    @Query("{ 'userId': ?0, 'data': { $regex: ?1 } }")
    List<Evento> findByUserIdAndMesAno(String userId, String mesAnoPattern);

}
