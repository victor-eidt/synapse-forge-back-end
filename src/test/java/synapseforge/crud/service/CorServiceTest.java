package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Cor.CorRequestDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Acabamento;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.repository.CorRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CorServiceTest {

    @Mock
    private CorRepository repository;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private CorService service;

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
        CorRequestDTO dto = new CorRequestDTO();
        dto.setNome("Azul");
        dto.setFornecedor("Fornecedor X");
        dto.setCodigo("ABC");
        dto.setHex("#0000FF");
        dto.setAcabamento(Acabamento.METALICO);
        dto.setEstoqueMl(100);
        dto.setEstoqueMinimoMl(10);
        dto.setCustoMl(2.5);

        Cor cor = service.toEntity(dto, "user-1");

        assertEquals("user-1", cor.getUsuarioId());
        assertEquals("Azul", cor.getNome());
        assertEquals("#0000FF", cor.getHex());
        assertEquals(Acabamento.METALICO, cor.getAcabamento());
    }

    @Test
    void criarDeveSalvarNaEquipeDoUsuarioComTimestamps() {
        naEquipe("user-1", "eq-1");
        Cor cor = new Cor();
        cor.setNome("Azul");
        cor.setUsuarioId("user-1");
        when(repository.save(any(Cor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cor result = service.criar(cor);

        assertEquals("eq-1", result.getEquipeId());
        assertNotNull(result.getCriadoEm());
        assertNotNull(result.getAtualizadoEm());
        verify(repository).save(cor);
    }

    @Test
    void criarSemEquipeDeveSerRecusado() {
        semEquipe("user-1");
        Cor cor = new Cor();
        cor.setUsuarioId("user-1");

        assertThrows(SemEquipeException.class, () -> service.criar(cor));
        verify(repository, never()).save(any());
    }

    @Test
    void listarDeveRetornarCoresDaEquipe() {
        naEquipe("user-1", "eq-1");
        Cor cor = new Cor();
        cor.setNome("Azul");
        when(repository.findByEquipeId("eq-1")).thenReturn(List.of(cor));

        List<Cor> result = service.listar("user-1");

        assertEquals(1, result.size());
    }

    @Test
    void listarSemEquipeDeveRetornarVazio() {
        semEquipe("user-1");

        assertTrue(service.listar("user-1").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void colegaDaEquipeEnxergaCorCadastradaPorOutroIntegrante() {
        naEquipe("user-2", "eq-1");
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");
        cor.setEquipeId("eq-1");
        when(repository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor));

        Optional<Cor> result = service.buscarPorId("c-1", "user-2");

        assertTrue(result.isPresent());
        assertEquals("c-1", result.get().getId());
    }

    @Test
    void corDeOutraEquipeNaoEEncontrada() {
        naEquipe("user-9", "eq-2");
        // a cor c-1 existe na eq-1, mas a busca é sempre feita na equipe de quem pede
        when(repository.findByIdAndEquipeId(anyString(), eq("eq-2"))).thenReturn(Optional.empty());

        assertTrue(service.buscarPorId("c-1", "user-9").isEmpty());
        RuntimeException aoAtualizar = assertThrows(RuntimeException.class,
                () -> service.atualizar("c-1", "user-9", new Cor()));
        RuntimeException aoDeletar = assertThrows(RuntimeException.class,
                () -> service.deletar("c-1", "user-9"));

        assertEquals("Cor não encontrada", aoAtualizar.getMessage());
        assertEquals("Cor não encontrada", aoDeletar.getMessage());
        verify(repository, never()).findById(any());
        verify(repository, never()).save(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void atualizarDeveSalvarMudancas() {
        naEquipe("user-1", "eq-1");
        Cor existente = new Cor();
        existente.setId("c-1");
        existente.setUsuarioId("user-1");
        existente.setEquipeId("eq-1");
        existente.setNome("Azul");
        existente.setFornecedor("Fornecedor A");
        existente.setHex("#0000FF");
        existente.setAcabamento(Acabamento.METALICO);
        existente.setEstoqueMl(100);
        existente.setEstoqueMinimoMl(10);
        existente.setCustoMl(1.5);

        Cor dados = new Cor();
        dados.setNome("Verde");
        dados.setFornecedor("Fornecedor B");
        dados.setHex("#00FF00");
        dados.setAcabamento(Acabamento.BRILHANTE);
        dados.setEstoqueMl(200);
        dados.setEstoqueMinimoMl(20);
        dados.setCustoMl(2.5);

        when(repository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        Cor result = service.atualizar("c-1", "user-1", dados);

        assertEquals("Verde", result.getNome());
        assertEquals("#00FF00", result.getHex());
        assertEquals("eq-1", result.getEquipeId());
        assertNotNull(result.getAtualizadoEm());
    }

    @Test
    void deletarDeveValidarEquipeAntesDeExcluir() {
        naEquipe("user-1", "eq-1");
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setEquipeId("eq-1");
        when(repository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor));

        service.deletar("c-1", "user-1");

        verify(repository).deleteById("c-1");
    }

    @Test
    void escritaSemEquipeDeveSerRecusada() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.atualizar("c-1", "user-1", new Cor()));
        assertThrows(SemEquipeException.class, () -> service.deletar("c-1", "user-1"));
        verifyNoInteractions(repository);
    }
}
