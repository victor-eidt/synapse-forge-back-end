package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

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
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ConviteEquipeService {

    private final ConviteEquipeRepository conviteRepository;
    private final EquipeRepository equipeRepository;
    private final UserRepository userRepository;


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

        Equipe equipe =
                equipeRepository.findById(equipeId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Equipe não encontrada"
                                )
                        );


        // -----------------------------------------------------
        // Verifica se o gerente é dono da equipe
        // -----------------------------------------------------

        if (!gerenteId.equals(
                equipe.getGerenteId()
        )) {

            throw new RuntimeException(
                    "Você não possui permissão para convidar usuários para esta equipe"
            );
        }


        // -----------------------------------------------------
        // Busca usuário
        // -----------------------------------------------------

        User usuario =
                userRepository.findById(usuarioId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Usuário não encontrado"
                                )
                        );


        // -----------------------------------------------------
        // Usuário precisa ser CLIENTE
        // -----------------------------------------------------

        if (usuario.getRole() != Role.CLIENTE) {

            throw new RuntimeException(
                    "Somente clientes podem ser convidados para uma equipe"
            );
        }


        // -----------------------------------------------------
        // Usuário não pode estar em outra equipe
        // -----------------------------------------------------

        if (usuario.getEquipeId() != null) {

            throw new RuntimeException(
                    "Este usuário já pertence a uma equipe"
            );
        }


        // -----------------------------------------------------
        // Verifica convite pendente
        // -----------------------------------------------------

        if (
                conviteRepository
                        .existsByEquipeIdAndUsuarioIdAndStatus(
                                equipeId,
                                usuarioId,
                                StatusConviteEquipe.PENDENTE
                        )
        ) {

            throw new RuntimeException(
                    "Já existe um convite pendente para este usuário"
            );
        }


        // -----------------------------------------------------
        // Cria convite
        // -----------------------------------------------------

        ConviteEquipe convite =
                new ConviteEquipe();

        convite.setEquipeId(
                equipeId
        );

        convite.setGerenteId(
                gerenteId
        );

        convite.setUsuarioId(
                usuarioId
        );

        convite.setToken(
                UUID.randomUUID().toString()
        );

        convite.setStatus(
                StatusConviteEquipe.PENDENTE
        );

        convite.setCriadoEm(
                LocalDateTime.now()
        );

        convite.setExpiraEm(
                LocalDateTime.now().plusHours(24)
        );

        convite.setRespondidoEm(
                null
        );


        return conviteRepository.save(
                convite
        );
    }


    // =========================================================
    // BUSCAR CONVITE PELO TOKEN
    // =========================================================

    public ConviteEquipe buscarPorToken(
            String token
    ) {

        ConviteEquipe convite =
                conviteRepository.findByToken(token)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Convite não encontrado"
                                )
                        );


        verificarExpiracao(
                convite
        );


        return convite;
    }


    // =========================================================
    // VERIFICAR EXPIRAÇÃO
    // =========================================================

    private void verificarExpiracao(
            ConviteEquipe convite
    ) {

        if (
                convite.getStatus()
                        != StatusConviteEquipe.PENDENTE
        ) {

            return;
        }


        if (
                convite.getExpiraEm() != null
                        && convite.getExpiraEm()
                        .isBefore(LocalDateTime.now())
        ) {

            convite.setStatus(
                    StatusConviteEquipe.EXPIRADO
            );

            conviteRepository.save(
                    convite
            );

            throw new RuntimeException(
                    "Este convite expirou"
            );
        }
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
}
