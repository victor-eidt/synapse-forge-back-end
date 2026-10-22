package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Índices de pedidos e do que pendura neles (ordens de pintura, comentários e
 * consumo). Como em {@link EstoqueIndexInitializer}: auto-index-creation está
 * desligado, então os índices são garantidos aqui.
 */
@Component
@RequiredArgsConstructor
public class PedidoIndexInitializer implements CommandLineRunner {

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // pedidos da equipe, com ou sem filtro de status
        mongoTemplate.indexOps("pedidos").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC));

        // clientes da equipe e "cliente já comprou este material"
        mongoTemplate.indexOps("pedidos").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("clienteId", Sort.Direction.ASC)
                        .on("materialId", Sort.Direction.ASC));

        // cliente vê os próprios pedidos de qualquer oficina
        mongoTemplate.indexOps("pedidos").ensureIndex(
                new Index().on("clienteId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC));

        mongoTemplate.indexOps("ordens_pintura").ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("criadoEm", Sort.Direction.DESC));

        mongoTemplate.indexOps("comentarios").ensureIndex(
                new Index().on("pedidoId", Sort.Direction.ASC)
                        .on("criadoEm", Sort.Direction.ASC));

        mongoTemplate.indexOps("consumos_pedido").ensureIndex(
                new Index().on("pedidoId", Sort.Direction.ASC)
                        .on("equipeId", Sort.Direction.ASC));
    }
}
