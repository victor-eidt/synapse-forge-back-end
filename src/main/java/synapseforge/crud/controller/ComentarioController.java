package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Comentario.ComentarioRequestDTO;
import synapseforge.crud.DTO.Comentario.ComentarioResponseDTO;
import synapseforge.crud.infrastructure.entity.Role;
import synapseforge.crud.service.ComentarioService;

import java.util.List;

@Tag(name = "Comentários", description = "Comentários da equipe e do cliente dentro de um pedido.")
@RestController
@RequestMapping("/pedidos/{pedidoId}/comentarios")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;

    @Operation(summary = "Comentar no pedido", description = "Adiciona um comentário ao pedido. Só quem tem acesso ao pedido pode comentar.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ComentarioResponseDTO criar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String pedidoId,
            @Valid @RequestBody ComentarioRequestDTO dto,
            Authentication auth
    ) {
        String usuarioId = (String) auth.getPrincipal();
        Role role = getRole(auth);

        return comentarioService.criar(
                pedidoId,
                dto,
                usuarioId,
                role
        );
    }

    @Operation(summary = "Listar comentários", description = "Lista os comentários do pedido, do mais antigo ao mais recente.")
    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public List<ComentarioResponseDTO> listar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String pedidoId,
            Authentication auth
    ) {
        String usuarioId = (String) auth.getPrincipal();
        Role role = getRole(auth);

        return comentarioService.listar(
                pedidoId,
                usuarioId,
                role
        );
    }

    @Operation(summary = "Excluir comentário", description = "Remove um comentário. Só o autor pode excluir.")
    @DeleteMapping("/{comentarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TECNICO','GERENTE')")
    public void deletar(
            @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String pedidoId,
            @Parameter(description = "ID do comentário", example = "6704a1c2e4b0f81a2c3d4e0a") @PathVariable String comentarioId,
            Authentication auth
    ) {
        String usuarioId = (String) auth.getPrincipal();

        Role role = getRole(auth);

        comentarioService.deletar(
                pedidoId,
                comentarioId,
                usuarioId,
                role
        );
    }

    @Operation(summary = "Editar comentário", description = "Altera o texto de um comentário. Só o autor pode editar.")
    @PutMapping("/{comentarioId}")
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ComentarioResponseDTO editar (
        @Parameter(description = "ID do pedido", example = "6704a1c2e4b0f81a2c3d4e01") @PathVariable String pedidoId,
        @Parameter(description = "ID do comentário", example = "6704a1c2e4b0f81a2c3d4e0a") @PathVariable String comentarioId,
        @Valid @RequestBody ComentarioRequestDTO dto,
        Authentication auth
    ) {
        String usuarioId = (String) auth.getPrincipal();
        Role role = getRole(auth);

        return comentarioService.editar(
                pedidoId,
                comentarioId,
                dto,
                usuarioId,
                role
        );

    }

    private Role getRole(Authentication auth) {
        String authority = auth.getAuthorities()
                .iterator()
                .next()
                .getAuthority();

        if (authority.startsWith("ROLE_")) {
            authority = authority.substring(5);
        }

        return Role.valueOf(authority);
    }
}