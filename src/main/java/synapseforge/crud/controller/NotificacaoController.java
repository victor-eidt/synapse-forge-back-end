package synapseforge.crud.controller;

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
@RestController
@RequestMapping("/notificacoes")
@RequiredArgsConstructor
public class NotificacaoController {

    private final NotificacaoService service;

    @GetMapping
    public List<NotificacaoResponseDTO> listar(
            @RequestParam(defaultValue = "false") boolean naoLidas,
            Authentication auth
    ) {
        return service.listar((String) auth.getPrincipal(), naoLidas)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @PatchMapping("/{id}/lida")
    public NotificacaoResponseDTO marcarComoLida(
            @PathVariable String id,
            Authentication auth
    ) {
        return service.toResponseDTO(
                service.marcarComoLida(id, (String) auth.getPrincipal())
        );
    }

    @PatchMapping("/lidas")
    public ResponseEntity<Void> marcarTodasComoLidas(Authentication auth) {
        service.marcarTodasComoLidas((String) auth.getPrincipal());
        return ResponseEntity.noContent().build();
    }
}
