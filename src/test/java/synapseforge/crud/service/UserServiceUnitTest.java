package synapseforge.crud.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import synapseforge.crud.DTO.User.PerfilUpdateRequestDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceUnitTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EmailService emailService;

    @Mock
    private EquipeContexto equipeContexto;

    @Mock
    private PedidoRepository pedidoRepository;

    private BCryptPasswordEncoder encoder;
    private UserService userService;

    @BeforeEach
    void setup() {
        encoder = new BCryptPasswordEncoder();
        userService = new UserService(repository, encoder, emailService, equipeContexto, pedidoRepository);
    }

    @Test
    void criarThrowsWhenEmailAlreadyExists() {
        User u = new User();
        u.setEmail("existe@teste.com");
        when(repository.buscarPorEmail(u.getEmail())).thenReturn(Optional.of(u));

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.criar(u));
        assertTrue(ex.getMessage().toLowerCase().contains("email"));
        verify(repository, never()).save(any());
    }

    @Test
    void atualizarThrowsWhenNotFound() {
        when(repository.findById("nope")).thenReturn(Optional.empty());

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.atualizar("nope", "nope", null));
        assertTrue(ex.getMessage().toLowerCase().contains("não encontrado") || ex.getMessage().toLowerCase().contains("nao encontrado"));
    }

    @Test
    void deletarCallsRepositoryDelete() {
        when(repository.findById("del-1")).thenReturn(Optional.of(new User()));
        userService.deletar("del-1", "del-1");
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
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(repository.findByEquipeIdAndNomeIgnoreCaseContaining("eq-1", "ana")).thenReturn(List.of(new User()));
        List<User> res = userService.buscarPorNome("u-1", "ana");
        assertFalse(res.isEmpty());
    }

    // ---- SYN-102: perfil só muda o que veio, senha atual conferida no back ----

    private User usuarioComDados() {
        User u = new User();
        u.setId("u-1");
        u.setNome("Ana");
        u.setEmail("ana@teste.com");
        u.setCpf("12345678900");
        u.setTelefone("11999998888");
        u.setSenha(encoder.encode("senha-antiga"));
        return u;
    }

    @Test
    void perfilComSoONomeNaoApagaEmailCpfETelefone() {
        User u = usuarioComDados();
        when(repository.findById("u-1")).thenReturn(Optional.of(u));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        PerfilUpdateRequestDTO dto = new PerfilUpdateRequestDTO();
        dto.setNome("  Ana Maria ");

        User salvo = userService.atualizarProprioPerfil("u-1", dto);

        assertEquals("Ana Maria", salvo.getNome());
        assertEquals("ana@teste.com", salvo.getEmail());
        assertEquals("12345678900", salvo.getCpf());
        assertEquals("11999998888", salvo.getTelefone());
    }

    @Test
    void perfilRecusaNovaSenhaComSenhaAtualErrada() {
        User u = usuarioComDados();
        when(repository.findById("u-1")).thenReturn(Optional.of(u));

        PerfilUpdateRequestDTO dto = new PerfilUpdateRequestDTO();
        dto.setSenhaAtual("chute");
        dto.setSenha("nova-senha");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> userService.atualizarProprioPerfil("u-1", dto));
        assertEquals("Senha atual incorreta", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void perfilRecusaNovaSenhaSemSenhaAtual() {
        User u = usuarioComDados();
        when(repository.findById("u-1")).thenReturn(Optional.of(u));

        PerfilUpdateRequestDTO dto = new PerfilUpdateRequestDTO();
        dto.setSenha("nova-senha");

        assertThrows(RuntimeException.class, () -> userService.atualizarProprioPerfil("u-1", dto));
        verify(repository, never()).save(any());
    }

    @Test
    void perfilTrocaASenhaQuandoASenhaAtualConfere() {
        User u = usuarioComDados();
        when(repository.findById("u-1")).thenReturn(Optional.of(u));
        when(repository.save(any())).thenAnswer(i -> i.getArgument(0));

        PerfilUpdateRequestDTO dto = new PerfilUpdateRequestDTO();
        dto.setSenhaAtual("senha-antiga");
        dto.setSenha("nova-senha");

        User salvo = userService.atualizarProprioPerfil("u-1", dto);

        assertTrue(encoder.matches("nova-senha", salvo.getSenha()));
    }

    // ---- SYN-102: e-mail sem diferenciar caixa ----

    @Test
    void buscarPorEmailIgnoraCaixaEEspacosEEscapaORegex() {
        User u = usuarioComDados();
        when(repository.buscarPorEmail(anyString())).thenCallRealMethod();
        when(repository.findByEmailPadraoIgnorandoCaixa("^\\QAna.Silva+1@Teste.com\\E$")).thenReturn(List.of(u));

        assertTrue(repository.buscarPorEmail("  Ana.Silva+1@Teste.com ").isPresent());
    }

    @Test
    void normalizarEmailTiraEspacosEMaiusculas() {
        assertEquals("ana@teste.com", UserRepository.normalizarEmail("  Ana@Teste.COM "));
        assertNull(UserRepository.normalizarEmail(null));
    }

    @Test
    void solicitarMudancaEmailGravaONovoEmailNormalizado() {
        User u = usuarioComDados();
        when(repository.findById("u-1")).thenReturn(Optional.of(u));
        when(repository.buscarPorEmail("novo@teste.com")).thenReturn(Optional.empty());

        userService.solicitarMudancaEmail("u-1", " Novo@Teste.com ");

        assertEquals("novo@teste.com", u.getEmailPendente());
    }
}
