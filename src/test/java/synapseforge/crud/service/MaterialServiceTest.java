package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.infrastructure.repository.MaterialRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class MaterialServiceTest {

    @Mock
    private MaterialRepository repository;

    @InjectMocks
    private MaterialService service;

    @Test
    void criarDevePersistirMaterial() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PLA");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.24);
        dto.setPrecoPorGrama(new BigDecimal("0.05"));
        dto.setAtivo(true);

        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setTipo("Filamento");
        material.setDensidadeGcm3(1.24);
        material.setPrecoPorGrama(new BigDecimal("0.05"));
        material.setAtivo(true);

        when(repository.save(any(Material.class))).thenReturn(material);

        MaterialResponseDTO result = service.criar(dto);

        assertEquals("PLA", result.getNome());
        assertTrue(result.getAtivo());
        verify(repository).save(any(Material.class));
    }

    @Test
    void criarComEstoqueMinimoInformadoPersisteOValorESaldoNasceZerado() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PLA");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.24);
        dto.setPrecoPorGrama(new BigDecimal("0.05"));
        dto.setEstoqueMinimo(new BigDecimal("200"));

        when(repository.save(any(Material.class))).thenAnswer(inv -> inv.getArgument(0));

        MaterialResponseDTO result = service.criar(dto);

        assertEquals(new BigDecimal("200"), result.getEstoqueMinimo());
        assertEquals(BigDecimal.ZERO, result.getSaldo());
        assertEquals(UnidadeMedida.G, result.getUnidade());
    }

    @Test
    void atualizarComEstoqueMinimoNuloPreservaOValorENuncaAlteraOSaldo() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setTipo("Filamento");
        material.setDensidadeGcm3(1.24);
        material.setPrecoPorGrama(new BigDecimal("0.05"));
        material.setAtivo(true);
        material.setUnidade(UnidadeMedida.G);
        material.setSaldo(new BigDecimal("350"));
        material.setEstoqueMinimo(new BigDecimal("200"));

        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PLA Premium");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.24);
        dto.setPrecoPorGrama(new BigDecimal("0.07"));

        when(repository.findById("m-1")).thenReturn(Optional.of(material));
        when(repository.save(material)).thenReturn(material);

        MaterialResponseDTO result = service.atualizar("m-1", dto);

        assertEquals("PLA Premium", result.getNome());
        assertEquals(new BigDecimal("200"), result.getEstoqueMinimo());
        assertEquals(new BigDecimal("350"), result.getSaldo());
    }

    @Test
    void listarAtivosDeveRetornarDtos() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setTipo("Filamento");
        material.setDensidadeGcm3(1.24);
        material.setPrecoPorGrama(new BigDecimal("0.05"));
        material.setAtivo(true);

        when(repository.findByAtivoTrue()).thenReturn(List.of(material));

        List<MaterialResponseDTO> result = service.listarAtivos();

        assertEquals(1, result.size());
        assertEquals("PLA", result.get(0).getNome());
    }

    @Test
    void buscarPorIdDeveRetornarMaterial() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setTipo("Filamento");
        material.setDensidadeGcm3(1.24);
        material.setPrecoPorGrama(new BigDecimal("0.05"));
        material.setAtivo(true);

        when(repository.findById("m-1")).thenReturn(Optional.of(material));

        MaterialResponseDTO result = service.buscarPorId("m-1");

        assertEquals("PLA", result.getNome());
    }

    @Test
    void atualizarDeveAplicarDados() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setTipo("Filamento");
        material.setDensidadeGcm3(1.24);
        material.setPrecoPorGrama(new BigDecimal("0.05"));
        material.setAtivo(true);

        MaterialRequestDTO dto = new MaterialRequestDTO();
        dto.setNome("PETG");
        dto.setTipo("Filamento");
        dto.setDensidadeGcm3(1.27);
        dto.setPrecoPorGrama(new BigDecimal("0.08"));
        dto.setAtivo(true);

        when(repository.findById("m-1")).thenReturn(Optional.of(material));
        when(repository.save(material)).thenReturn(material);

        MaterialResponseDTO result = service.atualizar("m-1", dto);

        assertEquals("PETG", result.getNome());
        assertEquals(new BigDecimal("0.08"), result.getPrecoPorGrama());
    }

    @Test
    void inativarDeveMarcarComoInativo() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setAtivo(true);

        when(repository.findById("m-1")).thenReturn(Optional.of(material));

        service.inativar("m-1");

        assertFalse(material.getAtivo());
        verify(repository).save(material);
    }
}
