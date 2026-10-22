package synapseforge.crud.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.index.IndexInfo;
import org.springframework.test.context.TestPropertySource;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Sobe a aplicação com o Mongo embutido (mesma configuração do ApplicationTests, então
 * o contexto é reaproveitado) e confere os índices criados pelos *IndexInitializer.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.auto-index-creation=true",
    "de.flapdoodle.mongodb.embedded.version=7.0.0",
    "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tMzItYnl0ZXMhISE="
})
class MongoIndexInitializerTest {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void notificacaoLidaExpiraPorTtl() {
        IndexInfo ttl = indice("notificacoes", "ttl_lidaEm");

        assertThat(ttl.getIndexFields()).extracting(f -> f.getKey()).containsExactly("lidaEm");
        assertThat(ttl.getExpireAfter())
                .contains(Duration.ofDays(NotificacaoIndexInitializer.DIAS_RETENCAO_LIDAS));
    }

    @Test
    void conviteExpiraPorTtlDepoisDoVencimento() {
        IndexInfo ttl = indice("convites_equipe", "ttl_expiraEm");

        assertThat(ttl.getIndexFields()).extracting(f -> f.getKey()).containsExactly("expiraEm");
        assertThat(ttl.getExpireAfter())
                .contains(Duration.ofDays(ConviteEquipeIndexInitializer.DIAS_RETENCAO_APOS_VENCIMENTO));
    }

    @Test
    void tokensDeUsuarioTemIndiceEsparso() {
        for (String token : List.of("resetToken", "emailConfirmToken", "emailMudancaToken")) {
            IndexInfo indice = indice("users", token + "_1");
            assertThat(indice.isSparse()).as(token).isTrue();
        }
    }

    @Test
    void consultasFrequentesTemIndice() {
        assertThat(nomes("pedidos")).contains("equipeId_1_status_1", "clienteId_1_status_1");
        assertThat(nomes("convites_equipe")).contains("token_1", "usuarioId_1_status_1");
        assertThat(nomes("comentarios")).contains("pedidoId_1_criadoEm_1");
        assertThat(nomes("eventos")).contains("equipeId_1_userId_1");
    }

    private IndexInfo indice(String colecao, String nome) {
        return mongoTemplate.indexOps(colecao).getIndexInfo().stream()
                .filter(i -> i.getName().equals(nome))
                .findFirst()
                .orElseThrow(() -> new AssertionError("índice " + nome + " ausente em " + colecao));
    }

    private List<String> nomes(String colecao) {
        return mongoTemplate.indexOps(colecao).getIndexInfo().stream().map(IndexInfo::getName).toList();
    }
}
