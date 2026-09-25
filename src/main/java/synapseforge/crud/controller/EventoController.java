package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import synapseforge.crud.DTO.Evento.EventoRequestDTO;
import synapseforge.crud.DTO.Evento.EventoResponseDTO;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.service.EventoService;

import java.util.List;

@RestController
@RequestMapping("/evento")
public class EventoController {

    @Autowired
    private EventoService service;

    @PostMapping("/registrar")
    public EventoResponseDTO criar(@RequestBody @Valid EventoRequestDTO dto, Authentication auth) {

        Evento evento = service.toEntity(dto);      // DTO → Entity
        Evento salvo = service.criar(evento, (String) auth.getPrincipal());       // salva na equipe do usuário
        return service.toResponseDTO(salvo);        // Entity → DTO
    }

    @GetMapping
    public List<EventoResponseDTO> listar(Authentication auth) {
        return service.listar((String) auth.getPrincipal())
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public EventoResponseDTO buscar(@PathVariable String id, Authentication auth) {
        Evento evento = service.buscarPorId(id, (String) auth.getPrincipal())
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        return service.toResponseDTO(evento);
    }

    @GetMapping("/buscar-mes/{userId}")
    public List<EventoResponseDTO> buscarPorUsuarioMes(
            @PathVariable String userId,
            @RequestParam String mes,
            @RequestParam String ano,
            Authentication auth) {
        return service.buscarPorUserIdAndMesAno((String) auth.getPrincipal(), userId, mes, ano)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @PutMapping("/{id}")
    public EventoResponseDTO atualizar(@PathVariable String id, @RequestBody EventoRequestDTO dto, Authentication auth) {

        Evento evento = service.toEntity(dto);
        Evento atualizado = service.atualizar(id, evento, (String) auth.getPrincipal());

        return service.toResponseDTO(atualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id, Authentication auth) {
        service.deletar(id, (String) auth.getPrincipal());
    }

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
