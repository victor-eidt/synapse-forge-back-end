package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Índices do sino de notificações. Como em {@link EstoqueIndexInitializer}:
 * auto-index-creation está desligado, então o @Indexed da entidade não tem efeito
 * e os índices são garantidos aqui, na subida da aplicação.
 */
@Component
@RequiredArgsConstructor
public class NotificacaoIndexInitializer implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // sino: avisos (não lidos) do usuário, mais recentes primeiro
        mongoTemplate.indexOps("notificacoes").ensureIndex(
                new Index().on("usuarioId", Sort.Direction.ASC)
                        .on("lida", Sort.Direction.ASC)
                        .on("criadaEm", Sort.Direction.DESC));
    }
}
