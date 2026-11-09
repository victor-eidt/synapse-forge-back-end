package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import synapseforge.crud.infrastructure.entity.FormaEnvio;

import java.util.List;
import java.util.Optional;

public interface FormaEnvioRepository extends MongoRepository<FormaEnvio, String> {

    // formas padrão do sistema (sem equipe) + as cadastradas pela equipe
    @Query("{ 'ativo': true, $or: [ { 'equipeId': null }, { 'equipeId': ?0 } ] }")
    List<FormaEnvio> findAtivasDisponiveisPara(String equipeId);

    List<FormaEnvio> findByAtivoTrueAndEquipeIdIsNull();

    @Query("{ '_id': ?0, 'ativo': true, $or: [ { 'equipeId': null }, { 'equipeId': ?1 } ] }")
    Optional<FormaEnvio> findAtivaDisponivelPara(String id, String equipeId);
}
