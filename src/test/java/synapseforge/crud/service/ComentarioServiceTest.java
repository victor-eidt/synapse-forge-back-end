package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Comentario.ComentarioRequestDTO;
import synapseforge.crud.exception.RecursoNaoEncontradoException;
import synapseforge.crud.infrastructure.entity.Comentario;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.repository.ComentarioRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class ComentarioServiceTest {

    @Mock
    private ComentarioRepository comentarioRepository;

    @Mock
    private PedidoService pedidoService;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ComentarioService service;

    @Test
    void listarPedidoDeOutraEquipeLancaNaoEncontrado() {
        when(pedidoService.buscarPorId("p1", "u1", Role.TECNICO)).thenReturn(Optional.empty());

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.listar("p1", "u1", Role.TECNICO));
        verify(comentarioRepository, never()).findByPedidoIdOrderByCriadoEmAsc(any());
    }

    @Test
    void criarEmPedidoDeOutraEquipeNaoSalva() {
        when(pedidoService.buscarPorId("p1", "u1", Role.GERENTE)).thenReturn(Optional.empty());
        ComentarioRequestDTO dto = new ComentarioRequestDTO();
        dto.setConteudo("oi");

        assertThrows(RecursoNaoEncontradoException.class,
                () -> service.criar("p1", dto, "u1", Role.GERENTE));
        verify(comentarioRepository, never()).save(any());
    }

    @Test
    void listarPedidoDaEquipeDevolveComentarios() {
        when(pedidoService.buscarPorId("p1", "u1", Role.TECNICO)).thenReturn(Optional.of(new Pedido()));
        Comentario c = new Comentario("c1", "p1", "u2", "texto", null);
        when(comentarioRepository.findByPedidoIdOrderByCriadoEmAsc("p1")).thenReturn(List.of(c));
        when(userRepository.findById("u2")).thenReturn(Optional.empty());

        var lista = service.listar("p1", "u1", Role.TECNICO);

        assertEquals(1, lista.size());
        assertEquals("Usuário", lista.get(0).getNomeUsuario());
    }
}
