package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import synapseforge.crud.DTO.FormaEnvio.FormaEnvioRequestDTO;
import synapseforge.crud.DTO.FormaEnvio.FormaEnvioResponseDTO;
import synapseforge.crud.service.FormaEnvioService;

import java.util.List;

@Tag(name = "Formas de envio", description = "Formas de envio dos pedidos (Correios, transportadora, retirada) e o modelo da URL de rastreio de cada uma.")
@RestController
@RequestMapping("/formas-envio")
@RequiredArgsConstructor
public class FormaEnvioController {

    private final FormaEnvioService service;

    @Operation(summary = "Listar formas de envio", description = "Formas ativas: as padrão do sistema e as cadastradas pela equipe do usuário.")
    @GetMapping
    public List<FormaEnvioResponseDTO> listarAtivas(Authentication auth) {
        return service.listarAtivas((String) auth.getPrincipal());
    }

    @PreAuthorize("hasAnyRole('TECNICO', 'GERENTE', 'ADMIN')")
    @Operation(summary = "Cadastrar forma de envio", description = "Adiciona uma forma de envio à equipe do usuário logado.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public FormaEnvioResponseDTO criar(@RequestBody @Valid FormaEnvioRequestDTO dto, Authentication auth) {
        return service.criar(dto, (String) auth.getPrincipal());
    }
}
