package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.infrastructure.entity.Notificacao;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;
import synapseforge.crud.infrastructure.repository.NotificacaoRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository repository;

    @InjectMocks
    private NotificacaoService service;

    private Pedido pedido(String clienteId) {
        Pedido pedido = new Pedido();
        pedido.setId("p-1");
        pedido.setProjeto("Miniatura dragão");
        pedido.setClienteId(clienteId);
        return pedido;
    }

    @Test
    void pedidoFinalizadoDeveCriarNotificacaoParaOCliente() {
        service.notificarPedidoFinalizado(pedido("cli-1"));

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        Notificacao salva = captor.getValue();

        assertEquals("cli-1", salva.getUsuarioId());
        assertEquals(TipoNotificacao.PEDIDO_FINALIZADO, salva.getTipo());
        assertEquals("p-1", salva.getReferenciaId());
        assertEquals("Miniatura dragão", salva.getTitulo());
        assertFalse(salva.isLida());
        assertNotNull(salva.getCriadaEm());
    }

    @Test
    void pedidoSemClienteNaoDeveNotificar() {
        service.notificarPedidoFinalizado(pedido(null));

        verifyNoInteractions(repository);
    }

    @Test
    void naoDeveDuplicarAvisoAindaNaoLido() {
        when(repository.existsByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
                "cli-1", TipoNotificacao.PEDIDO_FINALIZADO, "p-1")).thenReturn(true);

        service.notificarPedidoFinalizado(pedido("cli-1"));

        verify(repository, never()).save(any());
    }

    @Test
    void falhaAoSalvarNaoDevePropagar() {
        when(repository.save(any())).thenThrow(new RuntimeException("mongo fora"));

        assertDoesNotThrow(() -> service.notificarPedidoFinalizado(pedido("cli-1")));
    }

    @Test
    void marcarComoLidaDeveExigirQueSejaDoUsuario() {
        when(repository.findByIdAndUsuarioId("n-1", "outro")).thenReturn(Optional.empty());

        assertThrows(RuntimeException.class, () -> service.marcarComoLida("n-1", "outro"));
        verify(repository, never()).save(any());
    }

    @Test
    void marcarComoLidaDevePreencherDataDeLeitura() {
        Notificacao n = new Notificacao();
        n.setId("n-1");
        n.setUsuarioId("cli-1");
        when(repository.findByIdAndUsuarioId("n-1", "cli-1")).thenReturn(Optional.of(n));
        when(repository.save(n)).thenReturn(n);

        Notificacao result = service.marcarComoLida("n-1", "cli-1");

        assertTrue(result.isLida());
        assertNotNull(result.getLidaEm());
    }

    @Test
    void marcarTodasComoLidasDeveAtualizarSoAsNaoLidas() {
        Notificacao a = new Notificacao();
        Notificacao b = new Notificacao();
        when(repository.findByUsuarioIdAndLidaFalseOrderByCriadaEmDesc("cli-1")).thenReturn(List.of(a, b));

        service.marcarTodasComoLidas("cli-1");

        assertTrue(a.isLida());
        assertTrue(b.isLida());
        verify(repository).saveAll(List.of(a, b));
    }
}
