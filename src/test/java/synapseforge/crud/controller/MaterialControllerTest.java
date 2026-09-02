package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.service.MaterialService;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock
    private MaterialService service;

    @InjectMocks
    private MaterialController controller;

    @Test
    void criarDeveRetornarMaterialCriado() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PLA", "Filamento", 1.24, new BigDecimal("0.05"), true);
        when(service.criar(dto)).thenReturn(expected);

        assertEquals("PLA", controller.criar(dto).getNome());
        verify(service).criar(dto);
    }

    @Test
    void listarAtivosDeveRetornarLista() {
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PLA", "Filamento", 1.24, new BigDecimal("0.05"), true);
        when(service.listarAtivos()).thenReturn(List.of(expected));

        assertEquals(1, controller.listarAtivos().size());
    }

    @Test
    void buscarPorIdDeveRetornarMaterial() {
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PLA", "Filamento", 1.24, new BigDecimal("0.05"), true);
        when(service.buscarPorId("m-1")).thenReturn(expected);

        assertEquals("PLA", controller.buscarPorId("m-1").getNome());
    }

    @Test
    void atualizarDeveRetornarMaterialAtualizado() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PETG", "Filamento", 1.27, new BigDecimal("0.08"), true);
        when(service.atualizar("m-1", dto)).thenReturn(expected);

        assertEquals("PETG", controller.atualizar("m-1", dto).getNome());
    }

    @Test
    void inativarDeveChamarService() {
        controller.inativar("m-1");
        verify(service).inativar("m-1");
    }
}
