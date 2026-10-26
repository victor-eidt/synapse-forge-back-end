package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;

import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import synapseforge.crud.DTO.User.PreferenciasUsuarioDTO;
import synapseforge.crud.service.PreferenciasUsuarioService;

/**
 * Preferências do usuário logado (tema, idioma e dashboard). Sem id na rota: cada um lê e
 * altera só as próprias, qualquer que seja o papel.
 */
@Tag(name = "Usuários")
@RestController
@RequestMapping("/users/me/preferencias")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('CLIENTE', 'TECNICO', 'GERENTE', 'ADMIN')")
public class PreferenciasUsuarioController {

    private static final String EXEMPLO = """
            {
              "tema": "escuro",
              "idioma": "pt-BR",
              "dashboard": {
                "periodo": "90d",
                "widgets": [
                  { "id": "indicadores", "visivel": true },
                  { "id": "pedidosPorMes", "visivel": true },
                  { "id": "pedidosPorEtapa", "visivel": true },
                  { "id": "materiaisMaisUsados", "visivel": false },
                  { "id": "estoqueBaixo", "visivel": true }
                ]
              }
            }""";

    private final PreferenciasUsuarioService service;


    // =========================================================
    // BUSCAR MINHAS PREFERÊNCIAS
    // =========================================================

    @Operation(
            summary = "Minhas preferências",
            description = "Tema, idioma e dashboard salvos pelo usuário logado. Campo nunca salvo vem null (dashboard inclusive)."
    )
    @ApiResponse(responseCode = "200", description = "Preferências do usuário",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PreferenciasUsuarioDTO.class),
                    examples = {
                            @ExampleObject(name = "Salvas", value = EXEMPLO),
                            @ExampleObject(name = "Nunca salvou", value = "{\"tema\": null, \"idioma\": null, \"dashboard\": null}")
                    }))
    @GetMapping
    public PreferenciasUsuarioDTO buscar(
            Authentication auth
    ) {

        return service.buscar((String) auth.getPrincipal());
    }


    // =========================================================
    // ATUALIZAR MINHAS PREFERÊNCIAS
    // =========================================================

    @Operation(
            summary = "Salvar minhas preferências",
            description = "Campo ausente ou null mantém o valor atual (dá para mandar só o tema ou só o dashboard). "
                    + "O dashboard, quando enviado, substitui o dashboard inteiro: periodo e widgets (1 a 10, sem id repetido, "
                    + "visivel obrigatório) precisam vir. A ordem da lista é a ordem de exibição. Devolve as preferências salvas."
    )
    @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Preferências a alterar",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PreferenciasUsuarioDTO.class),
                    examples = {
                            @ExampleObject(name = "Só o tema", value = "{\"tema\": \"claro\"}"),
                            @ExampleObject(name = "Só o idioma", value = "{\"idioma\": \"en-US\"}"),
                            @ExampleObject(name = "Tudo", value = EXEMPLO)
                    }))
    @ApiResponse(responseCode = "200", description = "Preferências salvas",
            content = @Content(mediaType = "application/json",
                    schema = @Schema(implementation = PreferenciasUsuarioDTO.class),
                    examples = @ExampleObject(value = EXEMPLO)))
    @ApiResponse(responseCode = "400", description = "Valor fora da lista permitida",
            content = @Content(mediaType = "text/plain",
                    examples = @ExampleObject(value = "Tema inválido: use claro ou escuro")))
    @PutMapping
    public PreferenciasUsuarioDTO atualizar(
            @RequestBody PreferenciasUsuarioDTO dto,
            Authentication auth
    ) {

        return service.atualizar(
                (String) auth.getPrincipal(),
                dto
        );
    }
}
