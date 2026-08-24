package synapseforge.crud.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.infrastructure.entity.Material;
import synapseforge.crud.infrastructure.entity.Orcamento;
import synapseforge.crud.infrastructure.repository.MaterialRepository;
import synapseforge.crud.infrastructure.repository.OrcamentoRepository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@ExtendWith(MockitoExtension.class)
class OrcamentoServiceTest {

    @Mock
    private OrcamentoRepository repository;

    @Mock
    private MaterialRepository materialRepository;

    @InjectMocks
    private OrcamentoService service;

    @Test
    void calcularDeveRetornarPreview() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setDensidadeGcm3(1.25);
        material.setPrecoPorGrama(new BigDecimal("0.10"));
        material.setAtivo(true);

        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        dto.setMaterialId("m-1");
        dto.setVolumeCm3(100.0);
        dto.setTempoImpressaoHoras(2.0);
        dto.setTempoMaoDeObraHoras(1.0);
        dto.setCustoMaquinaHora(BigDecimal.valueOf(30));
        dto.setCustoMaoDeObraHora(BigDecimal.valueOf(20));
        dto.setMargemLucro(BigDecimal.valueOf(30));

        when(materialRepository.findById("m-1")).thenReturn(Optional.of(material));

        OrcamentoResponseDTO result = service.calcular(dto);

        assertEquals("PLA", result.getNomeMaterial());
        assertEquals("m-1", result.getMaterialId());
        assertNotNull(result.getCustoTotal());
    }

    @Test
    void salvarDevePersistirOrcamento() {
        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");
        material.setDensidadeGcm3(1.25);
        material.setPrecoPorGrama(new BigDecimal("0.10"));
        material.setAtivo(true);

        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        dto.setMaterialId("m-1");
        dto.setVolumeCm3(100.0);
        dto.setTempoImpressaoHoras(2.0);
        dto.setTempoMaoDeObraHoras(1.0);
        dto.setCustoMaquinaHora(BigDecimal.valueOf(30));
        dto.setCustoMaoDeObraHora(BigDecimal.valueOf(20));
        dto.setMargemLucro(BigDecimal.valueOf(30));

        when(materialRepository.findById("m-1")).thenReturn(Optional.of(material));
        when(repository.save(any(Orcamento.class))).thenAnswer(invocation -> {
            Orcamento orcamento = invocation.getArgument(0);
            orcamento.setId("o-1");
            orcamento.setCriadoEm(LocalDateTime.now());
            return orcamento;
        });

        OrcamentoResponseDTO result = service.salvar(dto);

        assertEquals("o-1", result.getId());
        assertEquals("PLA", result.getNomeMaterial());
    }

    @Test
    void listarDeveRetornarTodos() {
        Orcamento orcamento = new Orcamento();
        orcamento.setId("o-1");
        orcamento.setMaterialId("m-1");
        orcamento.setVolumeCm3(100.0);
        orcamento.setTempoImpressaoHoras(2.0);
        orcamento.setTempoMaoDeObraHoras(1.0);
        orcamento.setCustoMaquinaHora(BigDecimal.valueOf(30));
        orcamento.setCustoMaoDeObraHora(BigDecimal.valueOf(20));
        orcamento.setMargemLucro(BigDecimal.valueOf(30));
        orcamento.setCustoMaterial(BigDecimal.valueOf(12.5));
        orcamento.setCustoMaquina(BigDecimal.valueOf(60));
        orcamento.setCustoMaoDeObra(BigDecimal.valueOf(20));
        orcamento.setCustoTotal(BigDecimal.valueOf(92.5));
        orcamento.setPrecoFinal(BigDecimal.valueOf(120.25));

        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");

        when(repository.findAllByOrderByCriadoEmDesc()).thenReturn(List.of(orcamento));
        when(materialRepository.findById("m-1")).thenReturn(Optional.of(material));

        List<OrcamentoResponseDTO> result = service.listar();

        assertEquals(1, result.size());
        assertEquals("PLA", result.get(0).getNomeMaterial());
    }

    @Test
    void buscarPorIdDeveRetornarDetalhes() {
        Orcamento orcamento = new Orcamento();
        orcamento.setId("o-1");
        orcamento.setMaterialId("m-1");
        orcamento.setVolumeCm3(100.0);
        orcamento.setTempoImpressaoHoras(2.0);
        orcamento.setTempoMaoDeObraHoras(1.0);
        orcamento.setCustoMaquinaHora(BigDecimal.valueOf(30));
        orcamento.setCustoMaoDeObraHora(BigDecimal.valueOf(20));
        orcamento.setMargemLucro(BigDecimal.valueOf(30));
        orcamento.setCustoMaterial(BigDecimal.valueOf(12.5));
        orcamento.setCustoMaquina(BigDecimal.valueOf(60));
        orcamento.setCustoMaoDeObra(BigDecimal.valueOf(20));
        orcamento.setCustoTotal(BigDecimal.valueOf(92.5));
        orcamento.setPrecoFinal(BigDecimal.valueOf(120.25));

        Material material = new Material();
        material.setId("m-1");
        material.setNome("PLA");

        when(repository.findById("o-1")).thenReturn(Optional.of(orcamento));
        when(materialRepository.findById("m-1")).thenReturn(Optional.of(material));

        OrcamentoResponseDTO result = service.buscarPorId("o-1");

        assertEquals("o-1", result.getId());
        assertEquals("PLA", result.getNomeMaterial());
    }
}
