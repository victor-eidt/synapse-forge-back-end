package synapseforge.crud.service;

import com.mongodb.client.gridfs.model.GridFSFile;
import org.springframework.http.MediaType;
import org.springframework.http.MediaTypeFactory;

import java.util.Locale;
import java.util.Set;

public final class ArquivoUtils {

    private ArquivoUtils() {
    }

    public static String contentType(GridFSFile file) {
        String metadataContentType = file.getMetadata() == null
                ? null
                : firstNonBlank(
                        file.getMetadata().getString("contentType"),
                        file.getMetadata().getString("_contentType")
                );

        String filename = file.getFilename();
        String extension = extension(filename);
        String inferredContentType = switch (extension) {
            case "obj" -> "model/obj";
            case "stl" -> "model/stl";
            case "3mf" -> "model/3mf";
            case "jpg", "jpeg" -> MediaType.IMAGE_JPEG_VALUE;
            case "png" -> MediaType.IMAGE_PNG_VALUE;
            default -> filename == null
                    ? null
                    : MediaTypeFactory.getMediaType(filename)
                            .map(MediaType::toString)
                            .orElse(null);
        };

        // O metadata e gravado no upload; o nome do arquivo vem do cliente, entao so
        // serve de fallback. Os dois vem do cliente: fora da lista, o arquivo sai como
        // octet-stream (um text/html ou image/svg+xml servido pela API seria XSS).
        String permitido = permitido(metadataContentType);
        if (permitido == null) {
            permitido = permitido(inferredContentType);
        }

        return permitido == null
                ? MediaType.APPLICATION_OCTET_STREAM_VALUE
                : permitido;
    }

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            MediaType.IMAGE_JPEG_VALUE,
            MediaType.IMAGE_PNG_VALUE,
            MediaType.IMAGE_GIF_VALUE,
            "image/webp",
            "model/obj",
            "model/stl",
            "model/3mf",
            MediaType.APPLICATION_PDF_VALUE
    );

    private static String permitido(String contentType) {
        if (contentType == null) {
            return null;
        }
        // "image/png; charset=..." -> "image/png"
        String tipo = contentType.split(";", 2)[0].trim().toLowerCase(Locale.ROOT);
        return TIPOS_PERMITIDOS.contains(tipo) ? tipo : null;
    }

    private static String firstNonBlank(String first, String second) {
        return first != null && !first.isBlank() ? first
                : second != null && !second.isBlank() ? second
                : null;
    }

    private static String extension(String filename) {
        if (filename == null) {
            return "";
        }

        int dot = filename.lastIndexOf('.');
        return dot < 0 ? "" : filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
