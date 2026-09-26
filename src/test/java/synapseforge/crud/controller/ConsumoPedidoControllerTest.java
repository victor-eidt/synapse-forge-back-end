package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ConsumoPedidoResponseDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoRequestDTO;
import synapseforge.crud.DTO.ConsumoPedido.ItemConsumoResponseDTO;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoInsumo;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.service.ConsumoPedidoService;

@ExtendWith(MockitoExtension.class)
class ConsumoPedidoControllerTest {

    @Mock
    private ConsumoPedidoService service;

    @InjectMocks
    private ConsumoPedidoController controller;

    private Authentication auth(String principal) {
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(principal);
        return authentication;
    }

    @Test
    void salvar_deveDelegarAoService() {
        ConsumoPedidoRequestDTO dto = new ConsumoPedidoRequestDTO();
        dto.setPedidoId("ped-1");

        ItemConsumoRequestDTO item = new ItemConsumoRequestDTO();
        item.setTipoInsumo(TipoInsumo.MATERIAL);
        item.setInsumoId("mat-1");
        item.setQuantidade(new BigDecimal("10"));
        item.setUnidade(UnidadeMedida.G);
        item.setEtapaConsumo(StatusPedido.IMPRESSAO);
        dto.setItens(List.of(item));

        ConsumoPedidoResponseDTO response = new ConsumoPedidoResponseDTO(
                "cons-1",
                "ped-1",
                List.of(new ItemConsumoResponseDTO(TipoInsumo.MATERIAL, "mat-1", new BigDecimal("10"), UnidadeMedida.G, StatusPedido.IMPRESSAO)),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(service.salvar(dto, "user-1")).thenReturn(response);

        ConsumoPedidoResponseDTO result = controller.salvar(dto, auth("user-1"));

        assertEquals("cons-1", result.getId());
        assertEquals("ped-1", result.getPedidoId());
        verify(service).salvar(dto, "user-1");
    }

    @Test
    void buscarPorPedido_deveDelegarAoService() {
        ConsumoPedidoResponseDTO response = new ConsumoPedidoResponseDTO(
                "cons-2",
                "ped-2",
                List.of(new ItemConsumoResponseDTO(TipoInsumo.COR, "cor-1", new BigDecimal("5"), UnidadeMedida.ML, StatusPedido.PINTURA)),
                LocalDateTime.now(),
                LocalDateTime.now()
        );

        when(service.buscarPorPedido("ped-2", "user-1")).thenReturn(response);

        ConsumoPedidoResponseDTO result = controller.buscarPorPedido("ped-2", auth("user-1"));

        assertEquals("cons-2", result.getId());
        assertEquals("cor-1", result.getItens().get(0).getInsumoId());
        verify(service).buscarPorPedido("ped-2", "user-1");
    }
}
