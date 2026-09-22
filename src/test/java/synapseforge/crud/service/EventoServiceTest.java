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
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Evento;
import synapseforge.crud.infrastructure.repository.EventoRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class EventoServiceTest {

    @Mock
    private EventoRepository repository;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private EventoService service;

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

    private void semEquipe(String usuarioId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenThrow(new SemEquipeException());
    }

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
        assertNull(evento.getEquipeId());
    }

    @Test
    void criarDeveSalvarEventoNaEquipeDoUsuario() {
        naEquipe("u-1", "eq-1");
        Evento evento = new Evento();
        evento.setNome("Workshop");
        evento.setUserId("u-1");
        when(repository.save(any(Evento.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Evento result = service.criar(evento, "u-1");

        assertEquals("Workshop", result.getNome());
        assertEquals("eq-1", result.getEquipeId());
        verify(repository).save(evento);
    }

    @Test
    void criarSemEquipeDeveSerRecusado() {
        semEquipe("u-1");

        assertThrows(SemEquipeException.class, () -> service.criar(new Evento(), "u-1"));
        assertThrows(SemEquipeException.class, () -> service.criarVarios(List.of(new Evento()), "u-1"));
        verifyNoInteractions(repository);
    }

    @Test
    void listarDeveRetornarEventosDaEquipe() {
        naEquipe("u-1", "eq-1");
        Evento evento = new Evento();
        evento.setId("e-1");
        evento.setNome("Workshop");
        when(repository.findByEquipeId("eq-1")).thenReturn(List.of(evento));

        List<Evento> result = service.listar("u-1");

        assertEquals(1, result.size());
        assertEquals("Workshop", result.get(0).getNome());
        verify(repository, never()).findAll();
    }

    @Test
    void listarSemEquipeDeveRetornarVazio() {
        semEquipe("u-1");

        assertTrue(service.listar("u-1").isEmpty());
        assertTrue(service.buscarPorUserIdAndMesAno("u-1", "u-1", "08", "2026").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void buscarPorIdDeveRetornarOpcional() {
        naEquipe("u-1", "eq-1");
        Evento evento = new Evento();
        evento.setId("e-1");
        when(repository.findByIdAndEquipeId("e-1", "eq-1")).thenReturn(Optional.of(evento));

        Optional<Evento> result = service.buscarPorId("e-1", "u-1");

        assertTrue(result.isPresent());
        assertEquals("e-1", result.get().getId());
    }

    @Test
    void buscarPorUserIdAndMesAnoDeveFiltrarPorPadraoDentroDaEquipe() {
        naEquipe("u-2", "eq-1");
        Evento evento = new Evento();
        evento.setUserId("u-1");
        evento.setNome("Workshop");
        when(repository.findByEquipeIdAndUserIdOrParticipanteAndMesAno(eq("eq-1"), eq("u-1"), anyString()))
                .thenReturn(List.of(evento));

        List<Evento> result = service.buscarPorUserIdAndMesAno("u-2", "u-1", "08", "2026");

        assertEquals(1, result.size());
    }

    @Test
    void atualizarDeveSalvarMudancas() {
        naEquipe("u-1", "eq-1");
        Evento atual = new Evento();
        atual.setId("e-1");
        atual.setEquipeId("eq-1");
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

        when(repository.findByIdAndEquipeId("e-1", "eq-1")).thenReturn(Optional.of(atual));
        when(repository.save(atual)).thenReturn(atual);

        Evento result = service.atualizar("e-1", dados, "u-1");

        assertEquals("Workshop Atualizado", result.getNome());
        assertEquals("2026-08-24", result.getData());
        assertEquals("eq-1", result.getEquipeId());
    }

    @Test
    void eventoDeOutraEquipeNaoEEncontrado() {
        naEquipe("u-9", "eq-2");

        assertTrue(service.buscarPorId("e-1", "u-9").isEmpty());
        RuntimeException aoAtualizar = assertThrows(RuntimeException.class,
                () -> service.atualizar("e-1", new Evento(), "u-9"));
        RuntimeException aoDeletar = assertThrows(RuntimeException.class,
                () -> service.deletar("e-1", "u-9"));

        assertEquals("Evento não encontrado", aoAtualizar.getMessage());
        assertEquals("Evento não encontrado", aoDeletar.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deletarDeveChamarDeleteByIdDepoisDeConferirAEquipe() {
        naEquipe("u-1", "eq-1");
        Evento evento = new Evento();
        evento.setId("e-1");
        when(repository.findByIdAndEquipeId("e-1", "eq-1")).thenReturn(Optional.of(evento));

        service.deletar("e-1", "u-1");

        verify(repository).deleteById("e-1");
    }

    @Test
    void criarVariosDeveSalvarListaNaEquipe() {
        naEquipe("u-1", "eq-1");
        Evento evento = new Evento();
        evento.setNome("Workshop");
        when(repository.saveAll(anyList())).thenReturn(List.of(evento));

        List<Evento> result = service.criarVarios(List.of(evento), "u-1");

        assertEquals(1, result.size());
        assertEquals("eq-1", evento.getEquipeId());
        verify(repository).saveAll(anyList());
    }
}
