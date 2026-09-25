package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import synapseforge.crud.DTO.User.ClienteResumoDTO;
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

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("gerente-1");
        return auth;
    }

    @Test
    void criarDeveRetornarDtoConvertido() {
        UserRequestDTO dto = new UserRequestDTO();
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");

        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN", null, null);

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
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN", null, null);

        when(service.listar("gerente-1")).thenReturn(List.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        Authentication auth = auth();
        assertEquals(1, controller.listar(auth).size());
        assertEquals("Ana", controller.listar(auth).get(0).getNome());
    }

    @Test
    void buscarDeveRetornarUsuario() {
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN", null, null);

        when(service.buscarParaUsuario("gerente-1", "u-1")).thenReturn(Optional.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        UserResponseDTO result = controller.buscar("u-1", auth());

        assertEquals("u-1", result.getId());
    }

    @Test
    void atualizarDeveRetornarUsuarioAtualizado() {
        UserRequestDTO dto = new UserRequestDTO();
        User updated = new User();
        updated.setId("u-1");
        updated.setNome("Ana Atualizada");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana Atualizada", "ana@email.com", "123", "111", "ADMIN", null, null);

        when(service.atualizar("gerente-1", "u-1", dto)).thenReturn(updated);
        when(service.toResponseDTO(updated)).thenReturn(response);

        UserResponseDTO result = controller.atualizar("u-1", dto, auth());

        assertEquals("Ana Atualizada", result.getNome());
    }

    @Test
    void deletarDeveChamarService() {
        controller.deletar("u-1", auth());
        verify(service).deletar("gerente-1", "u-1");
    }

    @Test
    void criarVariosDeveRetornarLista() {
        UserRequestDTO dto = new UserRequestDTO();
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN", null, null);

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
        UserResponseDTO response = new UserResponseDTO("u-1", "Ana", "ana@email.com", "123", "111", "ADMIN", null, null);

        when(service.buscarPorNome("gerente-1", "Ana")).thenReturn(List.of(user));
        when(service.toResponseDTO(user)).thenReturn(response);

        Authentication auth = auth();
        assertEquals(1, controller.buscarPorNome("Ana", auth).size());
        assertEquals("Ana", controller.buscarPorNome("Ana", auth).get(0).getNome());
    }

    @Test
    void buscarDeUsuarioInacessivelRespondeNaoEncontrado() {
        when(service.buscarParaUsuario("gerente-1", "u-9")).thenReturn(Optional.empty());

        RuntimeException ex = org.junit.jupiter.api.Assertions.assertThrows(RuntimeException.class,
                () -> controller.buscar("u-9", auth()));

        assertEquals("Usuário não encontrado", ex.getMessage());
    }

    @Test
    void listarClientesDevolveResumo() {
        User cliente = new User();
        cliente.setId("c-1");
        when(service.listarClientes("gerente-1")).thenReturn(List.of(cliente));
        when(service.toClienteResumoDTO(cliente)).thenReturn(new ClienteResumoDTO("c-1", "Cli", "cli@x.com"));

        List<ClienteResumoDTO> result = controller.listarClientes(auth());

        assertEquals(1, result.size());
        assertEquals("cli@x.com", result.get(0).getEmail());
    }

    @Test
    void buscarClientePorEmailDevolve200Ou404() {
        User cliente = new User();
        cliente.setId("c-1");
        when(service.buscarClientePorEmail("gerente-1", "cli@x.com")).thenReturn(Optional.of(cliente));
        when(service.buscarClientePorEmail("gerente-1", "nada@x.com")).thenReturn(Optional.empty());
        when(service.toClienteResumoDTO(cliente)).thenReturn(new ClienteResumoDTO("c-1", "Cli", "cli@x.com"));

        ResponseEntity<ClienteResumoDTO> achado = controller.buscarClientePorEmail("cli@x.com", auth());
        ResponseEntity<ClienteResumoDTO> naoAchado = controller.buscarClientePorEmail("nada@x.com", auth());

        assertEquals(HttpStatus.OK, achado.getStatusCode());
        assertEquals("c-1", achado.getBody().getId());
        assertEquals(HttpStatus.NOT_FOUND, naoAchado.getStatusCode());
    }

    @Test
    void solicitarMudancaEmailDeveRetornarMensagem() {
        Map<String, String> body = Map.of("novoEmail", "novo@email.com");
        when(service.solicitarMudancaEmail("gerente-1", "gerente-1", "novo@email.com")).thenReturn(Map.of("mensagem", "ok"));

        assertEquals("ok", controller.solicitarMudancaEmail("gerente-1", body, auth()).get("mensagem"));
    }

    @Test
    void confirmarMudancaEmailDeveRetornarMensagem() {
        Map<String, String> response = Map.of("mensagem", "Email alterado com sucesso!");

        doNothing().when(service).confirmarMudancaEmail("token-1");

        assertEquals("Email alterado com sucesso!", controller.confirmarMudancaEmail("token-1").get("mensagem"));
        verify(service).confirmarMudancaEmail("token-1");
    }
}
