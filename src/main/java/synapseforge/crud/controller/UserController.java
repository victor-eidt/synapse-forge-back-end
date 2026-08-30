package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.core.Authentication;

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
    public List<UserResponseDTO> listar() {

        return service.listar()
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }


    // =========================================================
    // BUSCAR PRÓPRIO USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
    @GetMapping("/me")
    public UserResponseDTO meuPerfil(
            Authentication auth
    ) {

        String usuarioId = (String) auth.getPrincipal();

        User user = service.buscarPorId(usuarioId)
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

        String usuarioId = (String) auth.getPrincipal();

        User atualizado = service.atualizarProprioPerfil(
                usuarioId,
                dto
        );

        return service.toResponseDTO(atualizado);
    }


    // =========================================================
    // BUSCAR USUÁRIO POR ID
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @GetMapping("/{id}")
    public UserResponseDTO buscar(
            @PathVariable String id
    ) {

        User user = service.buscarPorId(id)
                .orElseThrow(() ->
                        new RuntimeException(
                                "Usuário não encontrado"
                        )
                );

        return service.toResponseDTO(user);
    }


    // =========================================================
    // ATUALIZAR USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @PutMapping("/{id}")
    public UserResponseDTO atualizar(
            @PathVariable String id,
            @RequestBody UserRequestDTO dto
    ) {

        User atualizado = service.atualizar(
                id,
                dto
        );

        return service.toResponseDTO(atualizado);
    }


    // =========================================================
    // DELETAR USUÁRIO
    // =========================================================

    @PreAuthorize("hasAnyRole('GERENTE', 'ADMIN')")
    @DeleteMapping("/{id}")
    public void deletar(
            @PathVariable String id
    ) {

        service.deletar(id);
    }


    // =========================================================
    // CRIAR VÁRIOS USUÁRIOS
    // =========================================================

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/batch")
    public List<UserResponseDTO> criarVarios(
            @RequestBody @Valid List<UserRequestDTO> dtos
    ) {

        List<User> users = dtos.stream()
                .map(service::toEntity)
                .toList();

        List<User> salvos = service.criarVarios(users);

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
            @RequestParam String nome
    ) {

        return service.buscarPorNome(nome)
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
            @RequestBody Map<String, String> body
    ) {

        return service.solicitarMudancaEmail(
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