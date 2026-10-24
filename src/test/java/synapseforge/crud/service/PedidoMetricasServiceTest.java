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
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.infrastructure.entity.StatusPedido;

import java.math.BigDecimal;
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

    @Test
    void pedidosPorStatusDeveRetornarVazioSemEquipe() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.empty());

        assertTrue(service.pedidosPorStatus("user-1").isEmpty());
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

        List<PedidoStatusMetricaDTO> resultado = service.pedidosPorStatus("user-1");

        assertEquals(List.of(StatusPedido.MODELAGEM, StatusPedido.PINTURA, StatusPedido.FINALIZADO),
                resultado.stream().map(PedidoStatusMetricaDTO::getStatus).toList());

        ArgumentCaptor<TypedAggregation<?>> captor = ArgumentCaptor.forClass(TypedAggregation.class);
        verify(mongoTemplate).aggregate(captor.capture(), eq(PedidoStatusMetricaDTO.class));
        List<Document> pipeline = captor.getValue().toPipeline(Aggregation.DEFAULT_CONTEXT);
        assertEquals(new Document("equipeId", "eq-1"), pipeline.get(0).get("$match"));
        assertEquals(List.of("$match", "$project", "$group", "$project"), estagios(pipeline));
    }

    @Test
    void materiaisMaisUsadosDeveValidarLimite() {
        assertThrows(IllegalArgumentException.class, () -> service.materiaisMaisUsados(0, "user-1"));
        verifyNoInteractions(mongoTemplate, equipeContexto);
    }

    @Test
    void materiaisMaisUsadosDeveRetornarVazioSemEquipe() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.empty());

        assertTrue(service.materiaisMaisUsados(5, "user-1").isEmpty());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void materiaisMaisUsadosDeveLimitarAntesDoLookup() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        MaterialPedidoMetricaDTO pla = new MaterialPedidoMetricaDTO("m-1", "PLA", 4, new BigDecimal("320"));
        when(mongoTemplate.aggregate(any(Aggregation.class), eq("pedidos"), eq(MaterialPedidoMetricaDTO.class)))
                .thenReturn(result(List.of(pla)));

        assertEquals(List.of(pla), service.materiaisMaisUsados(3, "user-1"));

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
    }

    private static List<String> estagios(List<Document> pipeline) {
        return pipeline.stream().map(estagio -> estagio.keySet().iterator().next()).toList();
    }

    private static <T> AggregationResults<T> result(List<T> mapped) {
        return new AggregationResults<>(mapped, new Document());
    }
}
