package synapseforge.crud.service;

import org.bson.Document;
import org.bson.types.Decimal128;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import synapseforge.crud.DTO.Estoque.AlertaEstoqueResponseDTO;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EstoqueMetricasServiceTest {

    @Mock
    private MongoTemplate mongoTemplate;

    @Mock
    private EstoqueService estoqueService;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private EstoqueMetricasService service;

    @Test
    void deveExecutarConsultasDeConsumoECalcularCustoDoPedido() {
        when(mongoTemplate.aggregate(any(), eq(synapseforge.crud.DTO.Estoque.ConsumoInsumoMetricaDTO.class)))
                .thenReturn(result(List.of()));
        when(mongoTemplate.aggregate(any(), eq(synapseforge.crud.DTO.Estoque.ConsumoEtapaMetricaDTO.class)))
                .thenReturn(result(List.of()));
        when(mongoTemplate.aggregate(any(), eq(synapseforge.crud.DTO.Estoque.CustoEtapaMetricaDTO.class)))
                .thenReturn(result(List.of()));
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));

        assertTrue(service.consumoPorInsumo(LocalDateTime.now().minusDays(1), LocalDateTime.now(), "user-1").isEmpty());
        assertTrue(service.consumoPorEtapa(LocalDateTime.now().minusDays(1), LocalDateTime.now(), "user-1").isEmpty());
        assertEquals(BigDecimal.ZERO, service.custoPorPedido("p-1", "user-1").getCustoTotal());
    }

    @Test
    void consumoMedioSemanalDeveValidarSemanasERetornarZeroSemDados() {
        assertThrows(IllegalArgumentException.class,
                () -> service.consumoMedioSemanal(TipoInsumo.MATERIAL, "m-1", 0, "user-1"));
        when(mongoTemplate.aggregate(any(), eq(Document.class))).thenReturn(result(List.of()));
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));

        var response = service.consumoMedioSemanal(TipoInsumo.MATERIAL, "m-1", 4, "user-1");

        assertEquals(BigDecimal.ZERO, response.getMediaSemanal());
        assertEquals(4, response.getSemanasConsideradas());
    }

    @Test
    void insumosCriticosDeveRetornarVazioSemAlertas() {
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        when(estoqueService.listarEmAlertaDaEquipe("eq-1")).thenReturn(List.of());
        assertTrue(service.insumosCriticos("user-1").isEmpty());
        verifyNoInteractions(mongoTemplate);
    }

    @Test
    void insumosCriticosDeveCalcularCoberturaEOrdenar() {
        AlertaEstoqueResponseDTO alerta = new AlertaEstoqueResponseDTO(
                TipoInsumo.MATERIAL, "m-1", "PLA", UnidadeMedida.G,
                BigDecimal.valueOf(100), BigDecimal.valueOf(20));
        when(equipeContexto.equipeDe("user-1")).thenReturn(Optional.of("eq-1"));
        when(estoqueService.listarEmAlertaDaEquipe("eq-1")).thenReturn(List.of(alerta));
        Document id = new Document("tipoInsumo", "MATERIAL").append("insumoId", "m-1");
        Document consumo = new Document("_id", id)
                .append("totalConsumido", new Decimal128(BigDecimal.valueOf(50)));
        when(mongoTemplate.aggregate(any(), eq(Document.class))).thenReturn(result(List.of(consumo)));

        var result = service.insumosCriticos("user-1");

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("60.0"), result.get(0).getDiasCobertura());
        assertEquals(new BigDecimal("1.6667"), result.get(0).getConsumoMedioDiario());
    }

    private <T> AggregationResults<T> result(List<T> values) {
        return new AggregationResults<>(values, new Document());
    }
}
