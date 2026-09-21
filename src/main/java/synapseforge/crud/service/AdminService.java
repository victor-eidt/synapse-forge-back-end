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
import synapseforge.crud.DTO.Admin.AdminPedidoUpdateRequestDTO;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final PedidoRepository pedidoRepository;

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
            user.setEmail(dto.getEmail());
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

        if (dto.getClienteId() != null) {
            pedido.setClienteId(dto.getClienteId());
        }

        if (dto.getCliente() != null) {
            pedido.setCliente(dto.getCliente());
        }

        if (dto.getProjeto() != null) {
            pedido.setProjeto(dto.getProjeto());
        }

        if (dto.getDescricao() != null) {
            pedido.setDescricao(dto.getDescricao());
        }

        if (dto.getMaterialId() != null) {
            pedido.setMaterialId(dto.getMaterialId());
        }

        if (dto.getVolumeCm3() != null) {
            pedido.setVolumeCm3(dto.getVolumeCm3());
        }

        if (dto.getTempoImpressaoHoras() != null) {
            pedido.setTempoImpressaoHoras(
                    dto.getTempoImpressaoHoras()
            );
        }

        if (dto.getTempoMaoDeObraHoras() != null) {
            pedido.setTempoMaoDeObraHoras(
                    dto.getTempoMaoDeObraHoras()
            );
        }

        if (dto.getCustoMaquinaHora() != null) {
            pedido.setCustoMaquinaHora(
                    dto.getCustoMaquinaHora()
            );
        }

        if (dto.getCustoMaoDeObraHora() != null) {
            pedido.setCustoMaoDeObraHora(
                    dto.getCustoMaoDeObraHora()
            );
        }

        if (dto.getMargemLucro() != null) {
            pedido.setMargemLucro(
                    dto.getMargemLucro()
            );
        }

        if (dto.getCustoMaterial() != null) {
            pedido.setCustoMaterial(
                    dto.getCustoMaterial()
            );
        }

        if (dto.getCustoMaquina() != null) {
            pedido.setCustoMaquina(
                    dto.getCustoMaquina()
            );
        }

        if (dto.getCustoMaoDeObra() != null) {
            pedido.setCustoMaoDeObra(
                    dto.getCustoMaoDeObra()
            );
        }

        if (dto.getCustoTotal() != null) {
            pedido.setCustoTotal(
                    dto.getCustoTotal()
            );
        }

        if (dto.getPrecoFinal() != null) {
            pedido.setPrecoFinal(
                    dto.getPrecoFinal()
            );
        }

        if (dto.getStatus() != null) {
            pedido.setStatus(dto.getStatus());
        }

        if (dto.getPrazo() != null) {
            pedido.setPrazo(dto.getPrazo());
        }

        pedido.setAtualizadoEm(
                java.time.LocalDateTime.now()
        );

        Pedido atualizado =
                pedidoRepository.save(pedido);

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
