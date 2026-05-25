package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
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
    public MaterialResponseDTO criar(@RequestBody @Valid MaterialRequestDTO dto) {
        return service.criar(dto);
    }

    @GetMapping
    public List<MaterialResponseDTO> listarAtivos() {
        return service.listarAtivos();
    }

    @GetMapping("/{id}")
    public MaterialResponseDTO buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }

    @PutMapping("/{id}")
    public MaterialResponseDTO atualizar(@PathVariable String id, @RequestBody @Valid MaterialRequestDTO dto) {
        return service.atualizar(id, dto);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@PathVariable String id) {
        service.inativar(id);
    }
}
