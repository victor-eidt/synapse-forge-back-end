package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Notificacao.NotificacaoResponseDTO;
import synapseforge.crud.service.NotificacaoService;

import java.util.List;

/**
 * Notificações do usuário logado (sino da Sidebar). Qualquer papel autenticado
 * acessa, e sempre só as próprias: o destinatário sai do token, nunca do request.
 */
@Tag(name = "Notificações", description = "Avisos do sino do usuário logado (mudança de etapa do pedido, ordem de pintura atribuída).")
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService service;

    @Operation(summary = "Listar meus avisos", description = "Avisos do usuário logado, do mais recente ao mais antigo (até 50).")
    @GetMapping
    public List<NotificacaoResponseDTO> listar(
            @Parameter(description = "true para trazer só os avisos não lidos", example = "true") @RequestParam(defaultValue = "false") boolean naoLidas,
            Authentication auth
    ) {
        return service.listar((String) auth.getPrincipal(), naoLidas)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @Operation(summary = "Marcar aviso como lido", description = "Marca um aviso como lido; só o dono do aviso consegue.")
    @PatchMapping("/{id}/lida")
    public NotificacaoResponseDTO marcarComoLida(
            @Parameter(description = "ID da notificação", example = "6704a1c2e4b0f81a2c3d4e09") @PathVariable String id,
            Authentication auth
    ) {
        return service.toResponseDTO(
                service.marcarComoLida(id, (String) auth.getPrincipal())
        );
    }

    @Operation(summary = "Marcar todos como lidos", description = "Marca todos os avisos do usuário logado como lidos.")
    @PatchMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(Authentication auth) {
        service.marcarTodasComoLidas((String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
