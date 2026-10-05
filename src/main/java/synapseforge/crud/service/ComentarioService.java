package synapseforge.crud.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import synapseforge.crud.DTO.Comentario.ComentarioRequestDTO;
import synapseforge.crud.DTO.Comentario.ComentarioResponseDTO;
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.infrastructure.entity.Comentario;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.repository.ComentarioRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.entity.User;


import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ComentarioService {

    private final ComentarioRepository comentarioRepository;
    private final PedidoService pedidoService;
    private final UserRepository userRepository;

    public ComentarioResponseDTO criar(
            String pedidoId,
            ComentarioRequestDTO dto,
            String usuarioId,
            Role role
    ) {
        validarPermissaoComentario(pedidoId, usuarioId, role);

        Comentario comentario = new Comentario();

        comentario.setPedidoId(pedidoId);
        comentario.setUsuarioId(usuarioId);
        comentario.setConteudo(dto.getConteudo());
        comentario.setCriadoEm(LocalDateTime.now());

        return toResponseDTO(
                comentarioRepository.save(comentario)
        );
    }

    public List<ComentarioResponseDTO> listar(
            String pedidoId,
            String usuarioId,
            Role role
    ) {
        validarPermissaoComentario(pedidoId, usuarioId, role);

        return comentarioRepository
                .findByPedidoIdOrderByCriadoEmAsc(pedidoId)
                .stream()
                .map(this::toResponseDTO)
                .toList();
    }

    private void validarPermissaoComentario(
            String pedidoId,
            String usuarioId,
            Role role
    ) {
        if (role != Role.GERENTE && role != Role.TECNICO) {
            throw new RuntimeException(
                    "Apenas GERENTE e TECNICO podem acessar os comentários."
            );
        }

        // Reutiliza a regra já existente de acesso ao Pedido.
        // buscarPorId devolve vazio quando o pedido não existe ou é de outra equipe.
        pedidoService.buscarPorId(
                pedidoId,
                usuarioId,
                role
        ).orElseThrow(() ->
                new RecursoNaoEncontradoException("Pedido não encontrado")
        );
    }

    public void deletar (
            String pedidoId,
            String comentarioId,
            String usuarioId,
            Role role
    ) {
        //Validação
        validarPermissaoComentario(pedidoId, usuarioId, role);

        //Conecta ao repository
        Comentario comentario = comentarioRepository
                .findById(comentarioId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Comentário não encontrado")
                );
        //Verifica Pedido
        if (!pedidoId.equals(comentario.getPedidoId())) {
            throw new RuntimeException("O comentário não pertence a este pedido.");
        }
        //Verifica se o comentário é do usuário ativo
        if (!usuarioId.equals(comentario.getUsuarioId())) {
            throw new RuntimeException("Você não pode excluir este comentário.");
        }

        //Realiza o delete após verificações
        comentarioRepository.delete(comentario);


    }

    public ComentarioResponseDTO editar (
            String pedidoId,
            String comentarioId,
            ComentarioRequestDTO dto,
            String usuarioId,
            Role role
    ) {
        //Validação
        validarPermissaoComentario(pedidoId, usuarioId, role);

        //Conecta
        Comentario comentario = comentarioRepository
                .findById(comentarioId)
                .orElseThrow(() ->
                        new RecursoNaoEncontradoException("Comentário não encontrado.")
                );
        // Verifica Pedido
        if (!pedidoId.equals(comentario.getPedidoId())) {
            throw new RuntimeException(
                    "O comentário não pertence a este pedido."
            );
        }
        // Verifica se o comentário é do usuário ativo
        if (!usuarioId.equals(comentario.getUsuarioId())) {
            throw new RuntimeException(
                    "Você não pode editar este comentário."
            );
        }

        comentario.setConteudo(dto.getConteudo());

        return toResponseDTO(comentarioRepository.save(comentario));
    }

    private ComentarioResponseDTO toResponseDTO(
            Comentario comentario
    ) {

        String nomeUsuario = userRepository
                .findById(comentario.getUsuarioId())
                .map(User::getNome)
                .orElse("Usuário");

        return new ComentarioResponseDTO(
                comentario.getId(),
                comentario.getPedidoId(),
                comentario.getUsuarioId(),
                nomeUsuario,
                comentario.getConteudo(),
                comentario.getCriadoEm()
        );
    }
}