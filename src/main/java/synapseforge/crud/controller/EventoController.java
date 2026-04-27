package synapseforge.crud.controller;

import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
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
    public EventoResponseDTO criar(@RequestBody @Valid EventoRequestDTO dto) {

        Evento evento = service.toEntity(dto);      // DTO → Entity
        Evento salvo = service.criar(evento);       // salva
        return service.toResponseDTO(salvo);        // Entity → DTO
    }

    @GetMapping
    public List<EventoResponseDTO> listar() {
        return service.listar()
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @GetMapping("/{id}")
    public EventoResponseDTO buscar(@PathVariable String id) {
        Evento evento = service.buscarPorId(id)
                .orElseThrow(() -> new RuntimeException("Evento não encontrado"));

        return service.toResponseDTO(evento);
    }

    @GetMapping("/buscar-mes/{userId}")
    public List<EventoResponseDTO> buscarPorUsuarioMes(
            @PathVariable String userId,
            @RequestParam String mes,
            @RequestParam String ano) {
        return service.buscarPorUserIdAndMesAno(userId, mes, ano)
                .stream()
                .map(service::toResponseDTO)
                .toList();
    }

    @PutMapping("/{id}")
    public EventoResponseDTO atualizar(@PathVariable String id, @RequestBody EventoRequestDTO dto) {

        Evento evento = service.toEntity(dto);
        Evento atualizado = service.atualizar(id, evento);

        return service.toResponseDTO(atualizado);
    }

    @DeleteMapping("/{id}")
    public void deletar(@PathVariable String id) {
        service.deletar(id);
    }

    @PostMapping("/batch")
    public List<EventoResponseDTO> criarVarios(@RequestBody @Valid List<EventoRequestDTO> dtos) {

        List<Evento> eventos = dtos.stream()
                .map(service::toEntity)
                .toList();

        List<Evento> salvos = service.criarVarios(eventos);

        return salvos.stream()
                .map(service::toResponseDTO)
                .toList();
    }

}
