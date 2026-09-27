package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Índices das consultas paginadas de orçamentos e do sino de notificações.
 * Como em {@link EstoqueIndexInitializer}: auto-index-creation está desligado,
 * então os índices são garantidos aqui, na subida da aplicação.
 */
@Component
@RequiredArgsConstructor
public class OrcamentoIndexInitializer implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // busca paginada: sempre filtra pela equipe e ordena pelos mais recentes
        mongoTemplate.indexOps("orcamentos").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("criadoEm", Sort.Direction.DESC));

        // sino: avisos não lidos do usuário, mais recentes primeiro
        mongoTemplate.indexOps("notificacoes").ensureIndex(
                new Index().on("usuarioId", Sort.Direction.ASC)
                        .on("lida", Sort.Direction.ASC)
                        .on("criadoEm", Sort.Direction.DESC));
    }
}
