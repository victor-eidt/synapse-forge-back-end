package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Notificacao.NotificacaoResponseDTO;
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.infrastructure.entity.Notificacao;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.NotificacaoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository repository;
    private final UserRepository userRepository;
    private final EmailService emailService;


    // =========================================================
    // PEDIDO FINALIZADO -> avisa o cliente vinculado (sino + email)
    // =========================================================
    //
    // Nunca derruba a mudança de etapa: se falhar, só registra no log.
    // Se o pedido regredir e for finalizado de novo enquanto o aviso
    // anterior não foi lido, não duplica (nem o sino, nem o email).
    // Sino e email são independentes: falha no SMTP não apaga o aviso.
    //

    public void notificarPedidoFinalizado(Pedido pedido) {

        String clienteId = pedido.getClienteId();

        if (clienteId == null || clienteId.isBlank()) {
            return;
        }

        try {

            if (repository.existsByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
                    clienteId,
                    TipoNotificacao.PEDIDO_FINALIZADO,
                    pedido.getId()
            )) {
                return;
            }

            Notificacao notificacao = new Notificacao();
            notificacao.setUsuarioId(clienteId);
            notificacao.setTipo(TipoNotificacao.PEDIDO_FINALIZADO);
            notificacao.setReferenciaId(pedido.getId());
            notificacao.setTitulo(pedido.getProjeto());
            notificacao.setLida(false);
            notificacao.setCriadaEm(LocalDateTime.now());

            repository.save(notificacao);

        } catch (RuntimeException e) {

            log.warn(
                    "Não foi possível registrar a notificação de pedido finalizado {}: {}",
                    pedido.getId(),
                    e.getMessage()
            );
        }

        enviarEmailPedidoFinalizado(pedido);
    }

    private void enviarEmailPedidoFinalizado(Pedido pedido) {

        try {

            User cliente = userRepository.findById(pedido.getClienteId())
                    .orElse(null);

            if (cliente == null
                    || cliente.getEmail() == null
                    || cliente.getEmail().isBlank()) {

                log.warn(
                        "Email de pedido finalizado não enviado: cliente {} não encontrado ou sem email.",
                        pedido.getClienteId()
                );
                return;
            }

            // Email não confirmado pode ter sido digitado errado (ou ser de outra
            // pessoa): não manda dados do pedido para um endereço não verificado
            if (!cliente.isEmailConfirmado()) {

                log.info(
                        "Email de pedido finalizado não enviado: cliente {} sem email confirmado.",
                        pedido.getClienteId()
                );
                return;
            }
            emailService.enviarPedidoFinalizado(
                    cliente.getEmail(),
                    cliente.getNome(),
                    pedido.getProjeto(),
                    pedido.getId()
            );

        } catch (RuntimeException e) {

            log.warn(
                    "Falha ao enviar email de pedido finalizado {}: {}",
                    pedido.getId(),
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // LISTAR (do usuário logado)
    // =========================================================

    public List<Notificacao> listar(String usuarioId, boolean apenasNaoLidas) {

        // Limitado como a listagem completa: o sino consulta isto a cada minuto
        if (apenasNaoLidas) {
            return repository.findTop50ByUsuarioIdAndLidaFalseOrderByCriadaEmDesc(usuarioId);
        }

        return repository.findTop50ByUsuarioIdOrderByCriadaEmDesc(usuarioId);
    }


    // =========================================================
    // MARCAR COMO LIDA
    // =========================================================
    //
    // Busca sempre pelo par (id, usuário): ninguém marca aviso de outra pessoa.
    //

    public Notificacao marcarComoLida(String id, String usuarioId) {

        Notificacao notificacao = repository.findByIdAndUsuarioId(id, usuarioId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Notificação não encontrada")
                );

        if (!notificacao.isLida()) {
            notificacao.setLida(true);
            notificacao.setLidaEm(LocalDateTime.now());
            notificacao = repository.save(notificacao);
        }

        return notificacao;
    }

    public void marcarTodasComoLidas(String usuarioId) {

        List<Notificacao> naoLidas =
                repository.findByUsuarioIdAndLidaFalseOrderByCriadaEmDesc(usuarioId);

        if (naoLidas.isEmpty()) {
            return;
        }

        LocalDateTime agora = LocalDateTime.now();

        naoLidas.forEach(n -> {
            n.setLida(true);
            n.setLidaEm(agora);
        });

        repository.saveAll(naoLidas);
    }


    // =========================================================
    // ENTITY -> DTO
    // =========================================================

    public NotificacaoResponseDTO toResponseDTO(Notificacao n) {

        return new NotificacaoResponseDTO(
                n.getId(),
                n.getTipo(),
                n.getReferenciaId(),
                n.getTitulo(),
                n.isLida(),
                n.getCriadaEm()
        );
    }
}
