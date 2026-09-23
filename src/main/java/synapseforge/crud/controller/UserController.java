package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

import synapseforge.crud.DTO.User.ClienteResumoDTO;
import synapseforge.crud.DTO.User.UserRequestDTO;
import synapseforge.crud.DTO.User.UserResponseDTO;
import synapseforge.crud.infrastructure.entity.User;
import synapseforge.crud.service.UserService;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/users")
public class UserController {

    @Autowired
    private UserService service;


    // =========================================================
    // CRIAR USUÁRIO
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
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
    @GetMapping("/clientes/buscar")
    public ResponseEntity<ClienteResumoDTO> buscarClientePorEmail(
            @RequestParam String email,
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
    @PutMapping("/me")
    public UserResponseDTO atualizarMeuPerfil(
            @RequestBody UserRequestDTO dto,
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

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @GetMapping("/{id}")
    public UserResponseDTO buscar(
            @PathVariable String id,
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
    @PutMapping("/{id}")
    public UserResponseDTO atualizar(
            @PathVariable String id,
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
    @DeleteMapping("/{id}")
    public void deletar(
            @PathVariable String id,
            Authentication auth
    ) {

        // próprio usuário ou colegas de equipe
        service.deletar((String) auth.getPrincipal(), id);
    }


    // =========================================================
    // CRIAR VÁRIOS USUÁRIOS
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
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

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @GetMapping("/search")
    public List<UserResponseDTO> buscarPorNome(
            @RequestParam String nome,
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

    @PostMapping("/{id}/solicitar-mudanca-email")
    public Map<String, String> solicitarMudancaEmail(
            @PathVariable String id,
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

    @GetMapping("/confirmar-mudanca-email/{token}")
    public Map<String, String> confirmarMudancaEmail(
            @PathVariable String token
    ) {

        service.confirmarMudancaEmail(token);

        return Map.of(
                "mensagem",
                "Email alterado com sucesso!"
        );
    }
}