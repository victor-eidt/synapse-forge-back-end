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
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.service.PdfService;
import synapseforge.crud.service.PedidoService;

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

    private Authentication authAdmin() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");
        doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
                .when(auth).getAuthorities();
        return auth;
    }

    @Test
    void listarDeveRetornarPedidosDoUsuario() {
        Authentication auth = authAdmin();

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente A");
        PedidoResponseDTO response = mock(PedidoResponseDTO.class);

        when(service.listar("user-1", Role.ADMIN)).thenReturn(List.of(pedido));
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals(1, controller.listar(null, auth).size());
    }

    @Test
    void buscarDeveRetornarPedido() {
        Authentication auth = authAdmin();

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente A");
        PedidoResponseDTO response = mock(PedidoResponseDTO.class);
        when(response.getCliente()).thenReturn("Cliente A");

        when(service.buscarPorId("p-1", "user-1", Role.ADMIN)).thenReturn(Optional.of(pedido));
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals("Cliente A", controller.buscar("p-1", auth).getCliente());
    }

    @Test
    void avancarStatusDeveRetornarPedidoAtualizado() {
        Authentication auth = authAdmin();

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);
        PedidoResponseDTO response = mock(PedidoResponseDTO.class);
        when(response.getStatus()).thenReturn(StatusPedido.IMPRESSAO);

        when(service.avancarStatus("p-1", "user-1", Role.ADMIN)).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals(StatusPedido.IMPRESSAO, controller.avancarStatus("p-1", auth).getStatus());
    }

    @Test
    void regredirStatusDeveRetornarPedidoAtualizado() {
        Authentication auth = authAdmin();

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setStatus(StatusPedido.MODELAGEM);
        PedidoResponseDTO response = mock(PedidoResponseDTO.class);
        when(response.getStatus()).thenReturn(StatusPedido.MODELAGEM);

        when(service.regredirStatus("p-1", "user-1", Role.ADMIN)).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals(StatusPedido.MODELAGEM, controller.regredirStatus("p-1", auth).getStatus());
    }

    @Test
    void atualizarDeveRetornarPedidoAtualizado() {
        Authentication auth = authAdmin();

        PedidoRequestDTO dto = new PedidoRequestDTO();
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente B");
        PedidoResponseDTO response = mock(PedidoResponseDTO.class);
        when(response.getCliente()).thenReturn("Cliente B");

        when(service.toEntity(dto, "user-1")).thenReturn(pedido);
        when(service.atualizar("p-1", "user-1", Role.ADMIN, pedido)).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(response);

        assertEquals("Cliente B", controller.atualizar("p-1", dto, auth).getCliente());
    }

    @Test
    void deletarDeveChamarService() {
        Authentication auth = authAdmin();

        controller.deletar("p-1", auth);

        verify(service).deletar("p-1", "user-1", Role.ADMIN);
    }

    @Test
    void gerarOrdemServicoDeveRetornarPdf() {
        Authentication auth = authAdmin();

        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setCliente("Cliente A");
        when(service.buscarPorId("p-1", "user-1", Role.ADMIN)).thenReturn(Optional.of(pedido));
        when(pdfService.gerarOrdemServico(pedido)).thenReturn(new byte[]{1, 2, 3});

        ResponseEntity<byte[]> response = controller.gerarOrdemServico("p-1", auth);

        assertEquals(200, response.getStatusCode().value());
        assertEquals("application/pdf", response.getHeaders().getContentType().toString());
    }
}
