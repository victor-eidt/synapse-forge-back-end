package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.User.ClienteResumoDTO;
import synapseforge.crud.DTO.User.PerfilUpdateRequestDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.service.UserService;

import java.util.List;
import java.util.Map;

@Tag(name = "Usuários", description = "Perfil do usuário logado, busca de clientes e gestão de usuários.")
@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;


    // =========================================================
    // CRIAR USUÁRIO
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar usuário (admin)", description = "Cria um usuário com qualquer papel.")
    @PostMapping
    public UserResponseDTO criar(
            @RequestBody @Valid UserRequestDTO dto
    ) {

        User user = service.toEntity(dto);

        User salvo = service.criar(user);

        return service.toResponseDTO(salvo);
    }


    // =========================================================
    // LISTAR USUÁRIOS
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar usuários", description = "Lista os usuários visíveis para o usuário logado.")
    @GetMapping
    public List<UserResponseDTO> listar(
            Authentication auth
    ) {

        return service.listar((String) auth.getPrincipal())
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }


    // =========================================================
    // LISTAR CLIENTES DA EQUIPE
    // =========================================================
    //
    // Clientes com pelo menos um pedido na equipe de quem consulta.
    //

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Listar clientes", description = "Clientes que podem ser vinculados a pedidos (dados mínimos).")
    @GetMapping("/clientes")
    public List<ClienteResumoDTO> listarClientes(
            Authentication auth
    ) {

        return service.listarClientes((String) auth.getPrincipal())
                .stream()
                .map(service::toClienteResumoDTO)
                .toList();
    }


    // =========================================================
    // BUSCAR CLIENTE POR EMAIL EXATO
    // =========================================================
    //
    // Para vincular um cliente novo a um pedido. 404 quando não
    // existe ou não é CLIENTE.
    //

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar cliente por e-mail", description = "Busca exata por e-mail, para vincular um cliente novo a um pedido.")
    @GetMapping("/clientes/buscar")
    public ResponseEntity<ClienteResumoDTO> buscarClientePorEmail(
            @Parameter(description = "E-mail exato do cliente", example = "mariana.costa@email.com") @RequestParam String email,
            Authentication auth
    ) {

        return service.buscarClientePorEmail(
                        (String) auth.getPrincipal(),
                        email
                )
                .map(service::toClienteResumoDTO)
                .map(ResponseEntity::ok)
                .orElseGet(() ->
                        ResponseEntity.notFound().build()
                );
    }


    // =========================================================
    // BUSCAR PRÓPRIO USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Meu perfil", description = "Dados do usuário logado.")
    @GetMapping("/me")
    public UserResponseDTO meuPerfil(
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        User user =
                service.buscarPorId(usuarioId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Usuário não encontrado"
                                )
                        );

        return service.toResponseDTO(user);
    }


    // =========================================================
    // ATUALIZAR PRÓPRIO USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Editar meu perfil", description = "Atualiza nome, CPF e telefone; para trocar a senha, envie senhaAtual e senha.")
    @PutMapping("/me")
    public UserResponseDTO atualizarMeuPerfil(
            @RequestBody PerfilUpdateRequestDTO dto,
            Authentication auth
    ) {

        String usuarioId =
                (String) auth.getPrincipal();

        User atualizado =
                service.atualizarProprioPerfil(
                        usuarioId,
                        dto
                );

        return service.toResponseDTO(
                atualizado
        );
    }


    // =========================================================
    // BUSCAR USUÁRIO POR ID
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar usuário", description = "Retorna um usuário pelo ID.")
    @GetMapping("/{id}")
    public UserResponseDTO buscar(
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id,
            Authentication auth
    ) {

        // próprio usuário, colegas de equipe ou clientes com pedido na equipe
        User user =
                service.buscarParaUsuario(
                                (String) auth.getPrincipal(),
                                id
                        )
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Usuário não encontrado"
                                )
                        );

        return service.toResponseDTO(
                user
        );
    }


    // =========================================================
    // ATUALIZAR USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Editar usuário", description = "Atualiza dados de um usuário.")
    @PutMapping("/{id}")
    public UserResponseDTO atualizar(
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id,
            @RequestBody UserRequestDTO dto,
            Authentication auth
    ) {

        // próprio usuário ou colegas de equipe
        User atualizado =
                service.atualizar(
                        (String) auth.getPrincipal(),
                        id,
                        dto
                );

        return service.toResponseDTO(
                atualizado
        );
    }


    // =========================================================
    // DELETAR USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @Operation(summary = "Excluir usuário", description = "Remove um usuário.")
    @DeleteMapping("/{id}")
    public void deletar(
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id,
            Authentication auth
    ) {

        // próprio usuário ou colegas de equipe
        service.deletar((String) auth.getPrincipal(), id);
    }


    // =========================================================
    // CRIAR VÁRIOS USUÁRIOS
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Criar usuários em lote (admin)", description = "Cadastra vários usuários de uma vez.")
    @PostMapping("/batch")
    public List<UserResponseDTO> criarVarios(
            @RequestBody @Valid List<UserRequestDTO> dtos
    ) {

        List<User> users =
                dtos.stream()
                        .map(service::toEntity)
                        .toList();

        List<User> salvos =
                service.criarVarios(users);

        return salvos.stream()
                .map(service::toResponseDTO)
                .toList();
    }


    // =========================================================
    // BUSCAR POR NOME
    // =========================================================

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Buscar usuários por nome", description = "Busca por trecho do nome, sem diferenciar maiúsculas.")
    @GetMapping("/search")
    public List<UserResponseDTO> buscarPorNome(
            @Parameter(description = "Trecho do nome a buscar", example = "Mariana") @RequestParam String nome,
            Authentication auth
    ) {

        return service.buscarPorNome((String) auth.getPrincipal(), nome)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }


    // =========================================================
    // SOLICITAR MUDANÇA DE EMAIL
    // =========================================================

    @Operation(summary = "Solicitar troca de e-mail", description = "Envia um link de confirmação para o novo e-mail; a troca só vale após confirmar. Só o próprio usuário pode pedir.")
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Novo e-mail", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"novoEmail\": \"mariana.nova@email.com\"}")))
    @PostMapping("/{id}/solicitar-mudanca-email")
    public Map<String, String> solicitarMudancaEmail(
            @Parameter(description = "ID do usuário", example = "6704a1c2e4b0f81a2c3d4e02") @PathVariable String id,
            @RequestBody Map<String, String> body,
            Authentication auth
    ) {

        // somente o próprio usuário
        return service.solicitarMudancaEmail(
                (String) auth.getPrincipal(),
                id,
                body.get("novoEmail")
        );
    }


    // =========================================================
    // CONFIRMAR MUDANÇA DE EMAIL
    // =========================================================

    @Operation(summary = "Confirmar troca de e-mail", description = "Rota pública do link enviado ao novo e-mail.")
    @ApiResponse(responseCode = "200", description = "Troca de e-mail concluída", content = @Content(mediaType = "application/json", examples = @ExampleObject(value = "{\"mensagem\": \"Email alterado com sucesso!\"}")))
    @GetMapping("/confirmar-mudanca-email/{token}")
    public Map<String, String> confirmarMudancaEmail(
            @Parameter(description = "Token recebido por e-mail", example = "3f9c2b7e-8a41-4d2e-9b6f-1c5d7e8a9b0c") @PathVariable String token
    ) {

        service.confirmarMudancaEmail(token);

        return Map.of(
                "mensagem",
                "Email alterado com sucesso!"
        );
    }
}