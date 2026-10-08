package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Cor.CorRequestDTO;
import synapseforge.crud.DTO.Cor.CorResponseDTO;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.service.CorService;

import java.util.List;

@Tag(name = "Cores", description = "Paleta de tintas da equipe, com estoque em mL e custo por mL.")
@RestController
@RequestMapping("/cores")
public class CorController {

    @Autowired
    private CorService service;

    @Operation(summary = "Cadastrar cor", description = "Adiciona uma tinta à paleta da equipe.")
    @PostMapping
    public CorResponseDTO criar(@RequestBody @Valid CorRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Cor cor = service.toEntity(dto, usuarioId);
        Cor salva = service.criar(cor);
        return service.toResponseDTO(salva);
    }

    @Operation(summary = "Listar cores", description = "Lista as tintas da equipe.")
    @GetMapping
    public List<CorResponseDTO> listar(Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        return service.listar(usuarioId).stream().map(service::toResponseDTO).toList();
    }

    @Operation(summary = "Buscar cor", description = "Retorna uma tinta pelo ID.")
    @GetMapping("/{id}")
    public CorResponseDTO buscar(@Parameter(description = "ID da cor", example = "6704a1c2e4b0f81a2c3d4e04") @PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Cor cor = service.buscarPorId(id, usuarioId)
                .orElseThrow(() -> new RuntimeException("Cor não encontrada"));
        return service.toResponseDTO(cor);
    }

    @Operation(summary = "Editar cor", description = "Atualiza os dados da tinta.")
    @PutMapping("/{id}")
    public CorResponseDTO atualizar(@Parameter(description = "ID da cor", example = "6704a1c2e4b0f81a2c3d4e04") @PathVariable String id, @RequestBody @Valid CorRequestDTO dto, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        Cor corAtualizada = service.toEntity(dto, usuarioId);
        Cor salva = service.atualizar(id, usuarioId, corAtualizada);
        return service.toResponseDTO(salva);
    }

    @Operation(summary = "Excluir cor", description = "Remove a tinta da paleta.")
    @DeleteMapping("/{id}")
    public void deletar(@Parameter(description = "ID da cor", example = "6704a1c2e4b0f81a2c3d4e04") @PathVariable String id, Authentication auth) {
        String usuarioId = (String) auth.getPrincipal();
        service.deletar(id, usuarioId);
    }
}
