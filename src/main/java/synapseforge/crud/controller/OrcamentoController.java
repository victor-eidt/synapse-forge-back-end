package synapseforge.crud.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.service.OrcamentoService;

import java.util.List;

@RestController
@RequestMapping("/orcamentos")
@RequiredArgsConstructor
public class OrcamentoController {

    private final OrcamentoService service;

    @PostMapping("/calcular")
    public OrcamentoResponseDTO calcular(@RequestBody @Valid CalcularOrcamentoRequestDTO dto) {
        return service.calcular(dto);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrcamentoResponseDTO salvar(@RequestBody @Valid CalcularOrcamentoRequestDTO dto) {
        return service.salvar(dto);
    }

    @GetMapping
    public List<OrcamentoResponseDTO> listar() {
        return service.listar();
    }

    @GetMapping("/{id}")
    public OrcamentoResponseDTO buscarPorId(@PathVariable String id) {
        return service.buscarPorId(id);
    }
}
