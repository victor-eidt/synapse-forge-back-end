package synapseforge.crud.controller;

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

@RestController
@RequestMapping("/pedidos/{pedidoId}/comentarios")
@RequiredArgsConstructor
public class ComentarioController {

    private final ComentarioService comentarioService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ComentarioResponseDTO criar(
            @PathVariable String pedidoId,
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

    @GetMapping
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public List<ComentarioResponseDTO> listar(
            @PathVariable String pedidoId,
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

    @DeleteMapping("/{comentarioId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasAnyRole('TECNICO','GERENTE')")
    public void deletar(
            @PathVariable String pedidoId,
            @PathVariable String comentarioId,
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

    @PutMapping("/{comentarioId}")
    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE')")
    public ComentarioResponseDTO editar (
        @PathVariable String pedidoId,
        @PathVariable String comentarioId,
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