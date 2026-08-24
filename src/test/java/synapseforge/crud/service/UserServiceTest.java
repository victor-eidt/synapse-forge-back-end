package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EmailService emailService;

    private BCryptPasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new UserService(repository, encoder, emailService);
    }

    @Test
    void toEntityDeveMapearCampos() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Ana");
        dto.setEmail("ana@teste.com");
        dto.setSenha("123");
        dto.setCpf("111");
        dto.setTelefone("222");
        dto.setRole(Role.ADMIN);

        User user = service.toEntity(dto);

        assertEquals("Ana", user.getNome());
        assertEquals("ana@teste.com", user.getEmail());
        assertEquals("123", user.getSenha());
        assertEquals(Role.ADMIN, user.getRole());
    }

    @Test
    void criarDeveLancarQuandoEmailJaExiste() {
        User user = new User();
        user.setEmail("ana@teste.com");

        when(repository.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.criar(user));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(repository, never()).save(any());
    }

    @Test
    void criarVariosDeveCodificarSenhas() {
        User a = new User();
        a.setNome("A");
        a.setEmail("a@teste.com");
        a.setSenha("s1");

        User b = new User();
        b.setNome("B");
        b.setEmail("b@teste.com");
        b.setSenha("s2");

        when(repository.saveAll(anyList())).thenAnswer(invocation -> invocation.getArgument(0));

        List<User> result = service.criarVarios(List.of(a, b));

        assertEquals(2, result.size());
        assertNotEquals("s1", result.get(0).getSenha());
        assertNotEquals("s2", result.get(1).getSenha());
        verify(repository).saveAll(anyList());
    }

    @Test
    void buscarPorNomeDeveRetornarListaQuandoNomeValido() {
        User user = new User();
        user.setNome("Ana");
        when(repository.findByNomeIgnoreCaseContaining("Ana")).thenReturn(List.of(user));

        List<User> result = service.buscarPorNome("Ana");

        assertEquals(1, result.size());
        assertEquals("Ana", result.get(0).getNome());
    }

    @Test
    void atualizarDeveSalvarAlteracoes() {
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        user.setEmail("ana@teste.com");
        user.setCpf("111");
        user.setTelefone("999");
        user.setRole(Role.CLIENTE);

        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Ana Atualizada");
        dto.setEmail("ana_nova@teste.com");
        dto.setCpf("222");
        dto.setTelefone("333");
        dto.setRole(Role.ADMIN);
        dto.setSenha("456");

        when(repository.findById("u-1")).thenReturn(Optional.of(user));
        when(repository.save(user)).thenReturn(user);

        User result = service.atualizar("u-1", dto);

        assertEquals("Ana Atualizada", result.getNome());
        assertEquals(Role.ADMIN, result.getRole());
        assertNotEquals("456", result.getSenha());
        verify(repository).save(user);
    }

    @Test
    void solicitarMudancaEmailDeveEnviarToken() {
        User user = new User();
        user.setId("u-1");
        user.setNome("Ana");
        user.setEmail("ana@teste.com");

        when(repository.findById("u-1")).thenReturn(Optional.of(user));
        when(repository.findByEmail("novo@teste.com")).thenReturn(Optional.empty());

        Map<String, String> result = service.solicitarMudancaEmail("u-1", "novo@teste.com");

        assertTrue(result.get("mensagem").contains("novo@teste.com"));
        assertEquals("novo@teste.com", user.getEmailPendente());
        assertNotNull(user.getEmailMudancaToken());
        verify(emailService).enviarConfirmacaoMudancaEmail(eq("novo@teste.com"), eq("Ana"), anyString());
    }

    @Test
    void confirmarMudancaEmailDeveAtualizarEmail() {
        User user = new User();
        user.setEmail("velho@teste.com");
        user.setEmailPendente("novo@teste.com");
        user.setEmailMudancaToken("token-1");
        user.setEmailMudancaTokenExpira(LocalDateTime.now().plusMinutes(30));

        when(repository.findByEmailMudancaToken("token-1")).thenReturn(Optional.of(user));

        service.confirmarMudancaEmail("token-1");

        assertEquals("novo@teste.com", user.getEmail());
        assertNull(user.getEmailPendente());
        verify(repository).save(user);
    }
}
