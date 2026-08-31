package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.service.UserService;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private UserService service;

    @InjectMocks
    private UserController controller;

    @Test
    void criarDeveRetornarDtoConvertido() {
        UserRequestDTO dto = new UserRequestDTO();
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");

        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN");

        when(service.toEntity(dto)).thenReturn(user);
        when(service.criar(user)).thenReturn(user);
        when(service.toResponseDTO(user)).thenReturn(response);

        UserResponseDTO result = controller.criar(dto);

        assertEquals("Ana", result.getNome());
        verify(service).criar(user);
    }

    @Test
    void listarDeveRetornarListaDeDtos() {
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN");

        when(service.listar()).thenReturn(List.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        assertEquals(1, controller.listar().size());
        assertEquals("Ana", controller.listar().get(0).getNome());
    }

    @Test
    void buscarDeveRetornarUsuario() {
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN");

        when(service.buscarPorId("u-1")).thenReturn(Optional.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        UserResponseDTO result = controller.buscar("u-1");

        assertEquals("u-1", result.getId());
    }

    @Test
    void atualizarDeveRetornarUsuarioAtualizado() {
        UserRequestDTO dto = new UserRequestDTO();
        User updated = new User();
        updated.setId("u-1");
        updated.setNome("Ana Atualizada");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana Atualizada", "ana@email.com", "123", "111", "ADMIN");

        when(service.atualizar("u-1", dto)).thenReturn(updated);
        when(service.toResponseDTO(updated)).thenReturn(response);

        UserResponseDTO result = controller.atualizar("u-1", dto);

        assertEquals("Ana Atualizada", result.getNome());
    }

    @Test
    void deletarDeveChamarService() {
        controller.deletar("u-1");
        verify(service).deletar("u-1");
    }

    @Test
    void criarVariosDeveRetornarLista() {
        UserRequestDTO dto = new UserRequestDTO();
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN");

        when(service.toEntity(dto)).thenReturn(user);
        when(service.criarVarios(List.of(user))).thenReturn(List.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        assertEquals(1, controller.criarVarios(List.of(dto)).size());
    }

    @Test
    void buscarPorNomeDeveRetornarLista() {
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN");

        when(service.buscarPorNome("Ana")).thenReturn(List.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        assertEquals(1, controller.buscarPorNome("Ana").size());
        assertEquals("Ana", controller.buscarPorNome("Ana").get(0).getNome());
    }

    @Test
    void solicitarMudancaEmailDeveRetornarMensagem() {
        Map<String, String> body = Map.of("novoEmail", "novo@email.com");
        when(service.solicitarMudancaEmail("u-1", "novo@email.com")).thenReturn(Map.of("mensagem", "ok"));

        assertEquals("ok", controller.solicitarMudancaEmail("u-1", body).get("mensagem"));
    }

    @Test
    void confirmarMudancaEmailDeveRetornarMensagem() {
        Map<String, String> response = Map.of("mensagem", "Email alterado com sucesso!");

        doNothing().when(service).confirmarMudancaEmail("token-1");

        assertEquals("Email alterado com sucesso!", controller.confirmarMudancaEmail("token-1").get("mensagem"));
        verify(service).confirmarMudancaEmail("token-1");
    }
}
