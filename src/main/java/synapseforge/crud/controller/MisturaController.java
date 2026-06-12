package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Mistura.MisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.MisturaResponseDTO;
import synapseforge.crud.service.MisturaService;

import java.util.List;

@RestController
@RequestMapping("/misturas")
public class MisturaController {

    @Autowired
    private MisturaService service;

    @PostMapping
    public MisturaResponseDTO criar(@RequestBody @Valid MisturaRequestDTO dto, Authentication auth) {
        return service.criar(dto, (String) auth.getPrincipal());
    }

    @GetMapping
    public List<MisturaResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal());
    }

    @GetMapping("/{id}")
    public MisturaResponseDTO buscar(@PathVariable String id, Authentication auth) {
        return service.buscarPorId(id, (String) auth.getPrincipal());
    }

    @PutMapping("/{id}")
    public MisturaResponseDTO atualizar(@PathVariable String id, @RequestBody @Valid MisturaRequestDTO dto, Authentication auth) {
        return service.atualizar(id, (String) auth.getPrincipal(), dto);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id, Authentication auth) {
        service.deletar(id, (String) auth.getPrincipal());
    }
}
