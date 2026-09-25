package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Evento.EventoRequestDTO;
import synapseforge.crud.DTO.Evento.EventoResponseDTO;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.service.EventoService;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EventoControllerTest {

    @Mock
    private EventoService service;

    @InjectMocks
    private EventoController controller;

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("u-1");
        return auth;
    }

    @Test
    void criarDeveRetornarEvento() {
        EventoRequestDTO dto = new EventoRequestDTO();
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        EventoResponseDTO response = new EventoResponseDTO("e-1", "u-1", "Workshop", LocalDate.now().toString(), "Desc", "09:00", "10:00", List.of("Ana"));

        when(service.toEntity(dto)).thenReturn(evento);
        when(service.criar(evento, "u-1")).thenReturn(evento);
        when(service.toResponseDTO(evento)).thenReturn(response);

        assertEquals("Workshop", controller.criar(dto, auth()).getNome());
    }

    @Test
    void listarDeveRetornarLista() {
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        EventoResponseDTO response = new EventoResponseDTO("e-1", "u-1", "Workshop", LocalDate.now().toString(), "Desc", "09:00", "10:00", List.of("Ana"));

        when(service.listar("u-1")).thenReturn(List.of(evento));
        when(service.toResponseDTO(evento)).thenReturn(response);

        assertEquals(1, controller.listar(auth()).size());
    }

    @Test
    void buscarDeveRetornarEvento() {
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        EventoResponseDTO response = new EventoResponseDTO("e-1", "u-1", "Workshop", LocalDate.now().toString(), "Desc", "09:00", "10:00", List.of("Ana"));

        when(service.buscarPorId("e-1", "u-1")).thenReturn(Optional.of(evento));
        when(service.toResponseDTO(evento)).thenReturn(response);

        assertEquals("Workshop", controller.buscar("e-1", auth()).getNome());
    }

    @Test
    void buscarPorUsuarioMesDeveRetornarLista() {
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        EventoResponseDTO response = new EventoResponseDTO("e-1", "u-1", "Workshop", LocalDate.now().toString(), "Desc", "09:00", "10:00", List.of("Ana"));

        when(service.buscarPorUserIdAndMesAno("u-1", "u-1", "09", "2026")).thenReturn(List.of(evento));
        when(service.toResponseDTO(evento)).thenReturn(response);

        assertEquals(1, controller.buscarPorUsuarioMes("u-1", "09", "2026", auth()).size());
    }

    @Test
    void atualizarDeveRetornarEventoAtualizado() {
        EventoRequestDTO dto = new EventoRequestDTO();
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop Atualizado");
        EventoResponseDTO response = new EventoResponseDTO("e-1", "u-1", "Workshop Atualizado", LocalDate.now().toString(), "Desc", "09:00", "10:00", List.of("Ana"));

        when(service.toEntity(dto)).thenReturn(evento);
        when(service.atualizar("e-1", evento, "u-1")).thenReturn(evento);
        when(service.toResponseDTO(evento)).thenReturn(response);

        assertEquals("Workshop Atualizado", controller.atualizar("e-1", dto, auth()).getNome());
    }

    @Test
    void deletarDeveChamarService() {
        controller.deletar("e-1", auth());
        verify(service).deletar("e-1", "u-1");
    }

    @Test
    void criarVariosDeveRetornarLista() {
        EventoRequestDTO dto = new EventoRequestDTO();
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        EventoResponseDTO response = new EventoResponseDTO("e-1", "u-1", "Workshop", LocalDate.now().toString(), "Desc", "09:00", "10:00", List.of("Ana"));

        when(service.toEntity(dto)).thenReturn(evento);
        when(service.criarVarios(List.of(evento), "u-1")).thenReturn(List.of(evento));
        when(service.toResponseDTO(evento)).thenReturn(response);

        assertEquals(1, controller.criarVarios(List.of(dto), auth()).size());
    }
}
