package synapseforge.crud.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.bson.Document;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ArquivoUtilsTest {

    @Test
    void deveInferirTiposConhecidosPeloNome() {
        assertEquals("model/obj", tipo("modelo.OBJ", null));
        assertEquals("model/stl", tipo("modelo.stl", null));
        assertEquals("model/3mf", tipo("modelo.3mf", null));
        assertEquals("image/jpeg", tipo("foto.jpeg", null));
        assertEquals("image/png", tipo("foto.png", null));
        assertEquals("application/pdf", tipo("arquivo.pdf", null));
    }

    @Test
    void deveUsarMetadadoQuandoFormatoNaoForInferido() {
        Document metadata = new Document("contentType", "application/x-custom");
        assertEquals("application/x-custom", tipo("arquivo.semextensao", metadata));

        Document fallback = new Document("_contentType", "text/plain");
        assertEquals("text/plain", tipo(null, fallback));
    }

    @Test
    void deveUsarOctetStreamSemNomeNemMetadado() {
        assertEquals("application/octet-stream", tipo(null, null));
        assertEquals("application/octet-stream", tipo("arquivo.desconhecido", new Document()));
    }

    private String tipo(String nome, Document metadata) {
        GridFSFile file = mock(GridFSFile.class);
        when(file.getFilename()).thenReturn(nome);
        when(file.getMetadata()).thenReturn(metadata);
        return ArquivoUtils.contentType(file);
    }
}
