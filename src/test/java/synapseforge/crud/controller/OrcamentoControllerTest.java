package synapseforge.crud.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.io.InputStreamResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;
import synapseforge.crud.DTO.Orcamento.CalcularOrcamentoRequestDTO;
import synapseforge.crud.DTO.Orcamento.OrcamentoResponseDTO;
import synapseforge.crud.exception.SemEquipeException;
import synapseforge.crud.service.OrcamentoService;

import java.io.ByteArrayInputStream;
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

    @Test
    void salvarComArquivos_deveGravarArquivosEPersistir() throws Exception {
        Authentication auth = auth();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(response.getId()).thenReturn("o-2");

        when(gridFsTemplate.store(any(), anyString(), anyString())).thenReturn(new org.bson.types.ObjectId());
        when(service.calcular(any(CalcularOrcamentoRequestDTO.class), eq("user-1"))).thenReturn(response);
        when(service.salvar(any(CalcularOrcamentoRequestDTO.class), eq("user-1"), anyString(), anyList())).thenReturn(response);

        OrcamentoResponseDTO result = controller.salvarComArquivos(
                "Cliente", "Projeto", null, LocalDate.now(), "m-1", 10.0, 1.0, 1.0,
                BigDecimal.ONE, BigDecimal.ONE, BigDecimal.TEN,
                new MockMultipartFile("objeto3D", "obj.stl", "model/stl", new byte[]{1}),
                new MultipartFile[]{new MockMultipartFile("img", "img.png", "image/png", new byte[]{1})},
                auth
        );

        assertEquals("o-2", result.getId());
        verify(gridFsTemplate, times(2)).store(any(), anyString(), anyString());
    }

    @Test
    void baixarObjeto3D_quandoExisteDeveRetornarArquivo() throws Exception {
        Authentication auth = auth();
        String fileId = new org.bson.types.ObjectId().toHexString();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(response.getObjeto3DFileId()).thenReturn(fileId);

        com.mongodb.client.gridfs.model.GridFSFile gridFsFile = mock(com.mongodb.client.gridfs.model.GridFSFile.class);
        when(gridFsFile.getFilename()).thenReturn("modelo.stl");
        when(gridFsFile.getMetadata()).thenReturn(new org.bson.Document("contentType", "model/stl"));

        org.springframework.data.mongodb.gridfs.GridFsResource resource = mock(org.springframework.data.mongodb.gridfs.GridFsResource.class);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        when(service.buscarPorId("o-1", "user-1")).thenReturn(response);
        when(gridFsTemplate.findOne(any())).thenReturn(gridFsFile);
        when(gridFsTemplate.getResource(gridFsFile)).thenReturn(resource);

        var result = controller.baixarObjeto3D("o-1", auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals("model/stl", result.getHeaders().getContentType().toString());
    }

    @Test
    void visualizarImagem_quandoValidaDeveRetornarArquivo() throws Exception {
        Authentication auth = auth();
        String imagemId = new org.bson.types.ObjectId().toHexString();
        OrcamentoResponseDTO response = mock(OrcamentoResponseDTO.class);
        when(response.getImagensReferenciaIds()).thenReturn(List.of(imagemId));

        com.mongodb.client.gridfs.model.GridFSFile gridFsFile = mock(com.mongodb.client.gridfs.model.GridFSFile.class);
        when(gridFsFile.getFilename()).thenReturn("img.png");
        when(gridFsFile.getMetadata()).thenReturn(new org.bson.Document("contentType", "image/png"));

        org.springframework.data.mongodb.gridfs.GridFsResource resource = mock(org.springframework.data.mongodb.gridfs.GridFsResource.class);
        when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(new byte[]{1, 2, 3}));

        when(service.buscarPorId("o-1", "user-1")).thenReturn(response);
        when(gridFsTemplate.findOne(any())).thenReturn(gridFsFile);
        when(gridFsTemplate.getResource(gridFsFile)).thenReturn(resource);

        var result = controller.visualizarImagem("o-1", imagemId, auth);

        assertEquals(HttpStatus.OK, result.getStatusCode());
    }

    @Test
    void aprovarERejeitarDeveRetornarOrcamentoAtualizado() {
        Authentication auth = auth();
        OrcamentoResponseDTO aprovada = mock(OrcamentoResponseDTO.class);
        OrcamentoResponseDTO rejeitada = mock(OrcamentoResponseDTO.class);

        when(service.aprovar("o-1", "user-1")).thenReturn(aprovada);
        when(service.rejeitar("o-1", "user-1")).thenReturn(rejeitada);

        assertEquals(aprovada, controller.aprovar("o-1", auth));
        assertEquals(rejeitada, controller.rejeitar("o-1", auth));
    }
}
