package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.service.MaterialService;

import java.util.List;

@Tag(name = "Materiais", description = "Materiais de impressão (filamentos e resinas), com densidade e preço por grama.")
@RestController
@RequestMapping("/materiais")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService service;

    @Operation(summary = "Cadastrar material", description = "Adiciona um material de impressão à equipe.")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MaterialResponseDTO criar(@RequestBody @Valid MaterialRequestDTO dto, Authentication auth) {
        return service.criar(dto, (String) auth.getPrincipal());
    }

    @Operation(summary = "Listar materiais ativos", description = "Materiais disponíveis para novos orçamentos, com o saldo em estoque.")
    @GetMapping
    public List<MaterialResponseDTO> listarAtivos(Authentication auth) {
        return service.listarAtivos((String) auth.getPrincipal());
    }

    @Operation(summary = "Buscar material", description = "Retorna um material pelo ID.")
    @GetMapping("/{id}")
    public MaterialResponseDTO buscarPorId(@Parameter(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03") @PathVariable String id, Authentication auth) {
        return service.buscarPorId(id, (String) auth.getPrincipal());
    }

    @Operation(summary = "Editar material", description = "Atualiza os dados do material.")
    @PutMapping("/{id}")
    public MaterialResponseDTO atualizar(@Parameter(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03") @PathVariable String id, @RequestBody @Valid MaterialRequestDTO dto,
                                         Authentication auth) {
        return service.atualizar(id, dto, (String) auth.getPrincipal());
    }

    @Operation(summary = "Inativar material", description = "Inativa o material (não é apagado, para manter o histórico dos pedidos).")
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void inativar(@Parameter(description = "ID do material", example = "6704a1c2e4b0f81a2c3d4e03") @PathVariable String id, Authentication auth) {
        service.inativar(id, (String) auth.getPrincipal());
    }
}
