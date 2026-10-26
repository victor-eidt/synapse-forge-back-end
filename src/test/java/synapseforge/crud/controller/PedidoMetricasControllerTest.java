package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PedidoMetricasService;

@ExtendWith(MockitoExtension.class)
class PedidoMetricasControllerTest {

    @Mock
    private PedidoMetricasService service;

    @InjectMocks
    private PedidoMetricasController controller;

    private Authentication auth(String principal) {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(principal);
        return auth;
    }

    @Test
    void pedidosPorStatus_deveDelegarParaService() {
        var dto = new PedidoStatusMetricaDTO(StatusPedido.IMPRESSAO, 4, new BigDecimal("600"), 1);
        when(service.pedidosPorStatus("user-1")).thenReturn(List.of(dto));

        var result = controller.pedidosPorStatus(auth("user-1"));

        assertEquals(1, result.size());
        assertEquals(StatusPedido.IMPRESSAO, result.get(0).getStatus());
    }

    @Test
    void materiaisMaisUsados_deveDelegarParaService() {
        var dto = new MaterialPedidoMetricaDTO("mat-1", "PLA", 7, new BigDecimal("910"));
        when(service.materiaisMaisUsados(5, "user-1")).thenReturn(List.of(dto));

        var result = controller.materiaisMaisUsados(5, auth("user-1"));

        assertEquals("mat-1", result.get(0).getMaterialId());
    }
}
