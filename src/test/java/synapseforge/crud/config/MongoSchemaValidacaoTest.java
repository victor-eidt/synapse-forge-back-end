package synapseforge.crud.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;
import synapseforge.crud.infrastructure.entity.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Aplica os JSON Schemas num MongoDB de verdade (o embutido) e grava entidades pelo
 * MongoTemplate, para conferir os tipos BSON que o Spring escreve. O run() do
 * MongoSchemaInitializer só registra WARN em caso de erro, então aqui o aplicar() é
 * chamado direto: schema quebrado derruba o teste.
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.auto-index-creation=true",
    "de.flapdoodle.mongodb.embedded.version=7.0.0",
    "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tMzItYnl0ZXMhISE="
})
class MongoSchemaValidacaoTest {

    private static final String EQUIPE = "eq-teste-schema";

    @Autowired
    private MongoTemplate mongoTemplate;

    @BeforeEach
    void aplicarSchemas() throws Exception {
        MongoSchemaInitializer initializer = new MongoSchemaInitializer(mongoTemplate, true, "error");
        for (String colecao : MongoSchemaInitializer.COLECOES_VALIDADAS) {
            initializer.aplicar(colecao, MongoSchemaInitializer.carregarSchema(colecao));
        }
    }

    @AfterEach
    void limpar() {
        Query daEquipe = Query.query(Criteria.where("equipeId").is(EQUIPE));
        for (Class<?> tipo : List.of(Pedido.class, Orcamento.class, OrdemPintura.class, MovimentoEstoque.class)) {
            mongoTemplate.remove(daEquipe, tipo);
        }
        mongoTemplate.remove(Query.query(Criteria.where("pedidoId").is("p-teste-schema")), Comentario.class);
    }

    @Test
    void pedidoCompletoPassa() {
        assertDoesNotThrow(() -> mongoTemplate.insert(pedido()));
    }

    @Test
    void pedidoSemClienteVinculadoPassa() {
        // o form permite "Nenhum cliente vinculado": o controller grava cliente = ""
        Pedido pedido = pedido();
        pedido.setCliente("");
        pedido.setClienteId(null);

        assertDoesNotThrow(() -> mongoTemplate.insert(pedido));
    }

    @Test
    void pedidoComCustoNegativoERecusado() {
        Pedido pedido = pedido();
        pedido.setCustoTotal(new BigDecimal("-1"));

        assertThrows(DataIntegrityViolationException.class, () -> mongoTemplate.insert(pedido));
    }

    @Test
    void orcamentoValidoPassaEVolumeZeroERecusado() {
        assertDoesNotThrow(() -> mongoTemplate.insert(orcamento(10.0)));
        assertThrows(DataIntegrityViolationException.class, () -> mongoTemplate.insert(orcamento(0.0)));
    }

    @Test
    void ordemDePinturaValidaPassa() {
        OrdemPintura ordem = new OrdemPintura();
        ordem.setEquipeId(EQUIPE);
        ordem.setUsuarioId("u1");
        ordem.setPedidoId("p1");
        ordem.setCorId("c1");
        ordem.setTecnicoId("t1");
        ordem.setTecnico("Técnico");
        ordem.setPrioridade(PrioridadeOrdemPintura.ALTA);
        ordem.setPrazo(LocalDate.now());
        ordem.setEtapa(EtapaOrdemPintura.RETRABALHO);
        ordem.setCriadoEm(LocalDateTime.now());
        ordem.setAtualizadoEm(LocalDateTime.now());

        assertDoesNotThrow(() -> mongoTemplate.insert(ordem));
    }

    @Test
    void movimentoValidoPassaESaldoNegativoERecusado() {
        assertDoesNotThrow(() -> mongoTemplate.insert(movimento(new BigDecimal("-30"), new BigDecimal("70"))));
        assertThrows(DataIntegrityViolationException.class,
                () -> mongoTemplate.insert(movimento(new BigDecimal("5"), new BigDecimal("-1"))));
    }

    @Test
    void comentarioValidoPassa() {
        Comentario comentario = new Comentario(null, "p-teste-schema", "u1", "Pintura aprovada", LocalDateTime.now());

        assertDoesNotThrow(() -> mongoTemplate.insert(comentario));
    }

    private Pedido pedido() {
        Pedido p = new Pedido();
        p.setEquipeId(EQUIPE);
        p.setUsuarioId("u1");
        p.setClienteId("c1");
        p.setCliente("Cliente");
        p.setProjeto("Projeto");
        p.setDescricao("Descrição");
        p.setOrcamentoId("o1");
        p.setMaterialId("m1");
        p.setVolumeCm3(12.5);
        p.setTempoImpressaoHoras(2.0);
        p.setTempoMaoDeObraHoras(0.0);
        p.setCustoMaquinaHora(new BigDecimal("3.50"));
        p.setCustoMaoDeObraHora(new BigDecimal("20"));
        p.setMargemLucro(new BigDecimal("0.3"));
        p.setCustoMaterial(new BigDecimal("1.23"));
        p.setCustoMaquina(new BigDecimal("7.00"));
        p.setCustoMaoDeObra(BigDecimal.ZERO);
        p.setCustoTotal(new BigDecimal("8.23"));
        p.setPrecoFinal(new BigDecimal("10.70"));
        p.setStatus(StatusPedido.MODELAGEM);
        p.setPrazo(LocalDate.now());
        p.setCriadoEm(LocalDateTime.now());
        p.setAtualizadoEm(LocalDateTime.now());
        p.setImagensReferenciaFileIds(List.of("65f000000000000000000003"));
        return p;
    }

    private Orcamento orcamento(double volumeCm3) {
        Orcamento o = new Orcamento();
        o.setEquipeId(EQUIPE);
        o.setUsuarioId("u1");
        o.setMaterialId("m1");
        o.setCliente("Cliente");
        o.setProjeto("Projeto");
        o.setPrazo(LocalDate.now());
        o.setStatus(StatusOrcamento.PENDENTE);
        o.setVolumeCm3(volumeCm3);
        o.setTempoImpressaoHoras(1.0);
        o.setTempoMaoDeObraHoras(1.0);
        o.setCustoMaquinaHora(new BigDecimal("1"));
        o.setCustoMaoDeObraHora(new BigDecimal("1"));
        o.setMargemLucro(new BigDecimal("0.2"));
        o.setCustoMaterial(new BigDecimal("1.00"));
        o.setCustoMaquina(new BigDecimal("1.00"));
        o.setCustoMaoDeObra(new BigDecimal("1.00"));
        o.setCustoTotal(new BigDecimal("3.00"));
        o.setPrecoFinal(new BigDecimal("3.60"));
        o.setCriadoEm(LocalDateTime.now());
        return o;
    }

    private MovimentoEstoque movimento(BigDecimal quantidade, BigDecimal saldoApos) {
        MovimentoEstoque m = new MovimentoEstoque();
        m.setEquipeId(EQUIPE);
        m.setTipoInsumo(TipoInsumo.MATERIAL);
        m.setInsumoId("m1");
        m.setTipo(TipoMovimento.AJUSTE);
        m.setQuantidade(quantidade);
        m.setUnidade(UnidadeMedida.G);
        m.setSaldoApos(saldoApos);
        m.setCustoUnitario(new BigDecimal("0.05"));
        m.setCustoTotal(new BigDecimal("0.05").multiply(quantidade.abs()));
        m.setMotivo("Inventário");
        m.setUsuarioId("u1");
        m.setCriadoEm(LocalDateTime.now());
        m.setChaveIdempotencia(UUID.randomUUID().toString());
        return m;
    }
}
