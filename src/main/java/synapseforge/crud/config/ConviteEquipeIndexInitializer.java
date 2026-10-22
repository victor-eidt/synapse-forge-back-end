package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Índices dos convites de equipe. Como em {@link EstoqueIndexInitializer}:
 * auto-index-creation está desligado, então os índices são garantidos aqui.
 * <p>
 * Expiração: o convite vence em 24h (expiraEm), mas o documento continua no banco
 * para que o link antigo ainda responda "Este convite expirou". Depois de
 * {@value #DIAS_RETENCAO_APOS_VENCIMENTO} dias do vencimento o MongoDB apaga o convite
 * (índice TTL em expiraEm), qualquer que seja o status: nada consulta o histórico.
 */
@Component
@RequiredArgsConstructor
public class ConviteEquipeIndexInitializer implements CommandLineRunner {

    static final long DIAS_RETENCAO_APOS_VENCIMENTO = 30;

    private static final String COLECAO = "convites_equipe";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // link do e-mail (aceitar/recusar)
        mongoTemplate.indexOps(COLECAO).ensureIndex(
                new Index().on("token", Sort.Direction.ASC).sparse());

        // convite pendente do usuário logado
        mongoTemplate.indexOps(COLECAO).ensureIndex(
                new Index().on("usuarioId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC));

        // convites pendentes da equipe
        mongoTemplate.indexOps(COLECAO).ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC)
                        .on("status", Sort.Direction.ASC)
                        .on("usuarioId", Sort.Direction.ASC));

        // TTL: mudar o prazo depois de criado exige collMod (ou apagar o índice),
        // senão o ensureIndex falha com conflito de opções
        mongoTemplate.indexOps(COLECAO).ensureIndex(
                new Index().on("expiraEm", Sort.Direction.ASC)
                        .expire(Duration.ofDays(DIAS_RETENCAO_APOS_VENCIMENTO))
                        .named("ttl_expiraEm"));
    }
}
