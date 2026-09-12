package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.StatusConviteEquipe;

import java.util.List;
import java.util.Optional;

public interface ConviteEquipeRepository
        extends MongoRepository<ConviteEquipe, String> {

    Optional<ConviteEquipe> findByToken(String token);

    Optional<ConviteEquipe> findByUsuarioIdAndStatus(
            String usuarioId,
            StatusConviteEquipe status
    );

    List<ConviteEquipe> findByEquipeIdAndStatus(
            String equipeId,
            StatusConviteEquipe status
    );

    boolean existsByEquipeIdAndUsuarioIdAndStatus(
            String equipeId,
            String usuarioId,
            StatusConviteEquipe status
    );
}

