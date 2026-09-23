package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import synapseforge.crud.DTO.Equipe.ConviteEquipeResponseDTO;
import synapseforge.crud.infrastructure.entity.ConviteEquipe;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusConviteEquipe;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.ConviteEquipeRepository;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class ConviteEquipeService {

    private final ConviteEquipeRepository conviteRepository;
    private final EquipeRepository equipeRepository;
    private final UserRepository userRepository;
    private final UserService userService;
    private final EmailService emailService;


    // =========================================================
    // CRIAR CONVITE
    // =========================================================

    public ConviteEquipe criarConvite(
            String equipeId,
            String gerenteId,
            String usuarioId
    ) {

        // -----------------------------------------------------
        // Verifica se a equipe existe
        // -----------------------------------------------------

        Equipe equipe = equipeRepository.findById(equipeId)
                .orElseThrow(() ->
                        new RuntimeException("Equipe não encontrada")
                );


        // -----------------------------------------------------
        // Verifica se o gerente é dono da equipe
        // -----------------------------------------------------

        if (!gerenteId.equals(equipe.getGerenteId())) {
            throw new RuntimeException(
                    "Você não possui permissão para convidar usuários para esta equipe"
            );
        }


        // -----------------------------------------------------
        // Busca o usuário convidado
        // -----------------------------------------------------

        User usuario = userRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );


        // -----------------------------------------------------
        // Somente CLIENTE pode ser convidado
        // -----------------------------------------------------

        if (usuario.getRole() != Role.CLIENTE) {
            throw new RuntimeException(
                    "Somente clientes podem ser convidados para uma equipe"
            );
        }


        // -----------------------------------------------------
        // Usuário não pode já estar em uma equipe
        // -----------------------------------------------------

        if (usuario.getEquipeId() != null) {
            throw new RuntimeException(
                    "Este usuário já pertence a uma equipe"
            );
        }


        // -----------------------------------------------------
        // Verifica se já existe convite pendente
        // -----------------------------------------------------

        if (conviteRepository.findByUsuarioIdAndStatus(
                usuarioId,
                StatusConviteEquipe.PENDENTE
        ).isPresent()) {

            throw new RuntimeException(
                    "Este usuário já possui um convite pendente"
            );
        }


        // -----------------------------------------------------
        // Cria o convite
        // -----------------------------------------------------

        ConviteEquipe convite = new ConviteEquipe();

        convite.setEquipeId(equipeId);
        convite.setGerenteId(gerenteId);
        convite.setUsuarioId(usuarioId);

        convite.setToken(
                UUID.randomUUID().toString()
        );

        convite.setStatus(
                StatusConviteEquipe.PENDENTE
        );

        LocalDateTime agora =
                LocalDateTime.now();

        convite.setCriadoEm(agora);

        convite.setExpiraEm(
                agora.plusHours(24)
        );

        convite.setRespondidoEm(null);


        // -----------------------------------------------------
        // Salva primeiro para garantir que o convite exista
        // -----------------------------------------------------

        ConviteEquipe salvo =
                conviteRepository.save(convite);


        // -----------------------------------------------------
        // Busca o gerente para obter nome e email
        // -----------------------------------------------------

        User gerente = userRepository.findById(gerenteId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Gerente da equipe não encontrado"
                        )
                );


        // -----------------------------------------------------
        // Envia email para o cliente
        // -----------------------------------------------------

        try {

            emailService.enviarConviteEquipe(
                    usuario.getEmail(),
                    usuario.getNome(),
                    gerente.getNome(),
                    equipe.getNome(),
                    salvo.getToken()
            );

        } catch (RuntimeException e) {

            // Se o email falhar, não deixamos
            // um convite pendente sem email enviado.

            conviteRepository.deleteById(
                    salvo.getId()
            );

            throw e;
        }


        return salvo;
    }


    // =========================================================
    // ACEITAR CONVITE
    // =========================================================

    public User aceitarConvite(String token) {

        ConviteEquipe convite =
                buscarConvitePendente(token);


        // -----------------------------------------------------
        // Busca equipe
        // -----------------------------------------------------

        Equipe equipe =
                equipeRepository.findById(
                        convite.getEquipeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "A equipe deste convite não existe mais"
                        )
                );


        // -----------------------------------------------------
        // Busca usuário
        // -----------------------------------------------------

        User usuario =
                userRepository.findById(
                        convite.getUsuarioId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "O usuário deste convite não existe mais"
                        )
                );


        // -----------------------------------------------------
        // Confere se ainda é CLIENTE
        // -----------------------------------------------------

        if (usuario.getRole() != Role.CLIENTE) {
            throw new RuntimeException(
                    "Este usuário não pode aceitar o convite"
            );
        }


        // -----------------------------------------------------
        // Confere se ainda não pertence a equipe
        // -----------------------------------------------------

        if (usuario.getEquipeId() != null) {
            throw new RuntimeException(
                    "Este usuário já pertence a uma equipe"
            );
        }


        // -----------------------------------------------------
        // Coloca usuário na equipe
        // CLIENTE → TECNICO
        // -----------------------------------------------------

        User atualizado =
                userService.entrarNaEquipe(
                        usuario.getId(),
                        equipe.getId()
                );


        // -----------------------------------------------------
        // Marca convite como aceito
        // -----------------------------------------------------

        convite.setStatus(
                StatusConviteEquipe.ACEITO
        );

        convite.setRespondidoEm(
                LocalDateTime.now()
        );

        conviteRepository.save(convite);


        // -----------------------------------------------------
        // Notifica o gerente
        // -----------------------------------------------------

        notificarGerenteConviteAceito(
                convite,
                equipe,
                atualizado
        );


        return atualizado;
    }


    // =========================================================
    // RECUSAR CONVITE
    // =========================================================

    public User recusarConvite(String token) {

        ConviteEquipe convite =
                buscarConvitePendente(token);


        // -----------------------------------------------------
        // Busca equipe
        // -----------------------------------------------------

        Equipe equipe =
                equipeRepository.findById(
                        convite.getEquipeId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "A equipe deste convite não existe mais"
                        )
                );


        // -----------------------------------------------------
        // Busca usuário
        // -----------------------------------------------------

        User usuario =
                userRepository.findById(
                        convite.getUsuarioId()
                ).orElseThrow(() ->
                        new RuntimeException(
                                "O usuário deste convite não existe mais"
                        )
                );


        // -----------------------------------------------------
        // Marca convite como recusado
        // -----------------------------------------------------

        convite.setStatus(
                StatusConviteEquipe.RECUSADO
        );

        convite.setRespondidoEm(
                LocalDateTime.now()
        );

        conviteRepository.save(convite);


        // -----------------------------------------------------
        // Notifica gerente
        // -----------------------------------------------------

        notificarGerenteConviteRecusado(
                convite,
                equipe,
                usuario
        );


        return usuario;
    }


    // =========================================================
    // BUSCAR CONVITE PENDENTE
    // =========================================================

    private ConviteEquipe buscarConvitePendente(
            String token
    ) {

        ConviteEquipe convite =
                conviteRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Convite não encontrado"
                                )
                        );


        // -----------------------------------------------------
        // Já foi respondido?
        // -----------------------------------------------------

        if (convite.getStatus()
                != StatusConviteEquipe.PENDENTE) {

            throw new RuntimeException(
                    "Este convite já foi respondido"
            );
        }


        // -----------------------------------------------------
        // Está expirado?
        // -----------------------------------------------------

        if (convite.getExpiraEm() != null
                && convite.getExpiraEm()
                .isBefore(LocalDateTime.now())) {

            convite.setStatus(
                    StatusConviteEquipe.EXPIRADO
            );

            convite.setRespondidoEm(
                    LocalDateTime.now()
            );

            conviteRepository.save(convite);

            throw new RuntimeException(
                    "Este convite expirou"
            );
        }


        return convite;
    }


    // =========================================================
    // CONVITE PENDENTE DO USUÁRIO LOGADO (SYN-101)
    // =========================================================

    /*
     * O convidado também vê o convite dentro do app, sem depender do e-mail.
     * Cada usuário tem no máximo um convite pendente (criarConvite garante).
     * Convite vencido é marcado como EXPIRADO e não aparece.
     */
    public Optional<ConviteEquipe> buscarConvitePendenteDoUsuario(
            String usuarioId
    ) {

        return conviteRepository.findByUsuarioIdAndStatus(
                        usuarioId,
                        StatusConviteEquipe.PENDENTE
                )
                .filter(convite -> {

                    if (convite.getExpiraEm() != null
                            && convite.getExpiraEm()
                            .isBefore(LocalDateTime.now())) {

                        convite.setStatus(
                                StatusConviteEquipe.EXPIRADO
                        );

                        convite.setRespondidoEm(
                                LocalDateTime.now()
                        );

                        conviteRepository.save(convite);

                        return false;
                    }

                    return true;
                });
    }

    public User aceitarConviteDoUsuario(String usuarioId) {

        ConviteEquipe convite =
                buscarConvitePendenteDoUsuario(usuarioId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Convite não encontrado"
                                )
                        );

        return aceitarConvite(convite.getToken());
    }

    public User recusarConviteDoUsuario(String usuarioId) {

        ConviteEquipe convite =
                buscarConvitePendenteDoUsuario(usuarioId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Convite não encontrado"
                                )
                        );

        return recusarConvite(convite.getToken());
    }


    // =========================================================
    // BUSCAR CONVITE PELO TOKEN
    // =========================================================

    public ConviteEquipe buscarPorToken(
            String token
    ) {

        return buscarConvitePendente(token);
    }


    // =========================================================
    // LISTAR CONVITES DA EQUIPE
    // =========================================================

    public List<ConviteEquipe> listarConvitesDaEquipe(
            String equipeId
    ) {

        return conviteRepository.findByEquipeIdAndStatus(
                equipeId,
                StatusConviteEquipe.PENDENTE
        );
    }


    // =========================================================
    // CONVERTER PARA RESPONSE DTO
    // =========================================================

    public ConviteEquipeResponseDTO toResponseDTO(
            ConviteEquipe convite
    ) {

        Equipe equipe =
                equipeRepository.findById(
                        convite.getEquipeId()
                ).orElse(null);

        User gerente =
                userRepository.findById(
                        convite.getGerenteId()
                ).orElse(null);

        User usuario =
                userRepository.findById(
                        convite.getUsuarioId()
                ).orElse(null);


        return new ConviteEquipeResponseDTO(
                convite.getId(),
                convite.getEquipeId(),
                equipe != null
                        ? equipe.getNome()
                        : null,
                convite.getGerenteId(),
                gerente != null
                        ? gerente.getNome()
                        : null,
                convite.getUsuarioId(),
                usuario != null
                        ? usuario.getNome()
                        : null,
                convite.getStatus(),
                convite.getCriadoEm(),
                convite.getExpiraEm()
        );
    }


    // =========================================================
    // NOTIFICAR GERENTE — CONVITE ACEITO
    // =========================================================

    private void notificarGerenteConviteAceito(
            ConviteEquipe convite,
            Equipe equipe,
            User usuario
    ) {

        User gerente =
                userRepository.findById(
                        convite.getGerenteId()
                ).orElse(null);


        if (gerente == null
                || gerente.getEmail() == null
                || gerente.getEmail().isBlank()) {

            log.warn(
                    "Não foi possível enviar notificação de convite aceito: gerente não encontrado ou sem email."
            );

            return;
        }


        try {

            emailService.enviarConviteAceito(
                    gerente.getEmail(),
                    gerente.getNome(),
                    usuario.getNome(),
                    equipe.getNome()
            );

        } catch (RuntimeException e) {

            // A aceitação já aconteceu.
            // Não vamos desfazer a entrada na equipe
            // somente porque o email falhou.

            log.error(
                    "Falha ao enviar email de convite aceito para o gerente {}",
                    gerente.getEmail(),
                    e
            );
        }
    }


    // =========================================================
    // NOTIFICAR GERENTE — CONVITE RECUSADO
    // =========================================================

    private void notificarGerenteConviteRecusado(
            ConviteEquipe convite,
            Equipe equipe,
            User usuario
    ) {

        User gerente =
                userRepository.findById(
                        convite.getGerenteId()
                ).orElse(null);


        if (gerente == null
                || gerente.getEmail() == null
                || gerente.getEmail().isBlank()) {

            log.warn(
                    "Não foi possível enviar notificação de convite recusado: gerente não encontrado ou sem email."
            );

            return;
        }


        try {

            emailService.enviarConviteRecusado(
                    gerente.getEmail(),
                    gerente.getNome(),
                    usuario.getNome(),
                    equipe.getNome()
            );

        } catch (RuntimeException e) {

            // A recusa já aconteceu.
            // Não vamos transformar uma falha de email
            // em falha da operação.

            log.error(
                    "Falha ao enviar email de convite recusado para o gerente {}",
                    gerente.getEmail(),
                    e
            );
        }
    }
}