package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import synapseforge.crud.infrastructure.entity.Evento;

import java.util.List;
import java.util.Optional;

public interface EventoRepository extends MongoRepository<Evento, String> {

    Optional<Evento> findByIdAndEquipeId(String id, String equipeId);

    List<Evento> findByEquipeId(String equipeId);

    List<Evento> findByEquipeIdAndUserId(String equipeId, String userId);

    @Query("{ 'equipeId': ?0, $or: [ { 'userId': ?1 }, { 'participantes': ?1 } ], 'data': { $regex: ?2 } }")
    List<Evento> findByEquipeIdAndUserIdOrParticipanteAndMesAno(String equipeId, String userId, String mesAnoPattern);
}
