package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.service.MaterialService;

import java.util.List;

@RestController
@RequestMapping("/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService service;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponseDTO criar(@RequestBody @Valid MaterialRequestDTO dto, Authentication auth) {
        return service.criar(dto, (String) auth.getPrincipal());
    }

    @GetMapping
    public List<MaterialResponseDTO> listarAtivos(Authentication auth) {
        return service.listarAtivos((String) auth.getPrincipal());
    }

    @GetMapping("/{id}")
    public MaterialResponseDTO buscarPorId(@PathVariable String id, Authentication auth) {
        return service.buscarPorId(id, (String) auth.getPrincipal());
    }

    @PutMapping("/{id}")
    public MaterialResponseDTO atualizar(@PathVariable String id, @RequestBody @Valid MaterialRequestDTO dto,
                                         Authentication auth) {
        return service.atualizar(id, dto, (String) auth.getPrincipal());
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable String id, Authentication auth) {
        service.inativar(id, (String) auth.getPrincipal());
    }
}
