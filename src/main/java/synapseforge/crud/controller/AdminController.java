package synapseforge.crud.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Admin.AdminPedidoResponseDTO;
import synapseforge.crud.DTO.Admin.AdminUserResponseDTO;
import synapseforge.crud.DTO.Admin.AdminUserUpdateRequestDTO;
import synapseforge.crud.config.AdminProperties;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.infrastructure.repository.UserRepository;
import synapseforge.crud.service.AdminService;
import synapseforge.crud.DTO.Admin.AdminPedidoUpdateRequestDTO;


import java.util.List;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;
    private final AdminProperties adminProperties;

    // =========================================================
    // USUÁRIOS
    // =========================================================

    @GetMapping("/usuarios")
    public List<AdminUserResponseDTO> listarUsuarios() {
        validarAdminAutorizado();
        return adminService.listarUsuarios();
    }

    @GetMapping("/usuarios/{id}")
    public AdminUserResponseDTO buscarUsuario(
            @PathVariable String id
    ) {
        validarAdminAutorizado();
        return adminService.buscarUsuario(id);
    }

    @PutMapping("/usuarios/{id}")
    public AdminUserResponseDTO atualizarUsuario(
            @PathVariable String id,
            @RequestBody AdminUserUpdateRequestDTO dto
    ) {
        validarAdminAutorizado();

        return adminService.atualizarUsuario(id, dto);
    }

    @DeleteMapping("/usuarios/{id}")
    public void deletarUsuario(
            @PathVariable String id
    ) {
        validarAdminAutorizado();

        adminService.deletarUsuario(id);
    }

    // =========================================================
    // PEDIDOS
    // =========================================================

    @GetMapping("/pedidos")
    public List<AdminPedidoResponseDTO> listarPedidos() {
        validarAdminAutorizado();
        return adminService.listarPedidos();
    }

    @GetMapping("/pedidos/{id}")
    public AdminPedidoResponseDTO buscarPedido(
            @PathVariable String id
    ) {
        validarAdminAutorizado();
        return adminService.buscarPedido(id);
    }


    @PutMapping("/pedidos/{id}")
    public AdminPedidoResponseDTO atualizarPedido(
            @PathVariable String id,
            @RequestBody AdminPedidoUpdateRequestDTO dto
    ) {
        validarAdminAutorizado();

        return adminService.atualizarPedido(
                id,
                dto
        );
    }


    @DeleteMapping("/pedidos/{id}")
    public void deletarPedido(
            @PathVariable String id
    ) {
        validarAdminAutorizado();

        adminService.deletarPedido(id);
    }


    // =========================================================
    // SEGURANÇA
    // =========================================================

    private void validarAdminAutorizado() {
        String usuarioId =
                org.springframework.security.core.context.SecurityContextHolder
                        .getContext()
                        .getAuthentication()
                        .getPrincipal()
                        .toString();

        User user = userRepository.findById(usuarioId)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário administrador não encontrado"
                        )
                );

        if (!adminProperties.emailAutorizado(user.getEmail())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Administrador não autorizado"
            );
        }
    }
}
