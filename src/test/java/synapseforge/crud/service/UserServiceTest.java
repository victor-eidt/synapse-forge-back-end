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
import synapseforge.crud.DTO.User.ClienteResumoDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository repository;

    @Mock
    private EmailService emailService;

    @Mock
    private EquipeContexto equipeContexto;

    @Mock
    private PedidoRepository pedidoRepository;

    private BCryptPasswordEncoder encoder;
    private UserService service;

    @BeforeEach
    void setUp() {
        encoder = new BCryptPasswordEncoder();
        service = new UserService(repository, encoder, emailService, equipeContexto, pedidoRepository);
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
    void buscarPorNomeDeveRetornarListaDaEquipeQuandoNomeValido() {
        User user = new User();
        user.setNome("Ana");
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(repository.findByEquipeIdAndNomeIgnoreCaseContaining("eq-1", "Ana")).thenReturn(List.of(user));

        List<User> result = service.buscarPorNome("u-1", "Ana");

        assertEquals(1, result.size());
        assertEquals("Ana", result.get(0).getNome());
        verify(repository, never()).findByNomeIgnoreCaseContaining(anyString());
    }

    // =========================================================
    // LISTAGENS POR EQUIPE (SYN-100)
    // =========================================================

    @Test
    void listarTrazSoUsuariosDaEquipe() {
        User colega = new User();
        colega.setEquipeId("eq-1");
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(repository.findByEquipeId("eq-1")).thenReturn(List.of(colega));

        assertEquals(List.of(colega), service.listar("u-1"));
        verify(repository, never()).findAll();
    }

    private User usuario(String id, String nome, Role role, String equipeId) {
        User user = new User();
        user.setId(id);
        user.setNome(nome);
        user.setEmail(id + "@teste.com");
        user.setRole(role);
        user.setEquipeId(equipeId);
        return user;
    }

    private Pedido pedidoDoCliente(String clienteId) {
        Pedido pedido = new Pedido();
        pedido.setClienteId(clienteId);
        return pedido;
    }

    @Test
    void clientesDaEquipeSaoOsQueTemPedidoNela() {
        // clientes não têm equipe: vêm dos pedidos da equipe, sem repetição, só CLIENTE, por nome
        User bia = usuario("c-2", "Bia", Role.CLIENTE, null);
        User ana = usuario("c-1", "Ana", Role.CLIENTE, null);
        User tecnicoVinculadoPorEngano = usuario("t-1", "Téc", Role.TECNICO, "eq-1");
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(pedidoRepository.findClienteIdsByEquipeId("eq-1")).thenReturn(List.of(
                pedidoDoCliente("c-2"), pedidoDoCliente("c-1"), pedidoDoCliente("c-2"),
                pedidoDoCliente(null), pedidoDoCliente("t-1")));
        when(repository.findAllById(Set.of("c-2", "c-1", "t-1")))
                .thenReturn(List.of(bia, tecnicoVinculadoPorEngano, ana));

        List<User> result = service.listarClientes("u-1");

        assertEquals(List.of(ana, bia), result);
        verify(repository, never()).findAll();
    }

    @Test
    void equipeSemPedidoComClienteNaoListaNinguem() {
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(pedidoRepository.findClienteIdsByEquipeId("eq-1")).thenReturn(List.of());

        assertTrue(service.listarClientes("u-1").isEmpty());
        verify(repository, never()).findAllById(any());
    }

    @Test
    void toClienteResumoDTOExpoeSoIdNomeEEmail() {
        ClienteResumoDTO dto = service.toClienteResumoDTO(usuario("c-1", "Ana", Role.CLIENTE, null));

        assertEquals("c-1", dto.getId());
        assertEquals("Ana", dto.getNome());
        assertEquals("c-1@teste.com", dto.getEmail());
    }

    // =========================================================
    // BUSCA DE CLIENTE POR EMAIL (SYN-100)
    // =========================================================

    @Test
    void buscaClientePorEmailExatoSemDiferenciarCaixa() {
        User cliente = usuario("c-1", "Ana", Role.CLIENTE, null);
        when(equipeContexto.equipeObrigatoria("u-1")).thenReturn("eq-1");
        when(repository.findByEmailPadraoIgnorandoCaixa("^\\QAna.Silva+3d@Teste.com\\E$"))
                .thenReturn(List.of(cliente));

        Optional<User> result = service.buscarClientePorEmail("u-1", "  Ana.Silva+3d@Teste.com ");

        assertEquals(Optional.of(cliente), result);
    }

    @Test
    void buscaPorEmailNaoRevelaQuemNaoECliente() {
        when(equipeContexto.equipeObrigatoria("u-1")).thenReturn("eq-1");
        when(repository.findByEmailPadraoIgnorandoCaixa(anyString()))
                .thenReturn(List.of(usuario("g-1", "Gerente", Role.GERENTE, "eq-2")));

        assertTrue(service.buscarClientePorEmail("u-1", "g-1@teste.com").isEmpty());
        assertTrue(service.buscarClientePorEmail("u-1", " ").isEmpty());
    }

    @Test
    void buscaPorEmailExigeEquipe() {
        when(equipeContexto.equipeObrigatoria("u-1")).thenThrow(new SemEquipeException());

        assertThrows(SemEquipeException.class, () -> service.buscarClientePorEmail("u-1", "a@b.com"));
        verifyNoInteractions(repository);
    }

    // =========================================================
    // /users/{id}: PRÓPRIO USUÁRIO, EQUIPE OU CLIENTE VINCULADO (SYN-100)
    // =========================================================

    @Test
    void proprioUsuarioSempreAcessa() {
        User eu = usuario("u-1", "Eu", Role.GERENTE, null);
        when(repository.findById("u-1")).thenReturn(Optional.of(eu));
        when(repository.save(eu)).thenReturn(eu);
        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Eu Mesmo");

        assertEquals(Optional.of(eu), service.buscarParaUsuario("u-1", "u-1"));
        assertEquals("Eu Mesmo", service.atualizar("u-1", "u-1", dto).getNome());
        service.deletar("u-1", "u-1");

        verify(repository).deleteById("u-1");
        verifyNoInteractions(equipeContexto);
    }

    @Test
    void colegaDeEquipeLeEditaEExclui() {
        User colega = usuario("u-2", "Colega", Role.TECNICO, "eq-1");
        when(repository.findById("u-2")).thenReturn(Optional.of(colega));
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(equipeContexto.equipeDe(colega)).thenReturn(Optional.of("eq-1"));
        when(repository.save(colega)).thenReturn(colega);
        UserRequestDTO dto = new UserRequestDTO();
        dto.setNome("Colega 2");

        assertEquals(Optional.of(colega), service.buscarParaUsuario("u-1", "u-2"));
        assertEquals("Colega 2", service.atualizar("u-1", "u-2", dto).getNome());
        service.deletar("u-1", "u-2");

        verify(repository).deleteById("u-2");
    }

    @Test
    void usuarioDeOutraEquipeSeComportaComoInexistente() {
        User deFora = usuario("u-9", "De Fora", Role.GERENTE, "eq-2");
        when(repository.findById("u-9")).thenReturn(Optional.of(deFora));
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(equipeContexto.equipeDe(deFora)).thenReturn(Optional.of("eq-2"));

        assertTrue(service.buscarParaUsuario("u-1", "u-9").isEmpty());
        assertEquals("Usuário não encontrado",
                assertThrows(RuntimeException.class,
                        () -> service.atualizar("u-1", "u-9", new UserRequestDTO())).getMessage());
        assertEquals("Usuário não encontrado",
                assertThrows(RuntimeException.class, () -> service.deletar("u-1", "u-9")).getMessage());

        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void clienteComPedidoNaEquipeSoPodeSerLido() {
        User cliente = usuario("c-1", "Cliente", Role.CLIENTE, null);
        when(repository.findById("c-1")).thenReturn(Optional.of(cliente));
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(equipeContexto.equipeDe(cliente)).thenReturn(Optional.empty());
        when(pedidoRepository.existsByEquipeIdAndClienteId("eq-1", "c-1")).thenReturn(true);

        assertEquals(Optional.of(cliente), service.buscarParaUsuario("u-1", "c-1"));
        assertThrows(RuntimeException.class, () -> service.atualizar("u-1", "c-1", new UserRequestDTO()));
        assertThrows(RuntimeException.class, () -> service.deletar("u-1", "c-1"));

        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void clienteSemPedidoNaEquipeNaoEEncontrado() {
        User cliente = usuario("c-1", "Cliente", Role.CLIENTE, null);
        when(repository.findById("c-1")).thenReturn(Optional.of(cliente));
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.of("eq-1"));
        when(equipeContexto.equipeDe(cliente)).thenReturn(Optional.empty());
        when(pedidoRepository.existsByEquipeIdAndClienteId("eq-1", "c-1")).thenReturn(false);

        assertTrue(service.buscarParaUsuario("u-1", "c-1").isEmpty());
    }

    @Test
    void semEquipeSoAcessaASiMesmo() {
        when(repository.findById("u-2")).thenReturn(Optional.of(usuario("u-2", "Outro", Role.TECNICO, "eq-1")));
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.empty());

        assertTrue(service.buscarParaUsuario("u-1", "u-2").isEmpty());
        verifyNoInteractions(pedidoRepository);
    }

    @Test
    void mudancaDeEmailSoParaOProprioUsuario() {
        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.solicitarMudancaEmail("u-1", "u-2", "novo@teste.com"));

        assertEquals("Usuário não encontrado", ex.getMessage());
        verifyNoInteractions(repository, emailService);
    }

    @Test
    void usuarioSemEquipeNaoListaNinguem() {
        when(equipeContexto.equipeDe("u-1")).thenReturn(Optional.empty());

        assertTrue(service.listar("u-1").isEmpty());
        assertTrue(service.listarClientes("u-1").isEmpty());
        assertTrue(service.buscarPorNome("u-1", "Ana").isEmpty());
        verifyNoInteractions(repository);
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

        User result = service.atualizar("u-1", "u-1", dto);

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
