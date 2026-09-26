package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import synapseforge.crud.DTO.Admin.AdminPedidoUpdateRequestDTO;
import synapseforge.crud.DTO.Admin.AdminUserUpdateRequestDTO;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.PedidoRepository;
import synapseforge.crud.infrastructure.repository.UserRepository;

@ExtendWith(MockitoExtension.class)
class AdminServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PedidoRepository pedidoRepository;

    @InjectMocks
    private AdminService service;

    @Test
    void listarUsuarios_deveMapearParaDto() {
        when(userRepository.findAll()).thenReturn(List.of(usuario("u-1", "Ana", Role.ADMIN)));
        var result = service.listarUsuarios();

        assertEquals(1, result.size());
        assertEquals("u-1", result.get(0).getId());
        assertEquals("ADMIN", result.get(0).getRole());
    }

    @Test
    void buscarUsuario_deveRetornarDtoOuErro() {
        when(userRepository.findById("u-1")).thenReturn(Optional.of(usuario("u-1", "Ana", Role.ADMIN)));

        assertEquals("Ana", service.buscarUsuario("u-1").getNome());
        assertThrows(RuntimeException.class, () -> service.buscarUsuario("missing"));
    }

    @Test
    void atualizarUsuario_deveAtualizarCamposDoPerfil() {
        User user = usuario("u-1", "Ana", Role.ADMIN);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.buscarPorEmail("novo@teste.com")).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminUserUpdateRequestDTO dto = new AdminUserUpdateRequestDTO();
        dto.setNome("Ana Nova");
        dto.setEmail("novo@teste.com");
        dto.setCpf("123");
        dto.setTelefone("999");
        dto.setRole(Role.TECNICO);
        dto.setEquipeId("eq-1");
        dto.setFuncaoVisual("Visual");
        dto.setAtivo(true);

        var result = service.atualizarUsuario("u-1", dto);

        assertEquals("Ana Nova", result.getNome());
        assertEquals("novo@teste.com", result.getEmail());
        assertEquals(Role.TECNICO.name(), result.getRole());
        assertTrue(result.isAtivo());
        verify(userRepository).save(user);
    }

    @Test
    void atualizarUsuario_deveRecusarEmailDuplicado() {
        User user = usuario("u-1", "Ana", Role.ADMIN);
        User outro = usuario("u-2", "Bia", Role.TECNICO);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));
        when(userRepository.buscarPorEmail("outro@teste.com")).thenReturn(Optional.of(outro));

        AdminUserUpdateRequestDTO dto = new AdminUserUpdateRequestDTO();
        dto.setEmail("outro@teste.com");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.atualizarUsuario("u-1", dto));
        assertTrue(ex.getMessage().contains("email"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void atualizarUsuario_deveProibirMudancaDeSenha() {
        User user = usuario("u-1", "Ana", Role.ADMIN);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        AdminUserUpdateRequestDTO dto = new AdminUserUpdateRequestDTO();
        dto.setSenha("novaSenha");

        RuntimeException ex = assertThrows(RuntimeException.class, () -> service.atualizarUsuario("u-1", dto));
        assertTrue(ex.getMessage().contains("senha"));
    }

    @Test
    void deletarUsuario_deveExcluir() {
        User user = usuario("u-1", "Ana", Role.ADMIN);
        when(userRepository.findById("u-1")).thenReturn(Optional.of(user));

        service.deletarUsuario("u-1");

        verify(userRepository).delete(user);
    }

    @Test
    void listarPedidos_eBuscarPedido_deveMapear() {
        Pedido pedido = pedido("p-1");
        when(pedidoRepository.findAll()).thenReturn(List.of(pedido));
        when(pedidoRepository.findById("p-1")).thenReturn(Optional.of(pedido));

        assertEquals(1, service.listarPedidos().size());
        assertEquals("p-1", service.buscarPedido("p-1").getId());
    }

    @Test
    void atualizarPedido_deveAtualizarCampos() {
        Pedido pedido = pedido("p-1");
        when(pedidoRepository.findById("p-1")).thenReturn(Optional.of(pedido));
        when(pedidoRepository.save(any(Pedido.class))).thenAnswer(inv -> inv.getArgument(0));

        AdminPedidoUpdateRequestDTO dto = new AdminPedidoUpdateRequestDTO();
        dto.setClienteId("cli-1");
        dto.setCliente("Cliente A");
        dto.setProjeto("Projeto A");
        dto.setDescricao("Descricao A");
        dto.setMaterialId("mat-1");
        dto.setVolumeCm3(25.0);
        dto.setTempoImpressaoHoras(5.0);
        dto.setTempoMaoDeObraHoras(3.0);
        dto.setCustoMaquinaHora(new BigDecimal("40"));
        dto.setCustoMaoDeObraHora(new BigDecimal("20"));
        dto.setMargemLucro(new BigDecimal("15"));
        dto.setCustoMaterial(new BigDecimal("100"));
        dto.setCustoMaquina(new BigDecimal("200"));
        dto.setCustoMaoDeObra(new BigDecimal("60"));
        dto.setCustoTotal(new BigDecimal("360"));
        dto.setPrecoFinal(new BigDecimal("480"));
        dto.setStatus(StatusPedido.IMPRESSAO);
        dto.setPrazo(LocalDate.now().plusDays(7));

        var result = service.atualizarPedido("p-1", dto);

        assertEquals("cli-1", result.getClienteId());
        assertEquals("Cliente A", result.getCliente());
        assertEquals(StatusPedido.IMPRESSAO.name(), result.getStatus());
        assertEquals(new BigDecimal("480"), result.getPrecoFinal());
    }

    @Test
    void deletarPedido_deveExcluir() {
        Pedido pedido = pedido("p-1");
        when(pedidoRepository.findById("p-1")).thenReturn(Optional.of(pedido));

        service.deletarPedido("p-1");

        verify(pedidoRepository).delete(pedido);
    }

    private User usuario(String id, String nome, Role role) {
        User user = new User();
        user.setId(id);
        user.setNome(nome);
        user.setEmail(id + "@teste.com");
        user.setRole(role);
        return user;
    }

    private Pedido pedido(String id) {
        Pedido pedido = new Pedido();
        pedido.setId(id);
        pedido.setClienteId("cli-1");
        pedido.setCliente("Cliente A");
        pedido.setProjeto("Projeto A");
        pedido.setDescricao("Descricao A");
        pedido.setMaterialId("mat-1");
        pedido.setStatus(StatusPedido.MODELAGEM);
        pedido.setCriadoEm(java.time.LocalDateTime.now());
        pedido.setAtualizadoEm(java.time.LocalDateTime.now());
        return pedido;
    }
}
