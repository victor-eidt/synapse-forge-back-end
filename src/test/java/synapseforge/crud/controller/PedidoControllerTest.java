package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PdfService;
import synapseforge.crud.service.PedidoService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService service;

    @Mock
    private PdfService pdfService;

    @InjectMocks
    private PedidoController controller;

    @Test
    void listarDeveRetornarPedidosDoUsuario() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente A");
        PedidoResponseDTO response = new PedidoResponseDTO("p-1", "Cliente A", "Projeto", "Desc", StatusPedido.MODELAGEM, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, null);

        when(service.listar("user-1")).thenReturn(List.of(pedido));
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals(1, controller.listar(null, auth).size());
    }

    @Test
    void buscarDeveRetornarPedido() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente A");
        PedidoResponseDTO response = new PedidoResponseDTO("p-1", "Cliente A", "Projeto", "Desc", StatusPedido.MODELAGEM, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, null);

        when(service.buscarPorId("p-1", "user-1")).thenReturn(Optional.of(pedido));
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals("Cliente A", controller.buscar("p-1", auth).getCliente());
    }

    @Test
    void avancarStatusDeveRetornarPedidoAtualizado() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);
        PedidoResponseDTO response = new PedidoResponseDTO("p-1", "Cliente A", "Projeto", "Desc", StatusPedido.IMPRESSAO, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, null);

        when(service.avancarStatus("p-1", "user-1")).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals(StatusPedido.IMPRESSAO, controller.avancarStatus("p-1", auth).getStatus());
    }

    @Test
    void regredirStatusDeveRetornarPedidoAtualizado() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setStatus(StatusPedido.MODELAGEM);
        PedidoResponseDTO response = new PedidoResponseDTO("p-1", "Cliente A", "Projeto", "Desc", StatusPedido.MODELAGEM, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, null);

        when(service.regredirStatus("p-1", "user-1")).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals(StatusPedido.MODELAGEM, controller.regredirStatus("p-1", auth).getStatus());
    }

    @Test
    void atualizarDeveRetornarPedidoAtualizado() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        PedidoRequestDTO dto = new PedidoRequestDTO();
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente B");
        PedidoResponseDTO response = new PedidoResponseDTO("p-1", "Cliente B", "Projeto", "Desc", StatusPedido.MODELAGEM, LocalDate.now(), LocalDateTime.now(), LocalDateTime.now(), null, null, null);

        when(service.toEntity(dto, "user-1")).thenReturn(pedido);
        when(service.atualizar("p-1", "user-1", pedido)).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals("Cliente B", controller.atualizar("p-1", dto, auth).getCliente());
    }

    @Test
    void deletarDeveChamarService() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        controller.deletar("p-1", auth);

        verify(service).deletar("p-1", "user-1");
    }

    @Test
    void gerarOrdemServicoDeveRetornarPdf() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente A");
        when(service.buscarPorId("p-1", "user-1")).thenReturn(Optional.of(pedido));
        when(pdfService.gerarOrdemServico(pedido)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.gerarOrdemServico("p-1", auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
    }
}
