package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.service.OrcamentoService;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrcamentoControllerTest {

    @Mock
    private OrcamentoService service;

    @InjectMocks
    private OrcamentoController controller;

    @Test
    void calcularDeveRetornarPreview() {
        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        OrcamentoResponseDTO response = new OrcamentoResponseDTO("o-1", "m-1", "PLA", 50.0, 2.0, 1.5, BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(30), BigDecimal.valueOf(3.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(30.0), BigDecimal.valueOf(93.0), BigDecimal.valueOf(120.9), null);
        when(service.calcular(dto)).thenReturn(response);

        assertEquals("PLA", controller.calcular(dto).getNomeMaterial());
    }

    @Test
    void salvarDeveRetornarOrcamentoPersistido() {
        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        OrcamentoResponseDTO response = new OrcamentoResponseDTO("o-1", "m-1", "PLA", 50.0, 2.0, 1.5, BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(30), BigDecimal.valueOf(3.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(30.0), BigDecimal.valueOf(93.0), BigDecimal.valueOf(120.9), null);
        when(service.salvar(dto)).thenReturn(response);

        assertEquals("o-1", controller.salvar(dto).getId());
    }

    @Test
    void listarDeveRetornarLista() {
        OrcamentoResponseDTO response = new OrcamentoResponseDTO("o-1", "m-1", "PLA", 50.0, 2.0, 1.5, BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(30), BigDecimal.valueOf(3.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(30.0), BigDecimal.valueOf(93.0), BigDecimal.valueOf(120.9), null);
        when(service.listar()).thenReturn(List.of(response));

        assertEquals(1, controller.listar().size());
    }

    @Test
    void buscarPorIdDeveRetornarOrcamento() {
        OrcamentoResponseDTO response = new OrcamentoResponseDTO("o-1", "m-1", "PLA", 50.0, 2.0, 1.5, BigDecimal.valueOf(30), BigDecimal.valueOf(20), BigDecimal.valueOf(30), BigDecimal.valueOf(3.0), BigDecimal.valueOf(60.0), BigDecimal.valueOf(30.0), BigDecimal.valueOf(93.0), BigDecimal.valueOf(120.9), null);
        when(service.buscarPorId("o-1")).thenReturn(response);

        assertEquals("PLA", controller.buscarPorId("o-1").getNomeMaterial());
    }
}
