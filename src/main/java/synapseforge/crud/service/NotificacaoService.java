package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Notificacao.NotificacaoResponseDTO;
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.infrastructure.entity.Notificacao;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.TipoNotificacao;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.NotificacaoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDate;
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
    // PEDIDO MUDOU DE ETAPA -> avisa o cliente vinculado (sino + email)
    // =========================================================
    //
    // Método único para qualquer mudança de etapa: avançar, voltar e também
    // finalizar (FINALIZADO é só mais uma etapa). Quem muda a etapa chama sempre
    // este método e não precisa saber que tipos de aviso existem.
    //
    // Um aviso por pedido no sino: se o pedido mudar de novo antes de o cliente
    // ler, o aviso existente é atualizado com a etapa nova (inclusive "Finalizado")
    // em vez de empilhar "Impressão", "Pintura", "Acabamento"...
    //
    // CANCELADO não passa por aqui. Falha nunca derruba a mudança de etapa:
    // só vai para o log. Sino e email são independentes.
    //

    public void notificarEtapaAlterada(Pedido pedido) {

        String clienteId = pedido.getClienteId();
        StatusPedido etapa = pedido.getStatus();

        if (clienteId == null || clienteId.isBlank()
                || etapa == null
                || etapa == StatusPedido.CANCELADO) {
            return;
        }

        try {

            List<Notificacao> naoLidos = repository.findByUsuarioIdAndTipoAndReferenciaIdAndLidaFalse(
                    clienteId,
                    TipoNotificacao.PEDIDO_ETAPA_ALTERADA,
                    pedido.getId()
            );

            Notificacao notificacao = naoLidos.isEmpty() ? new Notificacao() : naoLidos.get(0);
            notificacao.setUsuarioId(clienteId);
            notificacao.setTipo(TipoNotificacao.PEDIDO_ETAPA_ALTERADA);
            notificacao.setReferenciaId(pedido.getId());
            notificacao.setTitulo(pedido.getProjeto());
            notificacao.setDetalhe(etapa.name());
            notificacao.setLida(false);
            // data nova: o aviso sobe para o topo do sino como o mais recente
            notificacao.setCriadaEm(LocalDateTime.now());

            repository.save(notificacao);

        } catch (RuntimeException e) {

            log.warn(
                    "Não foi possível registrar a notificação de etapa do pedido {}: {}",
                    pedido.getId(),
                    e.getMessage()
            );
        }

        enviarEmailEtapaAlterada(pedido, etapa);
    }

    private void enviarEmailEtapaAlterada(Pedido pedido, StatusPedido etapa) {

        try {

            User cliente = userRepository.findById(pedido.getClienteId()).orElse(null);

            if (cliente == null
                    || cliente.getEmail() == null
                    || cliente.getEmail().isBlank()) {

                log.warn(
                        "Email de etapa não enviado: cliente {} não encontrado ou sem email.",
                        pedido.getClienteId()
                );
                return;
            }

            // mesma regra dos outros avisos: só para email verificado
            if (!cliente.isEmailConfirmado()) {

                log.info(
                        "Email de etapa não enviado: cliente {} sem email confirmado.",
                        pedido.getClienteId()
                );
                return;
            }

            emailService.enviarPedidoEtapaAlterada(
                    cliente.getEmail(),
                    cliente.getNome(),
                    pedido.getProjeto(),
                    pedido.getId(),
                    etapa
            );

        } catch (RuntimeException e) {

            log.warn(
                    "Falha ao enviar email de etapa do pedido {}: {}",
                    pedido.getId(),
                    e.getMessage()
            );
        }
    }


    // =========================================================
    // ORDEM DE PINTURA ATRIBUÍDA -> avisa o técnico (sino + email)
    // =========================================================
    //
    // Chamado ao criar a ordem e ao trocar o técnico dela. Quem atribui a ordem a si
    // mesmo não recebe aviso (já sabe). Como no pedido finalizado: falha aqui nunca
    // desfaz a ordem, só vai para o log, e sino e email são independentes.
    //

    public void notificarOrdemPinturaAtribuida(
            String ordemId,
            String tecnicoId,
            String atribuidaPor,
            String projeto,
            String corNome,
            LocalDate prazo
    ) {

        if (tecnicoId == null || tecnicoId.isBlank() || tecnicoId.equals(atribuidaPor)) {
            return;
        }

        try {

            Notificacao notificacao = new Notificacao();
            notificacao.setUsuarioId(tecnicoId);
            notificacao.setTipo(TipoNotificacao.ORDEM_PINTURA_ATRIBUIDA);
            notificacao.setReferenciaId(ordemId);
            notificacao.setTitulo(projeto);
            notificacao.setLida(false);
            notificacao.setCriadaEm(LocalDateTime.now());

            repository.save(notificacao);

        } catch (RuntimeException e) {

            log.warn(
                    "Não foi possível registrar a notificação da ordem de pintura {}: {}",
                    ordemId,
                    e.getMessage()
            );
        }

        enviarEmailOrdemPinturaAtribuida(ordemId, tecnicoId, projeto, corNome, prazo);
    }

    private void enviarEmailOrdemPinturaAtribuida(
            String ordemId,
            String tecnicoId,
            String projeto,
            String corNome,
            LocalDate prazo
    ) {

        try {

            User tecnico = userRepository.findById(tecnicoId).orElse(null);

            if (tecnico == null
                    || tecnico.getEmail() == null
                    || tecnico.getEmail().isBlank()) {

                log.warn(
                        "Email da ordem de pintura não enviado: técnico {} não encontrado ou sem email.",
                        tecnicoId
                );
                return;
            }

            // mesma regra do pedido finalizado: só manda para email verificado
            if (!tecnico.isEmailConfirmado()) {

                log.info(
                        "Email da ordem de pintura não enviado: técnico {} sem email confirmado.",
                        tecnicoId
                );
                return;
            }

            emailService.enviarOrdemPinturaAtribuida(
                    tecnico.getEmail(),
                    tecnico.getNome(),
                    projeto,
                    corNome,
                    prazo
            );

        } catch (RuntimeException e) {

            log.warn(
                    "Falha ao enviar email da ordem de pintura {}: {}",
                    ordemId,
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
                n.getDetalhe(),
                n.isLida(),
                n.getCriadaEm()
        );
    }
}
