package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import synapseforge.crud.DTO.Pedido.MaterialPedidoMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoMesMetricaDTO;
import synapseforge.crud.DTO.Pedido.PedidoStatusMetricaDTO;
import synapseforge.crud.exception.GlobalExceptionHandler;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.EquipeContexto;
import synapseforge.crud.service.PedidoMetricasService;

@ExtendWith(MockitoExtension.class)
class PedidoMetricasControllerTest {

    private static final LocalDate DE = LocalDate.of(2026, 8, 1);
    private static final LocalDate ATE = LocalDate.of(2026, 10, 31);

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
        when(service.pedidosPorStatus(DE, ATE, "user-1")).thenReturn(List.of(dto));

        var result = controller.pedidosPorStatus(DE, ATE, auth("user-1"));

        assertEquals(1, result.size());
        assertEquals(StatusPedido.IMPRESSAO, result.get(0).getStatus());
    }

    @Test
    void materiaisMaisUsados_deveDelegarParaService() {
        var dto = new MaterialPedidoMetricaDTO("mat-1", "PLA", 7, new BigDecimal("910"));
        when(service.materiaisMaisUsados(5, null, null, "user-1")).thenReturn(List.of(dto));

        var result = controller.materiaisMaisUsados(5, null, null, auth("user-1"));

        assertEquals("mat-1", result.get(0).getMaterialId());
    }

    @Test
    void pedidosPorMes_deveDelegarParaService() {
        var dto = new PedidoMesMetricaDTO("2026-08", 7, 1, new BigDecimal("1830.50"));
        when(service.pedidosPorMes(DE, null, "user-1")).thenReturn(List.of(dto));

        var result = controller.pedidosPorMes(DE, null, auth("user-1"));

        assertEquals("2026-08", result.get(0).getMes());
    }

    // contrato com o front: nomes dos parâmetros, formato ISO das datas e o JSON de resposta
    @Test
    void pedidosPorMes_deveLerOPeriodoDaQueryString() throws Exception {
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
        when(service.pedidosPorMes(DE, ATE, "user-1"))
                .thenReturn(List.of(new PedidoMesMetricaDTO("2026-08", 7, 1, new BigDecimal("1830.50"))));

        mockMvc.perform(get("/pedidos/metricas/por-mes").param("de", "2026-08-01").param("ate", "2026-10-31")
                        .principal(new UsernamePasswordAuthenticationToken("user-1", null)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].mes").value("2026-08"))
                .andExpect(jsonPath("$[0].quantidade").value(7))
                .andExpect(jsonPath("$[0].cancelados").value(1))
                .andExpect(jsonPath("$[0].valorTotal").value(1830.50));
    }

    // service de verdade (só o banco é mock): a validação do período chega como 400 com a mensagem
    @Test
    void periodoInvertido_deveResponder400() throws Exception {
        EquipeContexto equipeContexto = mock(EquipeContexto.class);
        PedidoMetricasController comServiceReal = new PedidoMetricasController(
                new PedidoMetricasService(mock(MongoTemplate.class), equipeContexto));
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(comServiceReal)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        for (String rota : List.of("/pedidos/metricas/por-status", "/pedidos/metricas/materiais-mais-usados",
                "/pedidos/metricas/por-mes")) {
            mockMvc.perform(get(rota).param("de", "2026-10-31").param("ate", "2026-08-01")
                            .principal(new UsernamePasswordAuthenticationToken("user-1", null)))
                    .andExpect(status().isBadRequest())
                    .andExpect(content().string("A data inicial deve ser anterior ou igual à final"));
        }
        verifyNoInteractions(equipeContexto);
    }
}
