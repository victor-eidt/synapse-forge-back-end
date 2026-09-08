package synapseforge.crud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EmailService emailService;

    private BCryptPasswordEncoder encoder;
    private UserService userService;

    @BeforeEach
    void setup() {
        encoder = new BCryptPasswordEncoder();
        userService = new UserService(repository, encoder, emailService);
    }

    @Test
    void criarThrowsWhenEmailAlreadyExists() {
        User u = new User();
        u.setEmail("existe@teste.com");
        when(repository.findByEmail(u.getEmail())).thenReturn(Optional.of(u));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.criar(u));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarThrowsWhenNotFound() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.atualizar("nope", null));
        assertTrue(ex.getMessage().toLowerCase().contains("não encontrado") || ex.getMessage().toLowerCase().contains("nao encontrado"));
    }

    @Test
    void deletarCallsRepositoryDelete() {
        when(repository.existsById("del-1")).thenReturn(true);
        userService.deletar("del-1");
        verify(repository).deleteById("del-1");
    }

    @Test
    void criarVariosEncodesPasswordsAndSavesAll() {
        User a = new User(); a.setSenha("s1"); a.setNome("A");
        User b = new User(); b.setSenha("s2"); b.setNome("B");

        when(repository.saveAll(any())).thenAnswer(i -> i.getArgument(0));

        List<User> out = userService.criarVarios(List.of(a, b));
        assertEquals(2, out.size());
        assertNotEquals("s1", out.get(0).getSenha());
        assertNotEquals("s2", out.get(1).getSenha());
        verify(repository).saveAll(any());
    }

    @Test
    void buscarPorNomeReturnsListWhenValid() {
        when(repository.findByNomeIgnoreCaseContaining("ana")).thenReturn(List.of(new User()));
        List<User> res = userService.buscarPorNome("ana");
        assertFalse(res.isEmpty());
    }
}
