package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.EquipeRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.Optional;

/**
 * Resolve a equipe (oficina) do usuário logado. É a única fonte do equipeId usado para
 * isolar os dados de negócio: o valor sai sempre do cadastro do usuário, nunca do request.
 * <p>
 * Ordem de resolução: User.equipeId; se vazio e o usuário for GERENTE/ADMIN, a equipe que
 * ele administra (Equipe.gerenteId), pois quem cria a equipe hoje não recebe equipeId.
 * <p>
 * CLIENTE nunca pertence a uma equipe (mesmo com equipeId gravado): o vínculo do cliente
 * com as oficinas é só pelos pedidos (Pedido.clienteId).
 */
@Component
@RequiredArgsConstructor
public class EquipeContexto {

    private final UserRepository userRepository;
    private final EquipeRepository equipeRepository;

    public Optional<String> equipeDe(String usuarioId) {
        if (usuarioId == null) {
            return Optional.empty();
        }
        return userRepository.findById(usuarioId).flatMap(this::equipeDe);
    }

    public Optional<String> equipeDe(User usuario) {
        if (usuario.getRole() == Role.CLIENTE) {
            return Optional.empty();
        }
        if (usuario.getEquipeId() != null && !usuario.getEquipeId().isBlank()) {
            return Optional.of(usuario.getEquipeId());
        }
        if (usuario.getRole() == Role.GERENTE || usuario.getRole() == Role.ADMIN) {
            return equipeRepository.findByGerenteId(usuario.getId()).map(Equipe::getId);
        }
        return Optional.empty();
    }

    /**
     * Para operações de escrita: sem equipe não há onde gravar, então a requisição é recusada
     * com 403 (SEM_EQUIPE).
     */
    public String equipeObrigatoria(String usuarioId) {
        return equipeDe(usuarioId).orElseThrow(SemEquipeException::new);
    }
}
