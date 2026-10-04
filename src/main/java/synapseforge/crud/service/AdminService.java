package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Admin.AdminPedidoResponseDTO;
import synapseforge.crud.DTO.Admin.AdminUserResponseDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.DTO.Admin.AdminUserUpdateRequestDTO;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.DTO.Admin.AdminPedidoUpdateRequestDTO;
import synapseforge.crud.mapper.PedidoAdminMapper;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PedidoRepository pedidoRepository;
    private final NotificacaoService notificacaoService;
    private final PedidoAdminMapper pedidoAdminMapper;

    // =========================================================
    // USUÁRIOS
    // =========================================================

    public List<AdminUserResponseDTO> listarUsuarios() {
        return userRepository.findAll()
                .stream()
                .map(this::toAdminUserResponseDTO)
                .toList();
    }

    public AdminUserResponseDTO buscarUsuario(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        return toAdminUserResponseDTO(user);
    }

    public AdminUserResponseDTO atualizarUsuario(
            String id,
            AdminUserUpdateRequestDTO dto
    ) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        if (dto.getNome() != null) {
            user.setNome(dto.getNome());
        }

        if (dto.getEmail() != null) {
            String email = UserRepository.normalizarEmail(dto.getEmail());
            userRepository.buscarPorEmail(email)
                    .filter(outro -> !outro.getId().equals(user.getId()))
                    .ifPresent(outro -> {
                        throw new RuntimeException("Este email já está em uso");
                    });
            user.setEmail(email);
        }

        if (dto.getCpf() != null) {
            user.setCpf(dto.getCpf());
        }

        if (dto.getTelefone() != null) {
            user.setTelefone(dto.getTelefone());
        }

        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        if (dto.getEquipeId() != null) {
            user.setEquipeId(dto.getEquipeId());
        }

        if (dto.getFuncaoVisual() != null) {
            user.setFuncaoVisual(dto.getFuncaoVisual());
        }

        if (dto.getAtivo() != null) {
            user.setAtivo(dto.getAtivo());
        }

        if (dto.getSenha() != null && !dto.getSenha().isBlank()) {
            throw new RuntimeException(
                    "Alteração de senha será implementada separadamente."
            );
        }

        user.setAtualizadoEm(
                java.time.LocalDateTime.now()
        );

        User atualizado = userRepository.save(user);

        return toAdminUserResponseDTO(atualizado);
    }

    public void deletarUsuario(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Usuário não encontrado")
                );

        userRepository.delete(user);
    }



    private AdminUserResponseDTO toAdminUserResponseDTO(User user) {
        return new AdminUserResponseDTO(
                user.getId(),
                user.getNome(),
                user.getEmail(),
                user.getCpf(),
                user.getTelefone(),
                user.getRole() != null
                        ? user.getRole().name()
                        : null,
                user.getEquipeId(),
                user.getFuncaoVisual(),
                user.isAtivo(),
                user.getCriadoEm(),
                user.getAtualizadoEm()
        );
    }

    // =========================================================
    // PEDIDOS
    // =========================================================

    public List<AdminPedidoResponseDTO> listarPedidos() {
        return pedidoRepository.findAll()
                .stream()
                .map(this::toAdminPedidoResponseDTO)
                .toList();
    }

    public AdminPedidoResponseDTO buscarPedido(String id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException("Pedido não encontrado")
                );

        return toAdminPedidoResponseDTO(pedido);
    }

    public AdminPedidoResponseDTO atualizarPedido(
            String id,
            AdminPedidoUpdateRequestDTO dto
    ) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido não encontrado"
                        )
                );

        StatusPedido statusAnterior = pedido.getStatus();

        // PATCH parcial: só os campos enviados (não nulos) sobrescrevem o pedido.
        // O código campo a campo é gerado pelo MapStruct (ver PedidoAdminMapper).
        pedidoAdminMapper.atualizar(dto, pedido);

        pedido.setAtualizadoEm(LocalDateTime.now());

        Pedido atualizado =
                pedidoRepository.save(pedido);

        // Etapa mudou (qualquer uma, inclusive FINALIZADO): avisa o cliente.
        // CANCELADO é ignorado dentro do método.
        if (atualizado.getStatus() != statusAnterior) {
            notificacaoService.notificarEtapaAlterada(atualizado);
        }

        return toAdminPedidoResponseDTO(atualizado);
    }


    public void deletarPedido(String id) {
        Pedido pedido = pedidoRepository.findById(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Pedido não encontrado"
                        )
                );

        pedidoRepository.delete(pedido);
    }




    private AdminPedidoResponseDTO toAdminPedidoResponseDTO(Pedido pedido) {
        int quantidadeImagens = 0;

        if (pedido.getImagensReferenciaFileIds() != null) {
            quantidadeImagens =
                    pedido.getImagensReferenciaFileIds().size();
        }

        return new AdminPedidoResponseDTO(
                pedido.getId(),

                pedido.getClienteId(),
                pedido.getCliente(),

                pedido.getProjeto(),
                pedido.getDescricao(),

                pedido.getOrcamentoId(),
                pedido.getMaterialId(),

                pedido.getVolumeCm3(),
                pedido.getTempoImpressaoHoras(),
                pedido.getTempoMaoDeObraHoras(),

                pedido.getCustoMaquinaHora(),
                pedido.getCustoMaoDeObraHora(),
                pedido.getMargemLucro(),

                pedido.getCustoMaterial(),
                pedido.getCustoMaquina(),
                pedido.getCustoMaoDeObra(),
                pedido.getCustoTotal(),
                pedido.getPrecoFinal(),

                pedido.getStatus() != null
                        ? pedido.getStatus().name()
                        : null,

                pedido.getPrazo(),

                pedido.getCriadoEm(),
                pedido.getAtualizadoEm(),

                pedido.getObjeto3DFileId(),
                quantidadeImagens
        );
    }
}
