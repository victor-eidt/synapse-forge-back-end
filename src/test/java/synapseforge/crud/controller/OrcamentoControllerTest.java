package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.service.OrcamentoService;

import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrcamentoControllerTest {

    @Mock
    private OrcamentoService service;

    @InjectMocks
    private OrcamentoController controller;

    private Authentication auth() {
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("user-1");
        return auth;
    }

    @Test
    void calcularDeveRetornarPreview() {
        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(response.getNomeMaterial()).thenReturn("PLA");
        when(service.calcular(dto)).thenReturn(response);

        assertEquals("PLA", controller.calcular(dto).getNomeMaterial());
    }

    @Test
    void salvarDeveRetornarOrcamentoPersistido() {
        Authentication auth = auth();
        CalcularOrcamentoRequestDTO dto = new CalcularOrcamentoRequestDTO();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(response.getId()).thenReturn("o-1");
        when(service.salvar(dto, "user-1")).thenReturn(response);

        assertEquals("o-1", controller.salvar(dto, auth).getId());
    }

    @Test
    void listarDeveRetornarLista() {
        Authentication auth = auth();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(service.listar("user-1")).thenReturn(List.of(response));

        assertEquals(1, controller.listar(auth).size());
    }

    @Test
    void buscarPorIdDeveRetornarOrcamento() {
        Authentication auth = auth();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(response.getNomeMaterial()).thenReturn("PLA");
        when(service.buscarPorId("o-1", "user-1")).thenReturn(response);

        assertEquals("PLA", controller.buscarPorId("o-1", auth).getNomeMaterial());
    }
}
