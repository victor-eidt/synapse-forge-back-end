package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.Estoque.ConsumoEtapaMetricaDTO;
import synapseforge.crud.DTO.Estoque.ConsumoInsumoMetricaDTO;
import synapseforge.crud.DTO.Estoque.ConsumoMedioSemanalResponseDTO;
import synapseforge.crud.DTO.Estoque.CustoPedidoResponseDTO;
import synapseforge.crud.DTO.Estoque.InsumoCriticoResponseDTO;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.service.EstoqueMetricasService;

@ExtendWith(MockitoExtension.class)
class EstoqueMetricasControllerTest {

    @Mock
    private EstoqueMetricasService service;

    @InjectMocks
    private EstoqueMetricasController controller;

    private Authentication auth(String principal) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        return auth;
    }

    @Test
    void consumoPorInsumo_deveDelegarParaService() {
        var dto = new ConsumoInsumoMetricaDTO(TipoInsumo.MATERIAL, "mat-1", new BigDecimal("15"), new BigDecimal("30"));
        when(service.consumoPorInsumo(LocalDate.now().minusDays(1).atStartOfDay(), LocalDate.now().atTime(23, 59, 59, 999_999_999), "user-1"))
                .thenReturn(List.of(dto));

        var result = controller.consumoPorInsumo(LocalDate.now().minusDays(1), LocalDate.now(), auth("user-1"));

        assertEquals(1, result.size());
        assertEquals("mat-1", result.get(0).getInsumoId());
    }

    @Test
    void custoPorPedido_deveDelegarParaService() {
        CustoPedidoResponseDTO dto = new CustoPedidoResponseDTO("ped-1", new BigDecimal("25.50"), List.of());
        when(service.custoPorPedido("ped-1", "user-1")).thenReturn(dto);

        var result = controller.custoPorPedido("ped-1", auth("user-1"));

        assertEquals("ped-1", result.getPedidoId());
        assertEquals(new BigDecimal("25.50"), result.getCustoTotal());
    }

    @Test
    void consumoPorEtapa_deveDelegarParaService() {
        var dto = new ConsumoEtapaMetricaDTO(StatusPedido.IMPRESSAO, new BigDecimal("8"), new BigDecimal("15"));
        when(service.consumoPorEtapa(LocalDate.now().minusDays(2).atStartOfDay(), LocalDate.now().atTime(23, 59, 59, 999_999_999), "user-1"))
                .thenReturn(List.of(dto));

        var result = controller.consumoPorEtapa(LocalDate.now().minusDays(2), LocalDate.now(), auth("user-1"));

        assertEquals(1, result.size());
        assertEquals(StatusPedido.IMPRESSAO, result.get(0).getEtapa());
    }

    @Test
    void consumoMedioSemanal_deveDelegarParaService() {
        ConsumoMedioSemanalResponseDTO dto = new ConsumoMedioSemanalResponseDTO(TipoInsumo.COR, "cor-1", 4, new BigDecimal("2.50"));
        when(service.consumoMedioSemanal(TipoInsumo.COR, "cor-1", 4, "user-1")).thenReturn(dto);

        var result = controller.consumoMedioSemanal(TipoInsumo.COR, "cor-1", 4, auth("user-1"));

        assertEquals("cor-1", result.getInsumoId());
        assertEquals(new BigDecimal("2.50"), result.getMediaSemanal());
    }

    @Test
    void insumosCriticos_deveDelegarParaService() {
        InsumoCriticoResponseDTO dto = new InsumoCriticoResponseDTO(TipoInsumo.MATERIAL, "mat-1", "Resina", UnidadeMedida.G, new BigDecimal("10"), new BigDecimal("50"), new BigDecimal("1.00"), new BigDecimal("10"));
        when(service.insumosCriticos("user-1")).thenReturn(List.of(dto));

        var result = controller.insumosCriticos(auth("user-1"));

        assertEquals(1, result.size());
        assertEquals("mat-1", result.get(0).getInsumoId());
    }
}
