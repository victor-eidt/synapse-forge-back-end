package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import synapseforge.crud.DTO.Admin.AdminPedidoResponseDTO;
import synapseforge.crud.DTO.Admin.AdminPedidoUpdateRequestDTO;
import synapseforge.crud.DTO.Admin.AdminUserResponseDTO;
import synapseforge.crud.DTO.Admin.AdminUserUpdateRequestDTO;
import synapseforge.crud.config.AdminProperties;
import synapseforge.crud.infrastructure.entity.Pedido;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.infrastructure.entity.StatusPedido;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.service.AdminService;

@ExtendWith(MockitoExtension.class)
class AdminControllerTest {

    @Mock
    private AdminService adminService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private AdminProperties adminProperties;

    @InjectMocks
    private AdminController controller;

    private void autenticarComoAdmin(String usuarioId, String email) {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn(usuarioId);
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        when(userRepository.findById(usuarioId)).thenReturn(Optional.of(user(email)));
        when(adminProperties.emailAutorizado(email)).thenReturn(true);
    }

    private User user(String email) {
        User user = new User();
        user.setId("admin-1");
        user.setEmail(email);
        return user;
    }

    @Test
    void listarUsuarios_deveChamarServiceQuandoAdminAutorizado() {
        autenticarComoAdmin("admin-1", "admin@teste.com");
        AdminUserResponseDTO dto = new AdminUserResponseDTO("u-1", "Ana", "ana@teste.com", "123", "999", "ADMIN", null, null, true, null, null);
        when(adminService.listarUsuarios()).thenReturn(List.of(dto));

        var result = controller.listarUsuarios();

        assertEquals(1, result.size());
        assertEquals("u-1", result.get(0).getId());
    }

    @Test
    void buscarUsuario_deveRetornarDto() {
        autenticarComoAdmin("admin-1", "admin@teste.com");
        AdminUserResponseDTO dto = new AdminUserResponseDTO("u-1", "Ana", "ana@teste.com", "123", "999", "ADMIN", null, null, true, null, null);
        when(adminService.buscarUsuario("u-1")).thenReturn(dto);

        assertEquals("Ana", controller.buscarUsuario("u-1").getNome());
    }

    @Test
    void atualizarUsuario_deveDelegarAoService() {
        autenticarComoAdmin("admin-1", "admin@teste.com");
        AdminUserUpdateRequestDTO dto = new AdminUserUpdateRequestDTO();
        dto.setNome("Ana Nova");
        AdminUserResponseDTO response = new AdminUserResponseDTO("u-1", "Ana Nova", "ana@teste.com", "123", "999", "ADMIN", null, null, true, null, null);
        when(adminService.atualizarUsuario("u-1", dto)).thenReturn(response);

        assertEquals("Ana Nova", controller.atualizarUsuario("u-1", dto).getNome());
    }

    @Test
    void deletarUsuario_deveDelegarAoService() {
        autenticarComoAdmin("admin-1", "admin@teste.com");

        controller.deletarUsuario("u-1");

        verify(adminService).deletarUsuario("u-1");
    }

    @Test
    void listarPedidos_deveRetornarLista() {
        autenticarComoAdmin("admin-1", "admin@teste.com");
        AdminPedidoResponseDTO dto = new AdminPedidoResponseDTO("p-1", "cli-1", "Cliente", "Projeto", "Desc", "orc-1", "mat-1", 10.0, 2.0, 1.0, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(20), BigDecimal.valueOf(50), BigDecimal.valueOf(60), BigDecimal.valueOf(10), BigDecimal.valueOf(120), BigDecimal.valueOf(150), StatusPedido.IMPRESSAO.name(), LocalDate.now(), null, null, "obj-1", 3);
        when(adminService.listarPedidos()).thenReturn(List.of(dto));

        var result = controller.listarPedidos();

        assertEquals(1, result.size());
        assertEquals("p-1", result.get(0).getId());
    }

    @Test
    void atualizarPedido_deveDelegarAoService() {
        autenticarComoAdmin("admin-1", "admin@teste.com");
        AdminPedidoUpdateRequestDTO dto = new AdminPedidoUpdateRequestDTO();
        dto.setStatus(StatusPedido.IMPRESSAO);
        AdminPedidoResponseDTO response = new AdminPedidoResponseDTO("p-1", "cli-1", "Cliente", "Projeto", "Desc", "orc-1", "mat-1", 10.0, 2.0, 1.0, BigDecimal.TEN, BigDecimal.ONE, BigDecimal.valueOf(20), BigDecimal.valueOf(50), BigDecimal.valueOf(60), BigDecimal.valueOf(10), BigDecimal.valueOf(120), BigDecimal.valueOf(150), StatusPedido.IMPRESSAO.name(), LocalDate.now(), null, null, "obj-1", 3);
        when(adminService.atualizarPedido("p-1", dto)).thenReturn(response);

        assertEquals(StatusPedido.IMPRESSAO.name(), controller.atualizarPedido("p-1", dto).getStatus());
    }

    @Test
    void deletarPedido_deveDelegarAoService() {
        autenticarComoAdmin("admin-1", "admin@teste.com");

        controller.deletarPedido("p-1");

        verify(adminService).deletarPedido("p-1");
    }

    @Test
    void validarAdminAutorizado_deveNegarQuandoEmailNaoAutorizado() {
        SecurityContext context = SecurityContextHolder.createEmptyContext();
        org.springframework.security.core.Authentication auth = mock(org.springframework.security.core.Authentication.class);
        when(auth.getPrincipal()).thenReturn("admin-1");
        context.setAuthentication(auth);
        SecurityContextHolder.setContext(context);

        when(userRepository.findById("admin-1")).thenReturn(Optional.of(user("outro@teste.com")));
        when(adminProperties.emailAutorizado("outro@teste.com")).thenReturn(false);

        try {
            controller.listarUsuarios();
            throw new AssertionError("Esperava AccessDeniedException");
        } catch (AccessDeniedException ex) {
            assertEquals("Administrador não autorizado", ex.getMessage());
        }
    }
}
