package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Cor.CorRequestDTO;
import synapseforge.crud.DTO.Cor.CorResponseDTO;
import synapseforge.crud.infrastructure.entity.Acabamento;
import synapseforge.crud.infrastructure.entity.Cor;
import synapseforge.crud.infrastructure.repository.CorRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class CorServiceTest {

    @Mock
    private CorRepository repository;

    @InjectMocks
    private CorService service;

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
    void criarDeveSalvarComTimestamps() {
        Cor cor = new Cor();
        cor.setNome("Azul");
        when(repository.save(any(Cor.class))).thenAnswer(invocation -> invocation.getArgument(0));

        Cor result = service.criar(cor);

        assertNotNull(result.getCriadoEm());
        assertNotNull(result.getAtualizadoEm());
        verify(repository).save(cor);
    }

    @Test
    void listarDeveRetornarCoresDoUsuario() {
        Cor cor = new Cor();
        cor.setNome("Azul");
        when(repository.findByUsuarioId("user-1")).thenReturn(List.of(cor));

        List<Cor> result = service.listar("user-1");

        assertEquals(1, result.size());
    }

    @Test
    void buscarPorIdDeveFiltrarUsuario() {
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");
        when(repository.findById("c-1")).thenReturn(Optional.of(cor));

        Optional<Cor> result = service.buscarPorId("c-1", "user-1");

        assertTrue(result.isPresent());
        assertEquals("c-1", result.get().getId());
    }

    @Test
    void atualizarDeveSalvarMudancas() {
        Cor existente = new Cor();
        existente.setId("c-1");
        existente.setUsuarioId("user-1");
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

        when(repository.findById("c-1")).thenReturn(Optional.of(existente));
        when(repository.save(existente)).thenReturn(existente);

        Cor result = service.atualizar("c-1", "user-1", dados);

        assertEquals("Verde", result.getNome());
        assertEquals("#00FF00", result.getHex());
        assertNotNull(result.getAtualizadoEm());
    }

    @Test
    void deletarDeveValidarUsuarioAntesDeExcluir() {
        Cor cor = new Cor();
        cor.setId("c-1");
        cor.setUsuarioId("user-1");
        when(repository.findById("c-1")).thenReturn(Optional.of(cor));

        service.deletar("c-1", "user-1");

        verify(repository).deleteById("c-1");
    }
}
