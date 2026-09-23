package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Material.MaterialRequestDTO;
import synapseforge.crud.DTO.Material.MaterialResponseDTO;
import synapseforge.crud.infrastructure.entity.UnidadeMedida;
import synapseforge.crud.service.MaterialService;

import java.math.BigDecimal;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class MaterialControllerTest {

    @Mock
    private MaterialService service;

    @InjectMocks
    private MaterialController controller;

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");
        return auth;
    }

    @Test
    void criarDeveRetornarMaterialCriado() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PLA", "Filamento", 1.24, new BigDecimal("0.05"), true, UnidadeMedida.G, new BigDecimal("0"), new BigDecimal("0"));
        when(service.criar(dto, "user-1")).thenReturn(expected);

        assertEquals("PLA", controller.criar(dto, auth()).getNome());
        verify(service).criar(dto, "user-1");
    }

    @Test
    void listarAtivosDeveRetornarLista() {
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PLA", "Filamento", 1.24, new BigDecimal("0.05"), true, UnidadeMedida.G, new BigDecimal("0"), new BigDecimal("0"));
        when(service.listarAtivos("user-1")).thenReturn(List.of(expected));

        assertEquals(1, controller.listarAtivos(auth()).size());
    }

    @Test
    void buscarPorIdDeveRetornarMaterial() {
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PLA", "Filamento", 1.24, new BigDecimal("0.05"), true, UnidadeMedida.G, new BigDecimal("0"), new BigDecimal("0"));
        when(service.buscarPorId("m-1", "user-1")).thenReturn(expected);

        assertEquals("PLA", controller.buscarPorId("m-1", auth()).getNome());
    }

    @Test
    void atualizarDeveRetornarMaterialAtualizado() {
        MaterialRequestDTO dto = new MaterialRequestDTO();
        MaterialResponseDTO expected = new MaterialResponseDTO("m-1", "PETG", "Filamento", 1.27, new BigDecimal("0.08"), true, UnidadeMedida.G, new BigDecimal("0"), new BigDecimal("0"));
        when(service.atualizar("m-1", dto, "user-1")).thenReturn(expected);

        assertEquals("PETG", controller.atualizar("m-1", dto, auth()).getNome());
    }

    @Test
    void inativarDeveChamarService() {
        controller.inativar("m-1", auth());
        verify(service).inativar("m-1", "user-1");
    }
}
