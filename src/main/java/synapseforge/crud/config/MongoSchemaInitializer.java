package synapseforge.crud.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Aplica validação por JSON Schema ($jsonSchema) nas coleções em que ela faz sentido.
 * <p>
 * O schema protege a FORMA do documento (campos obrigatórios, tipos, enums, valores não
 * negativos) mesmo quando a gravação não passa pela API (Compass, scripts, outro sistema).
 * Regras de negócio continuam nos services: existência de ids em outras coleções, ordem
 * das etapas, permissões por papel, cálculo de custos.
 * <p>
 * Ficam de fora de propósito: users (muitos campos opcionais de fluxo de e-mail/senha),
 * notificacoes (tipos mudam com frequência, dado pouco crítico) e coleções de cadastro simples.
 * <p>
 * validationLevel=moderate: documentos antigos que já estavam fora do padrão não travam
 * atualizações; tudo que é novo (ou que já era válido) passa a ser validado.
 * Roda por último para que migrações do {@link DataInitializer} aconteçam antes.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class MongoSchemaInitializer implements CommandLineRunner {

    static final List<String> COLECOES_VALIDADAS = List.of(
            "pedidos",
            "orcamentos",
            "ordens_pintura",
            "movimentos_estoque",
            "comentarios"
    );

    private static final Logger log = LoggerFactory.getLogger(MongoSchemaInitializer.class);

    private final MongoTemplate mongoTemplate;
    private final boolean habilitado;
    private final String acao;

    public MongoSchemaInitializer(
            MongoTemplate mongoTemplate,
            @Value("${app.mongo.schema-validation.enabled:true}") boolean habilitado,
            @Value("${app.mongo.schema-validation.action:error}") String acao) {
        this.mongoTemplate = mongoTemplate;
        this.habilitado = habilitado;
        this.acao = acao;
    }

    @Override
    public void run(String... args) {
        if (!habilitado) {
            log.info("Validação por JSON Schema desligada (app.mongo.schema-validation.enabled=false)");
            return;
        }
        for (String colecao : COLECOES_VALIDADAS) {
            try {
                aplicar(colecao, carregarSchema(colecao));
            } catch (Exception e) {
                // não derruba a aplicação: sem o schema, a validação da API continua valendo
                log.warn("Não foi possível aplicar o JSON Schema em '{}': {}", colecao, e.getMessage());
            }
        }
    }

    void aplicar(String colecao, Document schema) {
        Document validador = new Document("$jsonSchema", schema);
        // create (coleção nova) e collMod (coleção existente) recebem as mesmas opções
        Document comando = mongoTemplate.collectionExists(colecao)
                ? new Document("collMod", colecao)
                : new Document("create", colecao);
        comando.append("validator", validador)
                .append("validationLevel", "moderate")
                .append("validationAction", acao);

        mongoTemplate.executeCommand(comando);
        log.info("JSON Schema aplicado em '{}' (action={})", colecao, acao);
    }

    static Document carregarSchema(String colecao) throws IOException {
        ClassPathResource arquivo = new ClassPathResource("mongo/schemas/" + colecao + ".json");
        try (InputStream in = arquivo.getInputStream()) {
            return Document.parse(new String(in.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
