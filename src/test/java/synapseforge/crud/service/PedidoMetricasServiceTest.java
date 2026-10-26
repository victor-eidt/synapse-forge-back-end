package synapseforge.crud.service;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.TypedAggregation;
import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoMesMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PedidoMetricasServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private PedidoMetricasService service;

    private static final LocalDate AGOSTO = LocalDate.of(2026, 8, 1);
    private static final LocalDate FIM_DE_AGOSTO = LocalDate.of(2026, 8, 31);

    @Test
    void pedidosPorStatusDeveRetornarVazioSemEquipe() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.empty());

        assertTrue(service.pedidosPorStatus(null, null, "user-1").isEmpty());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pedidosPorStatusDeveFiltrarPelaEquipeNoPrimeiroEstagioEOrdenarPeloFluxo() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        when(mongoTemplate.aggregate(any(TypedAggregation.class), eq(PedidoStatusMetricaDTO.class)))
                .thenReturn(result(List.of(
                        new PedidoStatusMetricaDTO(StatusPedido.FINALIZADO, 3, new BigDecimal("900"), 0),
                        new PedidoStatusMetricaDTO(StatusPedido.MODELAGEM, 2, new BigDecimal("150"), 1),
                        new PedidoStatusMetricaDTO(StatusPedido.PINTURA, 1, new BigDecimal("80"), 0))));

        List<PedidoStatusMetricaDTO> resultado = service.pedidosPorStatus(null, null, "user-1");

        assertEquals(List.of(StatusPedido.MODELAGEM, StatusPedido.PINTURA, StatusPedido.FINALIZADO),
                resultado.stream().map(PedidoStatusMetricaDTO::getStatus).toList());

        ArgumentCaptor<TypedAggregation<?>> captor = ArgumentCaptor.forClass(TypedAggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq(PedidoStatusMetricaDTO.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertEquals(new Document("equipeId", "eq-1"), pipeline.get(0).get("$match"));
        assertEquals(List.of("$match", "$project", "$group", "$project"), estagios(pipeline));
    }

    @Test
    @SuppressWarnings("unchecked")
    void pedidosPorStatusDeveFiltrarPeloPeriodoNoPrimeiroMatch() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        when(mongoTemplate.aggregate(any(TypedAggregation.class), eq(PedidoStatusMetricaDTO.class)))
                .thenReturn(result(List.of()));

        service.pedidosPorStatus(AGOSTO, FIM_DE_AGOSTO, "user-1");

        ArgumentCaptor<TypedAggregation<?>> captor = ArgumentCaptor.forClass(TypedAggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq(PedidoStatusMetricaDTO.class));
        Document match = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT).get(0).get("$match", Document.class);
        assertEquals("eq-1", match.get("equipeId"));
        // "ate" inclui o dia inteiro: o limite é a meia-noite do dia seguinte, exclusivo
        assertEquals(new Document("$gte", LocalDateTime.of(2026, 8, 1, 0, 0))
                .append("$lt", LocalDateTime.of(2026, 9, 1, 0, 0)), match.get("criadoEm"));
    }

    @Test
    void pedidosPorStatusDeveRecusarPeriodoInvertido() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.pedidosPorStatus(FIM_DE_AGOSTO, AGOSTO, "user-1"));

        assertEquals("A data inicial deve ser anterior ou igual à final", erro.getMessage());
        verifyNoInteractions(mongoTemplate, equipeContexto);
    }

    @Test
    void materiaisMaisUsadosDeveValidarLimite() {
        assertThrows(IllegalArgumentException.class, () -> service.materiaisMaisUsados(0, null, null, "user-1"));
        verifyNoInteractions(mongoTemplate, equipeContexto);
    }

    @Test
    void materiaisMaisUsadosDeveRecusarPeriodoInvertido() {
        assertThrows(IllegalArgumentException.class,
                () -> service.materiaisMaisUsados(5, FIM_DE_AGOSTO, AGOSTO, "user-1"));
        verifyNoInteractions(mongoTemplate, equipeContexto);
    }

    @Test
    void materiaisMaisUsadosDeveRetornarVazioSemEquipe() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.empty());

        assertTrue(service.materiaisMaisUsados(5, null, null, "user-1").isEmpty());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void materiaisMaisUsadosDeveLimitarAntesDoLookup() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        MaterialPedidoMetricaDTO pla = new MaterialPedidoMetricaDTO("m-1", "PLA", 4, new BigDecimal("320"));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("pedidos"), eq(MaterialPedidoMetricaDTO.class)))
                .thenReturn(result(List.of(pla)));

        assertEquals(List.of(pla), service.materiaisMaisUsados(3, null, null, "user-1"));

        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq("pedidos"), eq(MaterialPedidoMetricaDTO.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertEquals(List.of("$match", "$project", "$group", "$sort", "$limit", "$addFields", "$lookup",
                "$unwind", "$project"), estagios(pipeline));

        Document match = pipeline.get(0).get("$match", Document.class);
        assertEquals("eq-1", match.get("equipeId"));
        assertEquals(new Document("$ne", "CANCELADO"), match.get("status"));
        assertEquals(3L, ((Number) pipeline.get(4).get("$limit")).longValue());
        assertEquals("materiais", pipeline.get(6).get("$lookup", Document.class).get("from"));
        assertNull(match.get("criadoEm"));
    }

    @Test
    void materiaisMaisUsadosDeveAceitarPeriodoAbertoDeUmLado() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("pedidos"), eq(MaterialPedidoMetricaDTO.class)))
                .thenReturn(result(List.of()));

        service.materiaisMaisUsados(5, AGOSTO, null, "user-1");
        service.materiaisMaisUsados(5, null, FIM_DE_AGOSTO, "user-1");

        ArgumentCaptor<Aggregation> captor = ArgumentCaptor.forClass(Aggregation.class);
        verify(mongoTemplate, times(2)).aggregate(captor.capture(), eq("pedidos"), eq(MaterialPedidoMetricaDTO.class));
        Document soDe = captor.getAllValues().get(0).toPipeline(Aggregation.DEFAULT_CONTEXT).get(0).get("$match", Document.class);
        Document soAte = captor.getAllValues().get(1).toPipeline(Aggregation.DEFAULT_CONTEXT).get(0).get("$match", Document.class);
        assertEquals(new Document("$gte", LocalDateTime.of(2026, 8, 1, 0, 0)), soDe.get("criadoEm"));
        assertEquals(new Document("$lt", LocalDateTime.of(2026, 9, 1, 0, 0)), soAte.get("criadoEm"));
        // o período entra junto com os demais critérios, sem tirar nenhum
        assertEquals("eq-1", soDe.get("equipeId"));
        assertEquals(new Document("$ne", "CANCELADO"), soAte.get("status"));
    }

    @Test
    void pedidosPorMesDeveRetornarVazioSemEquipe() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.empty());

        assertTrue(service.pedidosPorMes(AGOSTO, FIM_DE_AGOSTO, "user-1").isEmpty());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void pedidosPorMesDeveRecusarPeriodoInvertido() {
        IllegalArgumentException erro = assertThrows(IllegalArgumentException.class,
                () -> service.pedidosPorMes(FIM_DE_AGOSTO, AGOSTO, "user-1"));

        assertEquals("A data inicial deve ser anterior ou igual à final", erro.getMessage());
        verifyNoInteractions(mongoTemplate, equipeContexto);
    }

    @Test
    @SuppressWarnings("unchecked")
    void pedidosPorMesDeveAgruparPeloMesNoFusoDaJvmEOrdenarPeloMes() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        PedidoMesMetricaDTO agosto = new PedidoMesMetricaDTO("2026-08", 7, 1, new BigDecimal("1830.50"));
        when(mongoTemplate.aggregate(any(TypedAggregation.class), eq(PedidoMesMetricaDTO.class)))
                .thenReturn(result(List.of(agosto)));

        assertEquals(List.of(agosto), service.pedidosPorMes(AGOSTO, null, "user-1"));

        ArgumentCaptor<TypedAggregation<?>> captor = ArgumentCaptor.forClass(TypedAggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq(PedidoMesMetricaDTO.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertEquals(List.of("$match", "$project", "$group", "$project", "$sort"), estagios(pipeline));

        assertEquals(new Document("equipeId", "eq-1")
                        .append("criadoEm", new Document("$gte", LocalDateTime.of(2026, 8, 1, 0, 0))),
                pipeline.get(0).get("$match"));

        Document mes = pipeline.get(1).get("$project", Document.class)
                .get("mes", Document.class).get("$dateToString", Document.class);
        assertEquals("%Y-%m", mes.get("format"));
        assertEquals("$criadoEm", mes.get("date"));
        assertEquals(ZoneId.systemDefault().getId(), mes.get("timezone"));

        Document grupo = pipeline.get(2).get("$group", Document.class);
        assertEquals("$mes", grupo.get("_id"));
        assertEquals(new Document("$sum", "$ativo"), grupo.get("quantidade"));
        assertEquals(new Document("$sum", "$cancelado"), grupo.get("cancelados"));
        assertEquals(new Document("$sum", "$valor"), grupo.get("valorTotal"));

        assertEquals(new Document("mes", 1), pipeline.get(4).get("$sort"));
    }

    private static List<String> estagios(List<Document> pipeline) {
        return pipeline.stream().map(estagio -> estagio.keySet().iterator().next()).toList();
    }

    private static <T> AggregationResults<T> result(List<T> mapped) {
        return new AggregationResults<>(mapped, new Document());
    }
}
