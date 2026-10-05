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
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.NotificacaoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDate;
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
    void finalizarDeveGerarAvisoDeEtapaComDetalheFinalizado() {
        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.FINALIZADO));

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        Notificacao salva = captor.getValue();

        // finalizado é só mais uma etapa: mesmo tipo, etapa no detalhe
        assertEquals("cli-1", salva.getUsuarioId());
        assertEquals(TipoNotificacao.PEDIDO_ETAPA_ALTERADA, salva.getTipo());
        assertEquals("FINALIZADO", salva.getDetalhe());
        assertEquals("p-1", salva.getReferenciaId());
        assertEquals("Miniatura dragão", salva.getTitulo());
        assertFalse(salva.isLida());
        assertNotNull(salva.getCriadaEm());
    }

    @Test
    void pedidoSemClienteNaoDeveNotificar() {
        Pedido semCliente = pedido(null);
        semCliente.setStatus(StatusPedido.FINALIZADO);

        service.notificarEtapaAlterada(semCliente);

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
    void finalizarDeveEnviarEmailComAEtapaFinalizado() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente("ana@x.com")));

        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.FINALIZADO));

        verify(emailService).enviarPedidoEtapaAlterada("ana@x.com", "Ana", "Miniatura dragão", "p-1", StatusPedido.FINALIZADO);
    }

    @Test
    void clienteSemEmailNaoDeveEnviarMasMantemOAviso() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente(" ")));

        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.PINTURA));

        verify(repository).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void clienteComEmailNaoConfirmadoNaoDeveReceberEmail() {
        User u = cliente("ana@x.com");
        u.setEmailConfirmado(false);
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(u));

        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.PINTURA));

        verify(repository).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void falhaNoEmailNaoDevePropagar() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente("ana@x.com")));
        doThrow(new RuntimeException("smtp fora")).when(emailService)
                .enviarPedidoEtapaAlterada(any(), any(), any(), any(), any());

        assertDoesNotThrow(() -> service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.FINALIZADO)));
        verify(repository).save(any());
    }

    @Test
    void falhaAoSalvarNaoDevePropagar() {
        when(repository.save(any())).thenThrow(new RuntimeException("mongo fora"));

        assertDoesNotThrow(() -> service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.PINTURA)));
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

    // =========================================================
    // ORDEM DE PINTURA ATRIBUÍDA
    // =========================================================

    private User tecnico(boolean emailConfirmado) {
        User u = new User();
        u.setId("tec-1");
        u.setNome("José");
        u.setEmail("jose@x.com");
        u.setEmailConfirmado(emailConfirmado);
        return u;
    }

    @Test
    void ordemAtribuidaDeveAvisarOTecnicoNoSinoENoEmail() {
        LocalDate prazo = LocalDate.of(2026, 10, 15);
        when(userRepository.findById("tec-1")).thenReturn(Optional.of(tecnico(true)));

        service.notificarOrdemPinturaAtribuida("ord-1", "tec-1", "gerente-1", "Vaso", "Azul", prazo);

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        Notificacao salva = captor.getValue();
        assertEquals("tec-1", salva.getUsuarioId());
        assertEquals(TipoNotificacao.ORDEM_PINTURA_ATRIBUIDA, salva.getTipo());
        assertEquals("ord-1", salva.getReferenciaId());
        assertEquals("Vaso", salva.getTitulo());
        assertFalse(salva.isLida());

        verify(emailService).enviarOrdemPinturaAtribuida("jose@x.com", "José", "Vaso", "Azul", prazo);
    }

    @Test
    void quemAtribuiAOrdemASiMesmoNaoRecebeAviso() {
        service.notificarOrdemPinturaAtribuida("ord-1", "tec-1", "tec-1", "Vaso", "Azul", LocalDate.now());

        verifyNoInteractions(repository, emailService, userRepository);
    }

    @Test
    void tecnicoSemEmailConfirmadoRecebeSoNoSino() {
        when(userRepository.findById("tec-1")).thenReturn(Optional.of(tecnico(false)));

        service.notificarOrdemPinturaAtribuida("ord-1", "tec-1", "gerente-1", "Vaso", "Azul", LocalDate.now());

        verify(repository).save(any());
        verifyNoInteractions(emailService);
    }

    @Test
    void falhaAoSalvarAvisoDaOrdemNaoDevePropagar() {
        when(repository.save(any())).thenThrow(new RuntimeException("mongo fora"));

        assertDoesNotThrow(() -> service.notificarOrdemPinturaAtribuida(
                "ord-1", "tec-1", "gerente-1", "Vaso", "Azul", LocalDate.now()));
    }

    // =========================================================
    // PEDIDO MUDOU DE ETAPA
    // =========================================================

    private Pedido pedidoNaEtapa(StatusPedido etapa) {
        Pedido p = pedido("cli-1");
        p.setStatus(etapa);
        return p;
    }

    @Test
    void mudancaDeEtapaDeveCriarAvisoComAEtapaEEnviarEmail() {
        when(userRepository.findById("cli-1")).thenReturn(Optional.of(cliente("ana@x.com")));

        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.PINTURA));

        ArgumentCaptor<Notificacao> captor = ArgumentCaptor.forClass(Notificacao.class);
        verify(repository).save(captor.capture());
        Notificacao salva = captor.getValue();
        assertEquals("cli-1", salva.getUsuarioId());
        assertEquals(TipoNotificacao.PEDIDO_ETAPA_ALTERADA, salva.getTipo());
        assertEquals("p-1", salva.getReferenciaId());
        assertEquals("Miniatura dragão", salva.getTitulo());
        assertEquals("PINTURA", salva.getDetalhe());
        assertFalse(salva.isLida());

        verify(emailService).enviarPedidoEtapaAlterada("ana@x.com", "Ana", "Miniatura dragão", "p-1", StatusPedido.PINTURA);
    }

    @Test
    void novaEtapaAntesDeLerDeveAtualizarOAvisoEmVezDeEmpilhar() {
        Notificacao anterior = new Notificacao();
        anterior.setId("n-1");
        anterior.setDetalhe("IMPRESSAO");
        when(repository.findByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
                "cli-1", TipoNotificacao.PEDIDO_ETAPA_ALTERADA, "p-1")).thenReturn(List.of(anterior));

        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.PINTURA));

        verify(repository).save(anterior);
        assertEquals("n-1", anterior.getId());
        assertEquals("PINTURA", anterior.getDetalhe());
    }

    @Test
    void canceladoNaoGeraAvisoDeEtapa() {
        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.CANCELADO));

        verifyNoInteractions(repository, emailService);
    }

    @Test
    void finalizarComAvisoDeEtapaAbertoDeveAtualizarParaFinalizado() {
        Notificacao emAcabamento = new Notificacao();
        emAcabamento.setId("n-1");
        emAcabamento.setDetalhe("ACABAMENTO");
        when(repository.findByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
                "cli-1", TipoNotificacao.PEDIDO_ETAPA_ALTERADA, "p-1")).thenReturn(List.of(emAcabamento));

        service.notificarEtapaAlterada(pedidoNaEtapa(StatusPedido.FINALIZADO));

        // o mesmo aviso passa a dizer "Finalizado" (não fica "Acabamento" ao lado)
        verify(repository).save(emAcabamento);
        assertEquals("FINALIZADO", emAcabamento.getDetalhe());
    }

    @Test
    void pedidoSemClienteNaoGeraAvisoDeEtapa() {
        Pedido semCliente = pedido(null);
        semCliente.setStatus(StatusPedido.PINTURA);

        service.notificarEtapaAlterada(semCliente);

        verifyNoInteractions(repository, emailService);
    }
}
