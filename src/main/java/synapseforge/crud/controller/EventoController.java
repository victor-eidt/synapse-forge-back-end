package synapseforge.crud.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Evento.EventoRequestDTO;
import synapseforge.crud.DTO.Evento.EventoResponseDTO;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.service.EventoService;

import java.util.List;

@Tag(name = "Agenda", description = "Eventos da agenda da equipe (entregas, reuniões, retiradas).")
@RestController
@RequestMapping("/evento")
public class EventoController {

    @Autowired
    private EventoService service;

    @Operation(summary = "Criar evento", description = "Cadastra um evento na agenda da equipe.")
    @PostMapping("/registrar")
    public EventoResponseDTO criar(@RequestBody @Valid EventoRequestDTO dto, Authentication auth) {

        Evento evento = service.toEntity(dto);      // DTO → Entity
        Evento salvo = service.criar(evento, (String) auth.getPrincipal());       // salva na equipe do usuário
        return service.toResponseDTO(salvo);        // Entity → DTO
    }

    @Operation(summary = "Listar eventos", description = "Lista os eventos da equipe do usuário logado.")
    @GetMapping
    public List<EventoResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal())
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @Operation(summary = "Buscar evento", description = "Retorna um evento pelo ID.")
    @GetMapping("/{id}")
    public EventoResponseDTO buscar(@Parameter(description = "ID do evento", example = "6704a1c2e4b0f81a2c3d4e0d") @PathVariable String id, Authentication auth) {
        Evento evento = service.buscarPorId(id, (String) auth.getPrincipal())
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        return service.toResponseDTO(evento);
    }

    @Operation(summary = "Eventos do usuário no mês", description = "Eventos em que o usuário é dono ou participante, no mês/ano informado.")
    @GetMapping("/buscar-mes/{userId}")
    public List<EventoResponseDTO> buscarPorUsuarioMes(
            @Parameter(description = "ID do usuário dono da agenda", example = "6704a1c2e4b0f81a2c3d4e05") @PathVariable String userId,
            @Parameter(description = "Mês com dois dígitos", example = "10") @RequestParam String mes,
            @Parameter(description = "Ano com quatro dígitos", example = "2026") @RequestParam String ano,
            Authentication auth) {
        return service.buscarPorUserIdAndMesAno((String) auth.getPrincipal(), userId, mes, ano)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @Operation(summary = "Editar evento", description = "Atualiza os dados do evento.")
    @PutMapping("/{id}")
    public EventoResponseDTO atualizar(@Parameter(description = "ID do evento", example = "6704a1c2e4b0f81a2c3d4e0d") @PathVariable String id, @RequestBody EventoRequestDTO dto, Authentication auth) {

        Evento evento = service.toEntity(dto);
        Evento atualizado = service.atualizar(id, evento, (String) auth.getPrincipal());

        return service.toResponseDTO(atualizado);
    }

    @Operation(summary = "Excluir evento", description = "Remove o evento.")
    @DeleteMapping("/{id}")
    public void deletar(@Parameter(description = "ID do evento", example = "6704a1c2e4b0f81a2c3d4e0d") @PathVariable String id, Authentication auth) {
        service.deletar(id, (String) auth.getPrincipal());
    }

    @Operation(summary = "Criar eventos em lote", description = "Cadastra vários eventos de uma vez.")
    @PostMapping("/batch")
    public List<EventoResponseDTO> criarVarios(@RequestBody @Valid List<EventoRequestDTO> dtos, Authentication auth) {

        List<Evento> eventos = dtos.stream()
                .map(service::toEntity)
                .toList();

        List<Evento> salvos = service.criarVarios(eventos, (String) auth.getPrincipal());

        return salvos.stream()
                .map(service::toResponseDTO)
                .toList();
    }

}
