package synapseforge.crud.infrastructure.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import synapseforge.crud.infrastructure.entity.Notificacao;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;

import java.util.List;
import java.util.Optional;

public interface NotificacaoRepository extends MongoRepository<Notificacao, String> {

    List<Notificacao> findTop50ByUsuarioIdOrderByCriadaEmDesc(String usuarioId);

    List<Notificacao> findTop50ByUsuarioIdAndLidaFalseOrderByCriadaEmDesc(String usuarioId);

    List<Notificacao> findByUsuarioIdAndLidaFalseOrderByCriadaEmDesc(String usuarioId);

    Optional<Notificacao> findByIdAndUsuarioId(String id, String usuarioId);

    // Aviso ainda não lido do mesmo tipo para o mesmo registro (ex.: etapa do pedido p-1)
    List<Notificacao> findByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
            String usuarioId,
            TipoNotificacao tipo,
            String referenciaId
    );

}
