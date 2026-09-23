package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.DTO.Pedido.PedidoResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.service.PdfService;
import synapseforge.crud.service.PedidoService;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PedidoControllerTest {

    @Mock
    private PedidoService service;

    @Mock
    private PdfService pdfService;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @InjectMocks
    private PedidoController controller;

    private Authentication authAdmin() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");
        // lenient: a criação não lê o perfil (o @PreAuthorize cuida disso)
        lenient().doReturn(List.of(new SimpleGrantedAuthority("ROLE_ADMIN")))
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

    // =========================================================
    // MULTIPART: VALIDA ANTES DE GRAVAR NO GRIDFS (SYN-100)
    // =========================================================

    private MockMultipartFile arquivo(String nome) {
        return new MockMultipartFile(nome, nome + ".bin", "application/octet-stream", new byte[]{1, 2, 3});
    }

    private PedidoResponseDTO criarMultipart(Authentication auth) throws Exception {
        return controller.criarComArquivos(null, "", "Projeto", null, LocalDate.now(),
                null, null, null, null, null, null, null, null, null, null, null, null,
                arquivo("obj"), new MultipartFile[]{arquivo("img")}, auth);
    }

    private PedidoResponseDTO atualizarMultipart(Authentication auth) throws Exception {
        return controller.atualizarComArquivos("p-1", null, "", "Projeto", null, LocalDate.now(),
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, arquivo("obj"), false, new MultipartFile[]{arquivo("img")}, null, auth);
    }

    @Test
    void criarMultipartRecusadoNaoGravaArquivos() {
        Authentication auth = authAdmin();
        Pedido pedido = new Pedido();
        when(service.toEntity(any(PedidoRequestDTO.class), eq("user-1"))).thenReturn(pedido);
        doThrow(new SemEquipeException()).when(service).prepararParaCriacao(pedido);

        assertThrows(SemEquipeException.class, () -> criarMultipart(auth));

        verifyNoInteractions(gridFsTemplate);
        verify(service, never()).criar(any());
    }

    @Test
    void criarMultipartValidoGravaArquivosDepoisDaValidacao() throws Exception {
        Authentication auth = authAdmin();
        Pedido pedido = new Pedido();
        when(service.toEntity(any(PedidoRequestDTO.class), eq("user-1"))).thenReturn(pedido);
        when(gridFsTemplate.store(any(InputStream.class), anyString(), anyString())).thenReturn(new ObjectId());
        when(service.criar(pedido)).thenReturn(pedido);
        when(service.toResponseDTO(pedido)).thenReturn(mock(PedidoResponseDTO.class));

        criarMultipart(auth);

        var ordem = inOrder(service, gridFsTemplate);
        ordem.verify(service).prepararParaCriacao(pedido);
        ordem.verify(gridFsTemplate, times(2)).store(any(InputStream.class), anyString(), anyString());
        ordem.verify(service).criar(pedido);
    }

    @Test
    void atualizarMultipartRecusadoNaoGravaArquivos() {
        Authentication auth = authAdmin();
        Pedido dados = new Pedido();
        when(service.toEntity(any(PedidoRequestDTO.class), eq("user-1"))).thenReturn(dados);
        doThrow(new RuntimeException("Pedido não encontrado"))
                .when(service).validarAtualizacao("p-1", "user-1", Role.ADMIN, dados);

        RuntimeException ex = assertThrows(RuntimeException.class, () -> atualizarMultipart(auth));

        assertEquals("Pedido não encontrado", ex.getMessage());
        verifyNoInteractions(gridFsTemplate);
        verify(service, never()).atualizarComArquivos(any(), any(), any(), any(), any(), anyBoolean(), any(), any());
    }
}
