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

    @InjectMocks
    private MisturaService service;

    @Test
    void criarDeveCalcularHexEValor() {
        Cor cor1 = new Cor();
        cor1.setId("c-1");
        cor1.setUsuarioId("user-1");
        cor1.setHex("#FF0000");
        cor1.setCustoMl(1.5);

        Cor cor2 = new Cor();
        cor2.setId("c-2");
        cor2.setUsuarioId("user-1");
        cor2.setHex("#0000FF");
        cor2.setCustoMl(2.0);

        ItemMisturaRequestDTO item1 = new ItemMisturaRequestDTO();
        item1.setCorId("c-1");
        item1.setProporcao(50.0);

        ItemMisturaRequestDTO item2 = new ItemMisturaRequestDTO();
        item2.setCorId("c-2");
        item2.setProporcao(50.0);

        MisturaRequestDTO dto = new MisturaRequestDTO();
        dto.setNome("Mistura Roxa");
        dto.setVolumeMl(100);
        dto.setItens(List.of(item1, item2));

        when(corRepository.findById("c-1")).thenReturn(Optional.of(cor1));
        when(corRepository.findById("c-2")).thenReturn(Optional.of(cor2));
        when(repository.save(any(Mistura.class))).thenAnswer(invocation -> invocation.getArgument(0));

        MisturaResponseDTO result = service.criar(dto, "user-1");

        assertEquals("Mistura Roxa", result.getNome());
        assertEquals(100, result.getVolumeMl());
        assertNotNull(result.getHexResultado());
        assertNotNull(result.getCustoEstimado());
    }

    @Test
    void listarDeveRetornarMisturasDoUsuario() {
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");
        cor.setHex("#FF0000");
        cor.setNome("Vermelho");
        cor.setFornecedor("F");
        cor.setCustoMl(1.0);

        Mistura mistura = new Mistura();
        mistura.setId("m-1");
        mistura.setUsuarioId("user-1");
        mistura.setNome("Mistura");
        mistura.setVolumeMl(100);
        mistura.setHexResultado("#FF0000");
        mistura.setCustoEstimado(10.0);
        mistura.setItens(List.of(new synapseforge.crud.infrastructure.entity.ItemMistura("c-1", 100.0)));

        when(repository.findByUsuarioId("user-1")).thenReturn(List.of(mistura));
        when(corRepository.findAllById(List.of("c-1"))).thenReturn(List.of(cor));

        List<MisturaResponseDTO> result = service.listar("user-1");

        assertEquals(1, result.size());
        assertEquals("Mistura", result.get(0).getNome());
    }

    @Test
    void atualizarDeveAlterarDados() {
        Cor cor1 = new Cor();
        cor1.setId("c-1");
        cor1.setUsuarioId("user-1");
        cor1.setHex("#FF0000");
        cor1.setCustoMl(1.5);

        Cor cor2 = new Cor();
        cor2.setId("c-2");
        cor2.setUsuarioId("user-1");
        cor2.setHex("#0000FF");
        cor2.setCustoMl(2.0);

        Mistura existente = new Mistura();
        existente.setId("m-1");
        existente.setUsuarioId("user-1");
        existente.setNome("Antiga");
        existente.setVolumeMl(100);
        existente.setHexResultado("#000000");
        existente.setCustoEstimado(0.0);
        existente.setItens(List.of(new synapseforge.crud.infrastructure.entity.ItemMistura("c-1", 100.0)));

        ItemMisturaRequestDTO item1 = new ItemMisturaRequestDTO();
        item1.setCorId("c-1");
        item1.setProporcao(50.0);

        ItemMisturaRequestDTO item2 = new ItemMisturaRequestDTO();
        item2.setCorId("c-2");
        item2.setProporcao(50.0);

        MisturaRequestDTO dto = new MisturaRequestDTO();
        dto.setNome("Nova");
        dto.setVolumeMl(100);
        dto.setItens(List.of(item1, item2));

        when(repository.findById("m-1")).thenReturn(Optional.of(existente));
        when(corRepository.findById("c-1")).thenReturn(Optional.of(cor1));
        when(corRepository.findById("c-2")).thenReturn(Optional.of(cor2));
        when(repository.save(existente)).thenReturn(existente);

        MisturaResponseDTO result = service.atualizar("m-1", "user-1", dto);

        assertEquals("Nova", result.getNome());
        assertNotNull(result.getHexResultado());
    }

    @Test
    void deletarDeveExcluirMistura() {
        Mistura mistura = new Mistura();
        mistura.setId("m-1");
        mistura.setUsuarioId("user-1");
        when(repository.findById("m-1")).thenReturn(Optional.of(mistura));

        service.deletar("m-1", "user-1");

        verify(repository).deleteById("m-1");
    }
}
