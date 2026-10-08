package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Mistura.MisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.MisturaResponseDTO;
import synapseforge.crud.service.MisturaService;

import java.util.List;

@Tag(name = "Misturas", description = "Receitas de mistura de tintas, com cor resultante e custo estimado.")
@RestController
@RequestMapping("/misturas")
public class MisturaController {

    @Autowired
    private MisturaService service;

    @Operation(summary = "Criar mistura", description = "Salva uma receita; as proporções devem somar 100%.")
    @PostMapping
    public MisturaResponseDTO criar(@RequestBody @Valid MisturaRequestDTO dto, Authentication auth) {
        return service.criar(dto, (String) auth.getPrincipal());
    }

    @Operation(summary = "Listar misturas", description = "Lista as receitas da equipe.")
    @GetMapping
    public List<MisturaResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal());
    }

    @Operation(summary = "Buscar mistura", description = "Retorna uma receita pelo ID.")
    @GetMapping("/{id}")
    public MisturaResponseDTO buscar(@Parameter(description = "ID da mistura", example = "6704a1c2e4b0f81a2c3d4e0c") @PathVariable String id, Authentication auth) {
        return service.buscarPorId(id, (String) auth.getPrincipal());
    }

    @Operation(summary = "Editar mistura", description = "Atualiza a receita.")
    @PutMapping("/{id}")
    public MisturaResponseDTO atualizar(@Parameter(description = "ID da mistura", example = "6704a1c2e4b0f81a2c3d4e0c") @PathVariable String id, @RequestBody @Valid MisturaRequestDTO dto, Authentication auth) {
        return service.atualizar(id, (String) auth.getPrincipal(), dto);
    }

    @Operation(summary = "Excluir mistura", description = "Remove a receita.")
    @DeleteMapping("/{id}")
    public void deletar(@Parameter(description = "ID da mistura", example = "6704a1c2e4b0f81a2c3d4e0c") @PathVariable String id, Authentication auth) {
        service.deletar(id, (String) auth.getPrincipal());
    }
}
