package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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

@Tag(name = "Admin", description = "Painel do administrador: gestão de todos os usuários e pedidos (papel ADMIN).")
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

    @Operation(summary = "Listar todos os usuários", description = "Lista os usuários de todas as equipes.")
    @GetMapping("/usuarios")
    public List<AdminUserResponseDTO> listarUsuarios() {
        validarAdminAutorizado();
        return adminService.listarUsuarios();
    }

    @Operation(summary = "Buscar usuário", description = "Retorna um usuário pelo ID.")
    @GetMapping("/usuarios/{id}")
    public AdminUserResponseDTO buscarUsuario(
            @Parameter(description = "ID do registro", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id
    ) {
        validarAdminAutorizado();
        return adminService.buscarUsuario(id);
    }

    @Operation(summary = "Editar usuário", description = "Atualiza só os campos enviados (os nulos são mantidos). Permite trocar papel, equipe, ativar/desativar e redefinir a senha.")
    @PutMapping("/usuarios/{id}")
    public AdminUserResponseDTO atualizarUsuario(
            @Parameter(description = "ID do registro", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id,
            @RequestBody AdminUserUpdateRequestDTO dto
    ) {
        validarAdminAutorizado();

        return adminService.atualizarUsuario(id, dto);
    }

    @Operation(summary = "Excluir usuário", description = "Remove o usuário definitivamente.")
    @DeleteMapping("/usuarios/{id}")
    public void deletarUsuario(
            @Parameter(description = "ID do registro", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id
    ) {
        validarAdminAutorizado();

        adminService.deletarUsuario(id);
    }

    // =========================================================
    // PEDIDOS
    // =========================================================

    @Operation(summary = "Listar todos os pedidos", description = "Lista os pedidos de todas as equipes.")
    @GetMapping("/pedidos")
    public List<AdminPedidoResponseDTO> listarPedidos() {
        validarAdminAutorizado();
        return adminService.listarPedidos();
    }

    @Operation(summary = "Buscar pedido", description = "Retorna um pedido pelo ID.")
    @GetMapping("/pedidos/{id}")
    public AdminPedidoResponseDTO buscarPedido(
            @Parameter(description = "ID do registro", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id
    ) {
        validarAdminAutorizado();
        return adminService.buscarPedido(id);
    }


    @Operation(summary = "Editar pedido", description = "Atualiza só os campos enviados. Se a etapa mudar, o cliente recebe aviso no sino e por e-mail.")
    @PutMapping("/pedidos/{id}")
    public AdminPedidoResponseDTO atualizarPedido(
            @Parameter(description = "ID do registro", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id,
            @RequestBody AdminPedidoUpdateRequestDTO dto
    ) {
        validarAdminAutorizado();

        return adminService.atualizarPedido(
                id,
                dto
        );
    }


    @Operation(summary = "Excluir pedido", description = "Remove o pedido e seus arquivos.")
    @DeleteMapping("/pedidos/{id}")
    public void deletarPedido(
            @Parameter(description = "ID do registro", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id
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
