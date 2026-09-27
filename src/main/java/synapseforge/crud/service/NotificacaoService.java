package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Notificacao.NotificacaoResponseDTO;
import synapseforge.crud.infrastructure.entity.Notificacao;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;
import synapseforge.crud.infrastructure.repository.NotificacaoRepository;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificacaoService {

    private final NotificacaoRepository repository;


    // =========================================================
    // PEDIDO FINALIZADO -> avisa o cliente vinculado
    // =========================================================
    //
    // Nunca derruba a mudança de etapa: se falhar, só registra no log.
    // Se o pedido regredir e for finalizado de novo enquanto o aviso
    // anterior não foi lido, não duplica.
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
    }


    // =========================================================
    // LISTAR (do usuário logado)
    // =========================================================

    public List<Notificacao> listar(String usuarioId, boolean apenasNaoLidas) {

        if (apenasNaoLidas) {
            return repository.findByUsuarioIdAndLidaFalseOrderByCriadaEmDesc(usuarioId);
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
                        new RuntimeException("Notificação não encontrada")
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
