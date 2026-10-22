package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

import java.time.Duration;

/**
 * Índices do sino de notificações. Como em {@link EstoqueIndexInitializer}:
 * auto-index-creation está desligado, então o @Indexed da entidade não tem efeito
 * e os índices são garantidos aqui, na subida da aplicação.
 * <p>
 * Expiração: aviso já lido é apagado pelo próprio MongoDB (índice TTL em lidaEm)
 * {@value #DIAS_RETENCAO_LIDAS} dias depois da leitura. Aviso não lido não tem lidaEm,
 * então o TTL não o alcança e ele fica no sino até ser lido.
 */
@Component
@RequiredArgsConstructor
public class NotificacaoIndexInitializer implements CommandLineRunner {

    static final long DIAS_RETENCAO_LIDAS = 30;

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        // sino: avisos (não lidos) do usuário, mais recentes primeiro
        mongoTemplate.indexOps("notificacoes").ensureIndex(
                new Index().on("usuarioId", Sort.Direction.ASC)
                        .on("lida", Sort.Direction.ASC)
                        .on("criadaEm", Sort.Direction.DESC));

        // aviso repetido: procura o não lido do mesmo tipo para o mesmo registro
        mongoTemplate.indexOps("notificacoes").ensureIndex(
                new Index().on("usuarioId", Sort.Direction.ASC)
                        .on("tipo", Sort.Direction.ASC)
                        .on("referenciaId", Sort.Direction.ASC)
                        .on("lida", Sort.Direction.ASC));

        // TTL: mudar o prazo depois de criado exige collMod (ou apagar o índice),
        // senão o ensureIndex falha com conflito de opções
        mongoTemplate.indexOps("notificacoes").ensureIndex(
                new Index().on("lidaEm", Sort.Direction.ASC)
                        .expire(Duration.ofDays(DIAS_RETENCAO_LIDAS))
                        .named("ttl_lidaEm"));
    }
}
