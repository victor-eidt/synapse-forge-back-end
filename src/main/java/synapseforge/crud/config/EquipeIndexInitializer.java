package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Índices dos cadastros isolados por equipe (cores, materiais, misturas e eventos)
 * e da própria equipe. Como em {@link EstoqueIndexInitializer}: auto-index-creation
 * está desligado, então os índices são garantidos aqui.
 */
@Component
@RequiredArgsConstructor
public class EquipeIndexInitializer implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // equipe do gerente logado
        mongoTemplate.indexOps("equipes").ensureIndex(
                new Index().on("gerenteId", Sort.Direction.ASC));

        mongoTemplate.indexOps("cores").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC));

        // catálogo: só materiais ativos da equipe
        mongoTemplate.indexOps("materiais").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("ativo", Sort.Direction.ASC));

        mongoTemplate.indexOps("misturas").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC));

        // agenda: eventos do usuário (dono) na equipe; o filtro por mês usa o mesmo prefixo
        mongoTemplate.indexOps("eventos").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("userId", Sort.Direction.ASC));
    }
}
