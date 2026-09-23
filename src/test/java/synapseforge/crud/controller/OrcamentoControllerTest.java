package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.service.OrcamentoService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ExtendWith(MockitoExtension.class)
class OrcamentoControllerTest {

    @Mock
    private OrcamentoService service;

    @Mock
    private GridFsTemplate gridFsTemplate;

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
        when(service.calcular(dto, "user-1")).thenReturn(response);

        assertEquals("PLA", controller.calcular(dto, auth()).getNomeMaterial());
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

    @Test
    void salvarMultipartRecusadoNaoGravaArquivos() {
        Authentication auth = auth();
        doThrow(new SemEquipeException()).when(service).calcular(any(CalcularOrcamentoRequestDTO.class), eq("user-1"));

        assertThrows(SemEquipeException.class, () -> controller.salvarComArquivos("Cliente", "Projeto", null,
                LocalDate.now(), "m-1", 10.0, 1.0, 1.0, BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN,
                new MockMultipartFile("objeto3D", "obj.stl", "model/stl", new byte[]{1}),
                new MultipartFile[]{new MockMultipartFile("img", "img.png", "image/png", new byte[]{1})},
                auth));

        verifyNoInteractions(gridFsTemplate);
        verify(service, never()).salvar(any(), any(), any(), any());
    }
}
