package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import synapseforge.crud.DTO.User.LoginDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.infrastructure.entity.Equipe;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.infrastructure.security.JwtService;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EmailService emailService;

    @Mock
    private JwtService jwtService;

    @Mock
    private BCryptPasswordEncoder encoder;

    @Mock
    private EquipeService equipeService;

    @InjectMocks
    private AuthService service;

    @Test
    void cadastroDeveSalvarUsuarioEEnviarEmail() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Ana");
        dto.setEmail("ana@teste.com");
        dto.setSenha("123456");
        dto.setCpf("111");
        dto.setTelefone("9999");
        dto.setRole(Role.ADMIN);

        when(repository.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(encoder.encode("123456")).thenReturn("hash");

        Map<String, String> result = service.cadastro(dto);

        assertEquals("Conta criada! Verifique seu email para confirmar o acesso.", result.get("mensagem"));
        verify(repository).save(any(User.class));
        verify(emailService).enviarConfirmacaoCadastro(eq("ana@teste.com"), eq("Ana"), anyString());
    }

    private UserRequestDTO dtoGerente(String nomeEquipe) {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Gabi");
        dto.setEmail("gabi@loja.com");
        dto.setSenha("123456");
        dto.setNomeEquipe(nomeEquipe);
        return dto;
    }

    @Test
    void cadastroGerenteCriaALojaJuntoComAConta() {
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");

        when(repository.findByEmail("gabi@loja.com")).thenReturn(Optional.empty());
        when(encoder.encode("123456")).thenReturn("hash");
        when(equipeService.criar(any(), eq("Ateliê da Gabi"), isNull(), isNull())).thenReturn(equipe);

        service.cadastroGerente(dtoGerente("  Ateliê da Gabi  "));

        verify(equipeService).criar(any(), eq("Ateliê da Gabi"), isNull(), isNull());
        verify(repository, atLeastOnce()).save(argThat(u ->
                u.getRole() == Role.GERENTE && "eq-1".equals(u.getEquipeId())));
    }

    @Test
    void cadastroGerenteSemNomeDaLojaERecusadoSemCriarNada() {
        when(repository.findByEmail("gabi@loja.com")).thenReturn(Optional.empty());

        RuntimeException erro = assertThrows(RuntimeException.class,
                () -> service.cadastroGerente(dtoGerente("   ")));

        assertEquals("Informe o nome da loja", erro.getMessage());
        verify(repository, never()).save(any());
        verifyNoInteractions(equipeService, emailService);
    }

    @Test
    void cadastroGerenteDesfazLojaEContaSeOEmailFalhar() {
        Equipe equipe = new Equipe();
        equipe.setId("eq-1");

        when(repository.findByEmail("gabi@loja.com")).thenReturn(Optional.empty());
        when(encoder.encode("123456")).thenReturn("hash");
        when(equipeService.criar(any(), eq("Loja"), isNull(), isNull())).thenReturn(equipe);
        doThrow(new RuntimeException("SMTP fora")).when(emailService)
                .enviarConfirmacaoCadastro(anyString(), anyString(), anyString());

        assertThrows(RuntimeException.class, () -> service.cadastroGerente(dtoGerente("Loja")));

        verify(equipeService).deletar(eq("eq-1"), any());
        verify(repository).delete(any(User.class));
    }

    @Test
    void cadastroClienteNaoCriaLoja() {
        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Ana");
        dto.setEmail("ana@teste.com");
        dto.setSenha("123456");
        dto.setNomeEquipe("Ignorado");

        when(repository.findByEmail("ana@teste.com")).thenReturn(Optional.empty());
        when(encoder.encode("123456")).thenReturn("hash");

        service.cadastro(dto);

        verifyNoInteractions(equipeService);
    }

    @Test
    void loginDeveRetornarTokenQuandoCredenciaisForemValidas() {
        LoginDTO dto = new LoginDTO();
        dto.setEmail("ana@teste.com");
        dto.setSenha("123456");

        User user = new User();
        user.setId("u-1");
        user.setEmail("ana@teste.com");
        user.setSenha("hash");
        user.setEmailConfirmado(true);
        user.setTentativasLogin(0);
        user.setRole(Role.CLIENTE);

        when(repository.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));
        when(encoder.matches("123456", "hash")).thenReturn(true);
        when(jwtService.generateToken("u-1", Role.CLIENTE)).thenReturn("jwt-token");

        Map<String, String> result = service.login(dto);

        assertEquals("jwt-token", result.get("access_token"));
        assertEquals("u-1", result.get("user_id"));
    }

    @Test
    void confirmarEmailDeveAtualizarStatusDoUsuario() {
        User user = new User();
        user.setId("u-1");
        user.setEmail("ana@teste.com");
        user.setEmailConfirmToken("token-123");
        user.setEmailConfirmTokenExpira(LocalDateTime.now().plusHours(1));
        user.setRole(Role.CLIENTE);

        when(repository.findByEmailConfirmToken("token-123")).thenReturn(Optional.of(user));
        when(jwtService.generateToken("u-1", Role.CLIENTE)).thenReturn("jwt-confirmado");

        Map<String, String> result = service.confirmarEmail("token-123");

        assertTrue(user.isEmailConfirmado());
        assertEquals("jwt-confirmado", result.get("access_token"));
        verify(repository).save(user);
    }

    @Test
    void esqueciSenhaDeveSalvarTokenQuandoUsuarioExistir() {
        User user = new User();
        user.setEmail("ana@teste.com");
        user.setNome("Ana");

        when(repository.findByEmail("ana@teste.com")).thenReturn(Optional.of(user));

        service.esqueciSenha("ana@teste.com");

        assertNotNull(user.getResetToken());
        assertNotNull(user.getResetTokenExpira());
        verify(repository).save(user);
        verify(emailService).enviarRecuperacaoSenha(eq("ana@teste.com"), eq("Ana"), anyString());
    }

    @Test
    void redefinirSenhaDeveAtualizarSenhaEResetarToken() {
        User user = new User();
        user.setEmail("ana@teste.com");
        user.setResetToken("token-456");
        user.setResetTokenExpira(LocalDateTime.now().plusHours(1));
        user.setTentativasLogin(2);
        user.setBloqueadoEm(LocalDateTime.now());

        when(repository.findByResetToken("token-456")).thenReturn(Optional.of(user));
        when(encoder.encode("novaSenha")).thenReturn("senha-nova");

        service.redefinirSenha("ana@teste.com", "token-456", "novaSenha");

        assertEquals("senha-nova", user.getSenha());
        assertNull(user.getResetToken());
        assertNull(user.getResetTokenExpira());
        verify(repository).save(user);
    }
}
