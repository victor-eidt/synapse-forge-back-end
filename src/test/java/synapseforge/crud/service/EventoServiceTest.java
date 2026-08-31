package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Evento.EventoRequestDTO;
import synapseforge.crud.DTO.Evento.EventoResponseDTO;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.infrastructure.repository.EventoRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository repository;

    @InjectMocks
    private EventoService service;

    @Test
    void toEntityDeveMapearDados() {
        EventoRequestDTO dto = new EventoRequestDTO();
        dto.setUserId("u-1");
        dto.setNome("Workshop");
        dto.setData("2026-08-23");
        dto.setDescricao("Descrição");
        dto.setHorarioInicio("09:00");
        dto.setHorarioFim("10:00");
        dto.setParticipantes(List.of("Ana", "Bia"));

        Evento evento = service.toEntity(dto);

        assertEquals("u-1", evento.getUserId());
        assertEquals("Workshop", evento.getNome());
        assertEquals("2026-08-23", evento.getData());
        assertEquals("Ana", evento.getParticipantes().get(0));
    }

    @Test
    void criarDeveSalvarEvento() {
        Evento evento = new Evento();
        evento.setNome("Workshop");
        evento.setUserId("u-1");
        when(repository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Evento result = service.criar(evento);

        assertEquals("Workshop", result.getNome());
        verify(repository).save(evento);
    }

    @Test
    void listarDeveRetornarEventos() {
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        when(repository.findAll()).thenReturn(List.of(evento));

        List<Evento> result = service.listar();

        assertEquals(1, result.size());
        assertEquals("Workshop", result.get(0).getNome());
    }

    @Test
    void buscarPorIdDeveRetornarOpcional() {
        Evento evento = new Evento();
        evento.setId("e-1");
        when(repository.findById("e-1")).thenReturn(Optional.of(evento));

        Optional<Evento> result = service.buscarPorId("e-1");

        assertTrue(result.isPresent());
        assertEquals("e-1", result.get().getId());
    }

    @Test
    void buscarPorUserIdAndMesAnoDeveFiltrarPorPadrao() {
        Evento evento = new Evento();
        evento.setUserId("u-1");
        evento.setNome("Workshop");
        when(repository.findByUserIdOrParticipanteAndMesAno(eq("u-1"), anyString())).thenReturn(List.of(evento));

        List<Evento> result = service.buscarPorUserIdAndMesAno("u-1", "08", "2026");

        assertEquals(1, result.size());
    }

    @Test
    void atualizarDeveSalvarMudancas() {
        Evento atual = new Evento();
        atual.setId("e-1");
        atual.setNome("Workshop");
        atual.setData("2026-08-23");
        atual.setDescricao("Desc");
        atual.setHorarioInicio("09:00");
        atual.setHorarioFim("10:00");

        Evento dados = new Evento();
        dados.setNome("Workshop Atualizado");
        dados.setData("2026-08-24");
        dados.setDescricao("DescAtual");
        dados.setHorarioInicio("11:00");
        dados.setHorarioFim("12:00");
        dados.setParticipantes(List.of("Ana"));

        when(repository.findById("e-1")).thenReturn(Optional.of(atual));
        when(repository.save(atual)).thenReturn(atual);

        Evento result = service.atualizar("e-1", dados);

        assertEquals("Workshop Atualizado", result.getNome());
        assertEquals("2026-08-24", result.getData());
    }

    @Test
    void deletarDeveChamarDeleteById() {
        service.deletar("e-1");
        verify(repository).deleteById("e-1");
    }

    @Test
    void criarVariosDeveSalvarLista() {
        Evento evento = new Evento();
        evento.setNome("Workshop");
        when(repository.saveAll(anyList())).thenReturn(List.of(evento));

        List<Evento> result = service.criarVarios(List.of(evento));

        assertEquals(1, result.size());
        verify(repository).saveAll(anyList());
    }
}
