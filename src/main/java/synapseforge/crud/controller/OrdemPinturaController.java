package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

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
import synapseforge.crud.DTO.OrdemPintura.TecnicoResumoDTO;
import synapseforge.crud.service.OrdemPinturaService;

import java.util.List;

@Tag(name = "Ordens de pintura", description = "Quadro kanban de pintura: ordens por cor, responsável, prioridade e prazo.")
@RestController
@RequestMapping("/ordens-pintura")
@RequiredArgsConstructor
public class OrdemPinturaController {

    private final OrdemPinturaService service;

    @Operation(summary = "Listar ordens", description = "Ordens de pintura da equipe.")
    @GetMapping
    public List<OrdemPinturaResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal());
    }

    // Opções do select de técnico: técnicos ativos e o gerente da equipe de quem pede
    @Operation(summary = "Responsáveis disponíveis", description = "Técnicos ativos e o gerente da equipe, em ordem alfabética: opções válidas para tecnicoId.")
    @GetMapping("/tecnicos")
    public List<TecnicoResumoDTO> listarTecnicos(Authentication auth) {
        return service.listarTecnicos((String) auth.getPrincipal());
    }

    @Operation(summary = "Criar ordem", description = "Cria a ordem na etapa AGUARDANDO. O responsável recebe aviso no sino e por e-mail.")
    @PostMapping
    public OrdemPinturaResponseDTO criar(
            @RequestBody @Valid OrdemPinturaRequestDTO dto,
            Authentication auth
    ) {
        return service.criar(dto, (String) auth.getPrincipal());
    }

    @Operation(summary = "Mover ordem no quadro", description = "Altera a etapa da ordem (AGUARDANDO, MISTURANDO_TINTA, EM_PINTURA, SECANDO, FINALIZADO, RETRABALHO).")
    @PatchMapping("/{id}/etapa")
    public OrdemPinturaResponseDTO atualizarEtapa(
            @Parameter(description = "ID da ordem de pintura", example = "6704a1c2e4b0f81a2c3d4e08") @PathVariable String id,
            @RequestBody @Valid AtualizarEtapaOrdemPinturaDTO dto,
            Authentication auth
    ) {
        return service.atualizarEtapa(id, dto.getEtapa(), (String) auth.getPrincipal());
    }

    @Operation(summary = "Editar ordem", description = "Atualiza a ordem. Se o responsável mudar, o novo responsável é avisado.")
    @PutMapping("/{id}")
    public OrdemPinturaResponseDTO atualizar(
            @Parameter(description = "ID da ordem de pintura", example = "6704a1c2e4b0f81a2c3d4e08") @PathVariable String id,
            @RequestBody @Valid OrdemPinturaRequestDTO dto,
            Authentication auth
    ) {
        return service.atualizar(id, dto, (String) auth.getPrincipal());
    }

    @Operation(summary = "Excluir ordem", description = "Remove a ordem de pintura.")
    @DeleteMapping("/{id}")
    public void deletar(@Parameter(description = "ID da ordem de pintura", example = "6704a1c2e4b0f81a2c3d4e08") @PathVariable String id, Authentication auth) {
        service.deletar(id, (String) auth.getPrincipal());
    }
}
