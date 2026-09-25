package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Mistura.ItemMisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.MisturaRequestDTO;
import synapseforge.crud.DTO.Mistura.MisturaResponseDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.entity.Mistura;
import synapseforge.crud.infrastructure.repository.CorRepository;
import synapseforge.crud.infrastructure.repository.MisturaRepository;

import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MisturaServiceTest {

    @Mock
    private MisturaRepository repository;

    @Mock
    private CorRepository corRepository;

    @Mock
    private EquipeContexto equipeContexto;

    @InjectMocks
    private MisturaService service;

    private void naEquipe(String usuarioId, String equipeId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.of(equipeId));
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenReturn(equipeId);
    }

    private void semEquipe(String usuarioId) {
        lenient().when(equipeContexto.equipeDe(usuarioId)).thenReturn(Optional.empty());
        lenient().when(equipeContexto.equipeObrigatoria(usuarioId)).thenThrow(new SemEquipeException());
    }

    private Cor cor(String id, String hex, double custoMl) {
        Cor cor = new Cor();
        cor.setId(id);
        cor.setEquipeId("eq-1");
        cor.setHex(hex);
        cor.setCustoMl(custoMl);
        return cor;
    }

    private MisturaRequestDTO dtoMeioAMeio(String nome) {
        ItemMisturaRequestDTO item1 = new ItemMisturaRequestDTO();
        item1.setCorId("c-1");
        item1.setProporcao(50.0);

        ItemMisturaRequestDTO item2 = new ItemMisturaRequestDTO();
        item2.setCorId("c-2");
        item2.setProporcao(50.0);

        MisturaRequestDTO dto = new MisturaRequestDTO();
        dto.setNome(nome);
        dto.setVolumeMl(100);
        dto.setItens(List.of(item1, item2));
        return dto;
    }

    @Test
    void criarDeveCalcularHexEValorNaEquipe() {
        naEquipe("user-1", "eq-1");
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor("c-1", "#FF0000", 1.5)));
        when(corRepository.findByIdAndEquipeId("c-2", "eq-1")).thenReturn(Optional.of(cor("c-2", "#0000FF", 2.0)));
        when(repository.save(any(Mistura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MisturaResponseDTO result = service.criar(dtoMeioAMeio("Mistura Roxa"), "user-1");

        assertEquals("Mistura Roxa", result.getNome());
        assertEquals(100, result.getVolumeMl());
        assertNotNull(result.getHexResultado());
        assertNotNull(result.getCustoEstimado());
        verify(repository).save(argThat(m -> "eq-1".equals(m.getEquipeId()) && "user-1".equals(m.getUsuarioId())));
    }

    @Test
    void criarComCorDeOutraEquipeDeveFalhar() {
        naEquipe("user-1", "eq-1");
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor("c-1", "#FF0000", 1.5)));
        // c-2 é de outra equipe: na busca pela eq-1 ela não existe

        RuntimeException ex = assertThrows(RuntimeException.class,
                () -> service.criar(dtoMeioAMeio("Mistura"), "user-1"));

        assertEquals("Cor não encontrada na paleta", ex.getMessage());
        verify(repository, never()).save(any());
    }

    @Test
    void criarSemEquipeDeveSerRecusado() {
        semEquipe("user-1");

        assertThrows(SemEquipeException.class, () -> service.criar(dtoMeioAMeio("Mistura"), "user-1"));
        verifyNoInteractions(repository, corRepository);
    }

    @Test
    void listarDeveRetornarMisturasDaEquipe() {
        naEquipe("user-1", "eq-1");
        Cor cor = cor("c-1", "#FF0000", 1.0);
        cor.setNome("Vermelho");
        cor.setFornecedor("F");

        Mistura mistura = new Mistura();
        mistura.setId("m-1");
        mistura.setEquipeId("eq-1");
        mistura.setUsuarioId("user-2");
        mistura.setNome("Mistura");
        mistura.setVolumeMl(100);
        mistura.setHexResultado("#FF0000");
        mistura.setCustoEstimado(10.0);
        mistura.setItens(List.of(new synapseforge.crud.infrastructure.entity.ItemMistura("c-1", 100.0)));

        when(repository.findByEquipeId("eq-1")).thenReturn(List.of(mistura));
        when(corRepository.findByIdInAndEquipeId(List.of("c-1"), "eq-1")).thenReturn(List.of(cor));

        List<MisturaResponseDTO> result = service.listar("user-1");

        assertEquals(1, result.size());
        assertEquals("Mistura", result.get(0).getNome());
        assertEquals("Vermelho", result.get(0).getItens().get(0).getNome());
    }

    @Test
    void listarSemEquipeDeveRetornarVazio() {
        semEquipe("user-1");

        assertTrue(service.listar("user-1").isEmpty());
        verifyNoInteractions(repository);
    }

    @Test
    void atualizarDeveAlterarDados() {
        naEquipe("user-1", "eq-1");
        Mistura existente = new Mistura();
        existente.setId("m-1");
        existente.setEquipeId("eq-1");
        existente.setUsuarioId("user-1");
        existente.setNome("Antiga");
        existente.setVolumeMl(100);
        existente.setHexResultado("#000000");
        existente.setCustoEstimado(0.0);
        existente.setItens(List.of(new synapseforge.crud.infrastructure.entity.ItemMistura("c-1", 100.0)));

        when(repository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(existente));
        when(corRepository.findByIdAndEquipeId("c-1", "eq-1")).thenReturn(Optional.of(cor("c-1", "#FF0000", 1.5)));
        when(corRepository.findByIdAndEquipeId("c-2", "eq-1")).thenReturn(Optional.of(cor("c-2", "#0000FF", 2.0)));
        when(repository.save(existente)).thenReturn(existente);

        MisturaResponseDTO result = service.atualizar("m-1", "user-1", dtoMeioAMeio("Nova"));

        assertEquals("Nova", result.getNome());
        assertNotNull(result.getHexResultado());
    }

    @Test
    void misturaDeOutraEquipeNaoEEncontrada() {
        naEquipe("user-9", "eq-2");

        RuntimeException aoBuscar = assertThrows(RuntimeException.class,
                () -> service.buscarPorId("m-1", "user-9"));
        RuntimeException aoAtualizar = assertThrows(RuntimeException.class,
                () -> service.atualizar("m-1", "user-9", dtoMeioAMeio("X")));
        RuntimeException aoDeletar = assertThrows(RuntimeException.class,
                () -> service.deletar("m-1", "user-9"));

        assertEquals("Mistura não encontrada", aoBuscar.getMessage());
        assertEquals("Mistura não encontrada", aoAtualizar.getMessage());
        assertEquals("Mistura não encontrada", aoDeletar.getMessage());
        verify(repository, times(3)).findByIdAndEquipeId("m-1", "eq-2");
        verify(repository, never()).findById(any());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void deletarDeveExcluirMistura() {
        naEquipe("user-1", "eq-1");
        Mistura mistura = new Mistura();
        mistura.setId("m-1");
        mistura.setEquipeId("eq-1");
        when(repository.findByIdAndEquipeId("m-1", "eq-1")).thenReturn(Optional.of(mistura));

        service.deletar("m-1", "user-1");

        verify(repository).deleteById("m-1");
    }
}
