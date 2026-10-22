package synapseforge.crud.config;

import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.Index;
import org.springframework.stereotype.Component;

/**
 * Índices de usuários. Como em {@link EstoqueIndexInitializer}: auto-index-creation
 * está desligado, então os índices são garantidos aqui.
 * <p>
 * Os tokens (senha, confirmação e troca de e-mail) ficam dentro do próprio usuário,
 * então não dá para expirá-los com TTL (TTL apaga o documento inteiro). O vencimento
 * continua sendo conferido no código pelos campos *Expira; aqui só entram índices
 * esparsos, que guardam apenas quem tem token pendente.
 * <p>
 * O índice de e-mail não é único de propósito: contas antigas podem ter o mesmo
 * e-mail com caixa diferente e um índice único derrubaria a subida da aplicação.
 */
@Component
@RequiredArgsConstructor
public class UserIndexInitializer implements CommandLineRunner {

    private static final String COLECAO = "users";

    private final MongoTemplate mongoTemplate;

    @Override
    public void run(String... args) {
        mongoTemplate.indexOps(COLECAO).ensureIndex(
                new Index().on("email", Sort.Direction.ASC));

        // integrantes da equipe
        mongoTemplate.indexOps(COLECAO).ensureIndex(
                new Index().on("equipeId", Sort.Direction.ASC));

        // links de e-mail: recuperação de senha, confirmação e troca de e-mail
        for (String token : new String[]{"resetToken", "emailConfirmToken", "emailMudancaToken"}) {
            mongoTemplate.indexOps(COLECAO).ensureIndex(
                    new Index().on(token, Sort.Direction.ASC).sparse());
        }
    }
}
