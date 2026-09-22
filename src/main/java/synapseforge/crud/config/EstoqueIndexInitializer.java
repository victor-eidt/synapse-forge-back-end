package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Garante os índices do módulo de estoque na subida da aplicação, pois
 * auto-index-creation não está habilitado e @Indexed não teria efeito em runtime.
 * <p>
 * O índice ÚNICO em chaveIdempotencia é o que garante a idempotência da baixa no
 * nível do banco: mesmo que duas requisições concorrentes passem pela verificação
 * feita no código, a segunda gravação do mesmo movimento é rejeitada pelo MongoDB.
 */
@Component
@RequiredArgsConstructor
public class EstoqueIndexInitializer implements CommandLineRunner {

    private static final String COLECAO_MOVIMENTOS = "movimentos_estoque";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        mongoTemplate.indexOps(COLECAO_MOVIMENTOS).ensureIndex(
                new Index().on("chaveIdempotencia", Sort.Direction.ASC).unique());

        mongoTemplate.indexOps(COLECAO_MOVIMENTOS).ensureIndex(
                new Index().on("tipoInsumo", Sort.Direction.ASC)
                        .on("insumoId", Sort.Direction.ASC)
                        .on("criadoEm", Sort.Direction.ASC));

        mongoTemplate.indexOps(COLECAO_MOVIMENTOS).ensureIndex(
                new Index().on("pedidoId", Sort.Direction.ASC));
    }
}
