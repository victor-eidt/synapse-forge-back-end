package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Cor.CorRequestDTO;
import synapseforge.crud.DTO.Cor.CorResponseDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.service.CorService;

import java.util.List;

@RestController
@RequestMapping("/cores")
public class CorController {

    @Autowired
    private CorService service;

    @PostMapping
    public CorResponseDTO criar(@RequestBody @Valid CorRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Cor cor = service.toEntity(dto, usuarioId);
        Cor salva = service.criar(cor);
        return service.toResponseDTO(salva);
    }

    @GetMapping
    public List<CorResponseDTO> listar(Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        return service.listar(usuarioId).stream().map(service::toResponseDTO).toList();
    }

    @GetMapping("/{id}")
    public CorResponseDTO buscar(@PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Cor cor = service.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new RuntimeException("Cor não encontrada"));
        return service.toResponseDTO(cor);
    }

    @PutMapping("/{id}")
    public CorResponseDTO atualizar(@PathVariable String id, @RequestBody @Valid CorRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Cor corAtualizada = service.toEntity(dto, usuarioId);
        Cor salva = service.atualizar(id, usuarioId, corAtualizada);
        return service.toResponseDTO(salva);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        service.deletar(id, usuarioId);
    }
}
