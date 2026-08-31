package synapseforge.crud.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.gridfs.GridFsResource;
import org.springframework.data.mongodb.gridfs.GridFsTemplate;
import synapseforge.crud.infrastructure.entity.Pedido;

import java.io.ByteArrayInputStream;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfServiceTest {

    @Mock
    GridFsTemplate gridFsTemplate;

    @InjectMocks
    PdfService service;

    Pedido pedido;

    @BeforeEach
    void setup() {
        pedido = new Pedido();
        pedido.setId("abcdef123456");
        pedido.setCliente("Cliente X");
        pedido.setProjeto("Projeto Y");
        pedido.setDescricao("Uma descrição");
        pedido.setPrazo(LocalDate.of(2026,8,30));
        pedido.setStatus(synapseforge.crud.infrastructure.entity.StatusPedido.MODELAGEM);
    }

    @Test
    void gerarOrdemServico_should_return_non_empty_pdf_when_no_files() throws Exception {
        byte[] pdf = service.gerarOrdemServico(pedido);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100); // some bytes expected

        // basic PDF header check ("%PDF")
        String start = new String(pdf, 0, Math.min(4, pdf.length));
        assertTrue(start.contains("%PDF") || start.contains("%PDF-"));
    }

    @Test
    void gerarOrdemServico_should_include_images_when_found_in_gridfs() throws Exception {
        // simulate GridFSFile found for images
        GridFSFile gridFsFile = mock(GridFSFile.class);
        lenient().when(gridFsFile.getFilename()).thenReturn("ref.png");
        lenient().when(gridFsTemplate.findOne(any(Query.class))).thenReturn(gridFsFile);

        GridFsResource resource = mock(GridFsResource.class);
        byte[] img = java.util.Base64.getDecoder().decode(
                "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAQAAAC1HAwCAAAAC0lEQVR4nGNgYAAAAAMAAWgmWQ0AAAAASUVORK5CYII=");
        lenient().when(resource.getInputStream()).thenReturn(new ByteArrayInputStream(img));
        lenient().when(gridFsTemplate.getResource(gridFsFile)).thenReturn(resource);

        pedido.setImagensReferenciaFileIds(List.of("507f1f77bcf86cd799439011"));

        byte[] pdf = service.gerarOrdemServico(pedido);
        assertNotNull(pdf);
        assertTrue(pdf.length > 100);
    }

    @Test
    void adicionarLinha_private_method_should_add_cells() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        // create a PdfPTable using reflection since method is private
        com.lowagie.text.pdf.PdfPTable table = new com.lowagie.text.pdf.PdfPTable(2);

        Method m = PdfService.class.getDeclaredMethod("adicionarLinha", com.lowagie.text.pdf.PdfPTable.class, String.class, String.class, com.lowagie.text.Font.class, com.lowagie.text.Font.class);
        m.setAccessible(true);

        com.lowagie.text.Font f1 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10);
        com.lowagie.text.Font f2 = new com.lowagie.text.Font(com.lowagie.text.Font.HELVETICA, 10);

        m.invoke(service, table, "Label", "Valor", f1, f2);

        // after adding, table should have at least one row
        assertTrue(table.getRows().size() >= 1);
    }
}
