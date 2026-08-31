package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.test.util.ReflectionTestUtils;
import synapseforge.crud.DTO.Pedido.PedidoRequestDTO;
import synapseforge.crud.exception.EstoqueInsuficienteException;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.repository.PedidoRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class PedidoServiceTest {

    @Mock
    private PedidoRepository repository;

    @Mock
    private GridFsTemplate gridFsTemplate;

    @Mock
    private EstoqueService estoqueService;

    @InjectMocks
    private PedidoService service;

    @Test
    void toEntityDeveMapearCampos() {
        PedidoRequestDTO dto = new PedidoRequestDTO();
        dto.setCliente("Cliente A");
        dto.setProjeto("Projeto X");
        dto.setDescricao("Desc");
        dto.setPrazo(LocalDate.now().plusDays(5));
        dto.setStatus(StatusPedido.MODELAGEM);

        Pedido pedido = service.toEntity(dto, "user-1");

        assertEquals("Cliente A", pedido.getCliente());
        assertEquals("Projeto X", pedido.getProjeto());
        assertEquals("user-1", pedido.getUsuarioId());
        assertEquals(StatusPedido.MODELAGEM, pedido.getStatus());
    }

    @Test
    void criarDeveDefinirStatusEaDatas() {
        Pedido pedido = new Pedido();
        pedido.setCliente("Cliente A");
        when(repository.save(any(Pedido.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Pedido result = service.criar(pedido);

        assertEquals(StatusPedido.MODELAGEM, result.getStatus());
        assertNotNull(result.getCriadoEm());
        assertNotNull(result.getAtualizadoEm());
    }

    @Test
    void avancarStatusDeveIrParaProximaEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.avancarStatus("p-1", "user-1");

        assertEquals(StatusPedido.IMPRESSAO, result.getStatus());
    }

    @Test
    void regredirStatusDeveVoltarEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.regredirStatus("p-1", "user-1");

        assertEquals(StatusPedido.MODELAGEM, result.getStatus());
    }

    @Test
    void avancarParaImpressaoDeveBaixarEstoqueDaEtapa() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.avancarStatus("p-1", "user-1");

        verify(estoqueService).baixarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void baixaComEstoqueInsuficienteImpedeOSaveDoPedido() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.MODELAGEM);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        doThrow(new EstoqueInsuficienteException(List.of(
                new EstoqueInsuficienteException.Falta("Resina Cinza", new BigDecimal("100"), new BigDecimal("50")))))
                .when(estoqueService).baixarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");

        assertThrows(EstoqueInsuficienteException.class, () -> service.avancarStatus("p-1", "user-1"));

        verify(repository, never()).save(any());
    }

    @Test
    void regredirDeImpressaoDeveEstornarAEtapaAbandonada() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        service.regredirStatus("p-1", "user-1");

        verify(estoqueService).estornarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void cancelarDeveDefinirCanceladoEEstornarAsEtapasPercorridas() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.cancelar("p-1", "user-1");

        assertEquals(StatusPedido.CANCELADO, result.getStatus());
        verify(estoqueService).estornarPorEtapa("p-1", StatusPedido.MODELAGEM, "user-1");
        verify(estoqueService).estornarPorEtapa("p-1", StatusPedido.IMPRESSAO, "user-1");
    }

    @Test
    void pedidoCanceladoNaoAvancaNemRegride() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setStatus(StatusPedido.CANCELADO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));

        RuntimeException aoAvancar = assertThrows(RuntimeException.class,
                () -> service.avancarStatus("p-1", "user-1"));
        RuntimeException aoRegredir = assertThrows(RuntimeException.class,
                () -> service.regredirStatus("p-1", "user-1"));

        assertEquals("Pedido cancelado não pode mudar de etapa", aoAvancar.getMessage());
        assertEquals("Pedido cancelado não pode mudar de etapa", aoRegredir.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarDeveSalvarMudancas() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setCliente("Cliente A");
        pedido.setProjeto("Projeto A");
        pedido.setDescricao("Desc A");
        pedido.setPrazo(LocalDate.now());
        pedido.setStatus(StatusPedido.MODELAGEM);

        Pedido dados = new Pedido();
        dados.setCliente("Cliente B");
        dados.setProjeto("Projeto B");
        dados.setDescricao("Desc B");
        dados.setPrazo(LocalDate.now().plusDays(2));
        dados.setStatus(StatusPedido.IMPRESSAO);

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(repository.save(pedido)).thenReturn(pedido);

        Pedido result = service.atualizar("p-1", "user-1", dados);

        assertEquals("Cliente B", result.getCliente());
        assertEquals(StatusPedido.IMPRESSAO, result.getStatus());
    }

    @Test
    void deletarDeveExcluirPedidoEArquivos() {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setUsuarioId("user-1");
        pedido.setObjeto3DFileId(null);
        pedido.setImagensReferenciaFileIds(List.of());

        when(repository.findById("p-1")).thenReturn(Optional.of(pedido));

        ReflectionTestUtils.setField(service, "gridFsTemplate", gridFsTemplate);

        service.deletar("p-1", "user-1");

        verify(repository).deleteById("p-1");
    }
}
