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
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.infrastructure.entity.Notificacao;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.NotificacaoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class NotificacaoServiceTest {

    @Mock
    private NotificacaoRepository repository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmailService emailService;

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

        verifyNoInteractions(repository, emailService);
    }

    private User cliente(String email) {
        User u = new User();
        u.setId("cli-1");
        u.setNome("Ana");
        u.setEmail(email);

        u.setEmailConfirmado(true);
        return u;
    }

    @Test
    void pedidoFinalizadoDeveEnviarEmailAoCliente() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente("ana@x.com")));

        service.notificarPedidoFinalizado(pedido("cli-1"));

        verify(emailService).enviarPedidoFinalizado("ana@x.com", "Ana", "Miniatura dragão", "p-1");
    }

    @Test
    void clienteSemEmailNaoDeveEnviarMasMantemOAviso() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente(" ")));

        service.notificarPedidoFinalizado(pedido("cli-1"));

        verify(repository).save(any());
        verifyNoInteractions(emailService);
    }

    @Test

    void clienteComEmailNaoConfirmadoNaoDeveReceberEmail() {
        User u = cliente("ana@x.com");
        u.setEmailConfirmado(false);
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(u));

        service.notificarPedidoFinalizado(pedido("cli-1"));

        verify(repository).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void falhaNoEmailNaoDevePropagar() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente("ana@x.com")));
        doThrow(new RuntimeException("smtp fora")).when(emailService)
                .enviarPedidoFinalizado(any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notificarPedidoFinalizado(pedido("cli-1")));
        verify(repository).save(any());
    }

    @Test
    void naoDeveDuplicarAvisoAindaNaoLido() {
        when(repository.existsByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
                "cli-1", TipoNotificacao.PEDIDO_FINALIZADO, "p-1")).thenReturn(true);

        service.notificarPedidoFinalizado(pedido("cli-1"));

        verify(repository, never()).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void falhaAoSalvarNaoDevePropagar() {
        when(repository.save(any())).thenThrow(new RuntimeException("mongo fora"));

        assertDoesNotThrow(() -> service.notificarPedidoFinalizado(pedido("cli-1")));
    }

    @Test
    void marcarComoLidaDeveExigirQueSejaDoUsuario() {
        when(repository.findByIdAndUsuarioId("n-1", "outro")).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class, () -> service.marcarComoLida("n-1", "outro"));
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

    @Test
    void listarNaoLidasDeveUsarConsultaLimitada() {
        Notificacao a = new Notificacao();
        when(repository.findTop50ByUsuarioIdAndLidaFalseOrderByCriadaEmDesc("cli-1")).thenReturn(List.of(a));

        assertEquals(List.of(a), service.listar("cli-1", true));
        verify(repository, never()).findByUsuarioIdAndLidaFalseOrderByCriadaEmDesc(any());
    }
}
