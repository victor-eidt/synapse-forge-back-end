package synapseforge.crud.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.mongodb.core.MongoTemplate;

import synapseforge.crud.infrastructure.entity.Comentario;
import synapseforge.crud.infrastructure.entity.EtapaOrdemPintura;
import synapseforge.crud.infrastructure.entity.MovimentoEstoque;
import synapseforge.crud.infrastructure.entity.Orcamento;
import synapseforge.crud.infrastructure.entity.OrdemPintura;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.PrioridadeOrdemPintura;
import synapseforge.crud.infrastructure.entity.StatusOrcamento;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.TipoMovimento;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

class MongoSchemaInitializerTest {

    private static final Map<String, Class<?>> ENTIDADES = Map.of(
            "pedidos", Pedido.class,
            "orcamentos", Orcamento.class,
            "ordens_pintura", OrdemPintura.class,
            "movimentos_estoque", MovimentoEstoque.class,
            "comentarios", Comentario.class
    );

    // ---------- os arquivos de schema ----------

    @Test
    void cadaColecaoValidadaTemSchemaComCamposObrigatorios() throws Exception {
        for (String colecao : MongoSchemaInitializer.COLECOES_VALIDADAS) {
            Document schema = MongoSchemaInitializer.carregarSchema(colecao);

            assertEquals("object", schema.getString("bsonType"), colecao);
            assertFalse(schema.getList("required", String.class).isEmpty(), colecao);
            // _class do Spring Data precisa continuar sendo aceito
            assertFalse(Boolean.FALSE.equals(schema.get("additionalProperties")), colecao);
        }
    }

    @Test
    void camposDoSchemaExistemNaEntidadeEColecaoBateComODocument() throws Exception {
        for (String colecao : MongoSchemaInitializer.COLECOES_VALIDADAS) {
            Class<?> entidade = ENTIDADES.get(colecao);
            assertEquals(colecao,
                    entidade.getAnnotation(org.springframework.data.mongodb.core.mapping.Document.class).collection());

            Document schema = MongoSchemaInitializer.carregarSchema(colecao);
            Document propriedades = schema.get("properties", Document.class);
            for (String campo : propriedades.keySet()) {
                assertTrue(temCampo(entidade, campo), colecao + "." + campo + " não existe em " + entidade.getSimpleName());
            }
            for (String obrigatorio : schema.getList("required", String.class)) {
                assertTrue(propriedades.containsKey(obrigatorio), colecao + ": required sem propriedade " + obrigatorio);
            }
        }
    }

    @Test
    void enumsDoSchemaAcompanhamOsEnumsJava() throws Exception {
        assertEnum("pedidos", "status", StatusPedido.class);
        assertEnum("orcamentos", "status", StatusOrcamento.class);
        assertEnum("ordens_pintura", "prioridade", PrioridadeOrdemPintura.class);
        assertEnum("ordens_pintura", "etapa", EtapaOrdemPintura.class);
        assertEnum("movimentos_estoque", "tipoInsumo", TipoInsumo.class);
        assertEnum("movimentos_estoque", "tipo", TipoMovimento.class);
        assertEnum("movimentos_estoque", "unidade", UnidadeMedida.class);
        assertEnum("movimentos_estoque", "etapaOrigem", StatusPedido.class);
    }

    @Test
    void saldoDoEstoqueNaoAceitaNegativoMasQuantidadeDoAjusteAceita() throws Exception {
        Document props = MongoSchemaInitializer.carregarSchema("movimentos_estoque").get("properties", Document.class);
        String padraoSaldo = props.get("saldoApos", Document.class).getString("pattern");
        String padraoQuantidade = props.get("quantidade", Document.class).getString("pattern");

        // BigDecimal é gravado como string pelo Spring Data
        assertTrue("125.50".matches(padraoSaldo));
        assertTrue("0E-10".matches(padraoSaldo));
        assertFalse("-1".matches(padraoSaldo));
        assertTrue("-30".matches(padraoQuantidade));
        assertFalse("abc".matches(padraoQuantidade));
    }

    // ---------- aplicação no MongoDB ----------

    @Test
    void colecaoNovaECriadaComValidadorModerate() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.collectionExists(anyString())).thenReturn(false);

        new MongoSchemaInitializer(mongo, true, "error").run();

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(mongo, times(MongoSchemaInitializer.COLECOES_VALIDADAS.size())).executeCommand(captor.capture());
        Document comando = captor.getAllValues().get(0);
        assertEquals("pedidos", comando.getString("create"));
        assertEquals("moderate", comando.getString("validationLevel"));
        assertEquals("error", comando.getString("validationAction"));
        assertTrue(comando.get("validator", Document.class).containsKey("$jsonSchema"));
    }

    @Test
    void colecaoExistenteRecebeCollMod() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.collectionExists(anyString())).thenReturn(true);

        new MongoSchemaInitializer(mongo, true, "warn").run();

        ArgumentCaptor<Document> captor = ArgumentCaptor.forClass(Document.class);
        verify(mongo, times(MongoSchemaInitializer.COLECOES_VALIDADAS.size())).executeCommand(captor.capture());
        List<String> alteradas = captor.getAllValues().stream().map(c -> c.getString("collMod")).toList();
        assertEquals(MongoSchemaInitializer.COLECOES_VALIDADAS, alteradas);
        assertEquals("warn", captor.getAllValues().get(0).getString("validationAction"));
    }

    @Test
    void falhaEmUmaColecaoNaoImpedeAsOutrasNemDerrubaAAplicacao() {
        MongoTemplate mongo = mock(MongoTemplate.class);
        when(mongo.collectionExists(anyString())).thenReturn(true);
        doThrow(new RuntimeException("sem permissão de collMod"))
                .doReturn(new Document("ok", 1))
                .when(mongo).executeCommand(any(Document.class));

        new MongoSchemaInitializer(mongo, true, "error").run();

        verify(mongo, times(MongoSchemaInitializer.COLECOES_VALIDADAS.size())).executeCommand(any(Document.class));
    }

    @Test
    void desligadoNaoMexeNoBanco() {
        MongoTemplate mongo = mock(MongoTemplate.class);

        new MongoSchemaInitializer(mongo, false, "error").run();

        verify(mongo, never()).executeCommand(any(Document.class));
    }

    private static void assertEnum(String colecao, String campo, Class<? extends Enum<?>> tipo) throws Exception {
        Document props = MongoSchemaInitializer.carregarSchema(colecao).get("properties", Document.class);
        List<String> doSchema = props.get(campo, Document.class).getList("enum", String.class);
        List<String> doJava = Arrays.stream(tipo.getEnumConstants()).map(Enum::name).toList();
        assertEquals(doJava, doSchema, colecao + "." + campo + " desatualizado em relação a " + tipo.getSimpleName());
    }

    private static boolean temCampo(Class<?> classe, String nome) {
        try {
            classe.getDeclaredField(nome);
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }
}
