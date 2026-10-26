package synapseforge.crud.service;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.test.context.TestPropertySource;
import synapseforge.crud.DTO.Pedido.PedidoMesMetricaDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.User;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Roda a aggregation de pedidos por mês no MongoDB embutido (mesma configuração do
 * ApplicationTests, então o contexto é reaproveitado): o teste de unidade só confere o
 * pipeline, aqui confere o resultado ($dateToString no fuso da JVM, $$REMOVE, limites do período).
 */
@SpringBootTest
@TestPropertySource(properties = {
    "spring.data.mongodb.auto-index-creation=true",
    "de.flapdoodle.mongodb.embedded.version=7.0.0",
    "app.jwt.secret=c2VncmVkby1kZS10ZXN0ZS1jb20tMzItYnl0ZXMhISE="
})
class PedidoMetricasPorMesMongoTest {

    private static final String EQUIPE = "eq-teste-metricas-mes";
    private static final String GERENTE = "u-teste-metricas-mes";

    @Autowired
    private MongoTemplate mongoTemplate;

    @Autowired
    private PedidoMetricasService service;

    @BeforeEach
    void criarPedidos() {
        User gerente = new User();
        gerente.setId(GERENTE);
        gerente.setNome("Gerente");
        gerente.setEmail("gerente-metricas-mes@teste.com");
        gerente.setRole(Role.GERENTE);
        gerente.setEquipeId(EQUIPE);
        gerente.setAtivo(true);
        mongoTemplate.insert(gerente);

        mongoTemplate.insertAll(List.of(
                pedido(EQUIPE, LocalDateTime.of(2026, 7, 31, 23, 59), StatusPedido.FINALIZADO, "40.00"),
                pedido(EQUIPE, LocalDateTime.of(2026, 8, 1, 0, 0), StatusPedido.MODELAGEM, "100.00"),
                // fim de agosto à noite: em UTC já seria setembro
                pedido(EQUIPE, LocalDateTime.of(2026, 8, 31, 22, 30), StatusPedido.FINALIZADO, "50.50"),
                pedido(EQUIPE, LocalDateTime.of(2026, 8, 15, 10, 0), StatusPedido.CANCELADO, "999.00"),
                pedido(EQUIPE, LocalDateTime.of(2026, 9, 10, 9, 0), StatusPedido.IMPRESSAO, null),
                pedido(EQUIPE, LocalDateTime.of(2026, 9, 30, 23, 59), StatusPedido.CANCELADO, "10.00"),
                pedido(EQUIPE, LocalDateTime.of(2026, 10, 1, 0, 0), StatusPedido.PINTURA, "30.00"),
                pedido("outra-equipe-metricas-mes", LocalDateTime.of(2026, 8, 10, 10, 0), StatusPedido.MODELAGEM, "500.00")));
    }

    @AfterEach
    void limpar() {
        mongoTemplate.remove(Query.query(Criteria.where("equipeId")
                .in(EQUIPE, "outra-equipe-metricas-mes")), Pedido.class);
        mongoTemplate.remove(Query.query(Criteria.where("_id").is(GERENTE)), User.class);
    }

    @Test
    void agrupaPorMesSeparandoCanceladosDentroDoPeriodo() {
        List<PedidoMesMetricaDTO> meses = service.pedidosPorMes(
                LocalDate.of(2026, 8, 1), LocalDate.of(2026, 9, 30), GERENTE);

        assertEquals(List.of("2026-08", "2026-09"), meses.stream().map(PedidoMesMetricaDTO::getMes).toList());

        PedidoMesMetricaDTO agosto = meses.get(0);
        assertEquals(2, agosto.getQuantidade());
        assertEquals(1, agosto.getCancelados());
        assertEquals(0, new BigDecimal("150.50").compareTo(agosto.getValorTotal()));

        // o único não cancelado de setembro não tem preço: valor null, não zero
        PedidoMesMetricaDTO setembro = meses.get(1);
        assertEquals(1, setembro.getQuantidade());
        assertEquals(1, setembro.getCancelados());
        assertNull(setembro.getValorTotal());
    }

    @Test
    void semPeriodoTrazTodosOsMesesEmOrdem() {
        List<PedidoMesMetricaDTO> meses = service.pedidosPorMes(null, null, GERENTE);

        assertEquals(List.of("2026-07", "2026-08", "2026-09", "2026-10"),
                meses.stream().map(PedidoMesMetricaDTO::getMes).toList());
        assertEquals(0, new BigDecimal("30.00").compareTo(meses.get(3).getValorTotal()));
    }

    private Pedido pedido(String equipeId, LocalDateTime criadoEm, StatusPedido status, String precoFinal) {
        Pedido p = new Pedido();
        p.setEquipeId(equipeId);
        p.setUsuarioId(GERENTE);
        p.setProjeto("Projeto");
        p.setStatus(status);
        p.setPrecoFinal(precoFinal == null ? null : new BigDecimal(precoFinal));
        p.setCriadoEm(criadoEm);
        p.setAtualizadoEm(criadoEm);
        return p;
    }
}
