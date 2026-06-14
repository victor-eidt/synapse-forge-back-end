package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import synapseforge.crud.DTO.OrdemPintura.AtualizarEtapaOrdemPinturaDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaRequestDTO;
import synapseforge.crud.DTO.OrdemPintura.OrdemPinturaResponseDTO;
import synapseforge.crud.service.OrdemPinturaService;

import java.util.List;

@RestController
@RequestMapping("/ordens-pintura")
@RequiredArgsConstructor
public class OrdemPinturaController {

    private final OrdemPinturaService service;

    @GetMapping
    public List<OrdemPinturaResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal());
    }

    @PostMapping
    public OrdemPinturaResponseDTO criar(
            @RequestBody @Valid OrdemPinturaRequestDTO dto,
            Authentication auth
    ) {
        return service.criar(dto, (String) auth.getPrincipal());
    }

    @PatchMapping("/{id}/etapa")
    public OrdemPinturaResponseDTO atualizarEtapa(
            @PathVariable String id,
            @RequestBody @Valid AtualizarEtapaOrdemPinturaDTO dto,
            Authentication auth
    ) {
        return service.atualizarEtapa(id, dto.getEtapa(), (String) auth.getPrincipal());
    }

    @PutMapping("/{id}")
    public OrdemPinturaResponseDTO atualizar(
            @PathVariable String id,
            @RequestBody @Valid OrdemPinturaRequestDTO dto,
            Authentication auth
    ) {
        return service.atualizar(id, dto, (String) auth.getPrincipal());
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id, Authentication auth) {
        service.deletar(id, (String) auth.getPrincipal());
    }
}
